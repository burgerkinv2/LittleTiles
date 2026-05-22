# LittleTiles Sable compatibility notes

This note documents the Sable compatibility work carried by this clean
LittleTiles 1.21.1 branch. The latest visible issue was LT animations inside
Sable sublevels, but the branch also covers LT block entity rendering, cache
refresh, lighting, selection outlines, tools, previews, interaction packets,
assembly movement, neighbor updates, collision, and Sable entity lifetime rules.

## Dependency model

The build uses local Sable jars as compile-only dependencies. By default Gradle
looks in `libs`:

```gradle
def sableLibDir = file(findProperty("sableLibDir") ?: "libs")
compileOnly files(
    "${sableLibDir}/sable-neoforge-1.21.1-1.1.3.jar",
    "${sableLibDir}/sable-companion-common-1.21.1-1.6.0.jar"
)
```

These jars are not committed to the repository and are not bundled by
LittleTiles. A developer building this branch should place the two jars in
`libs`, or pass a different local directory with
`-PsableLibDir=F:/path/to/sable-jars`. This keeps the build offline without
redistributing Sable's full jar from the LittleTiles repository.

Most runtime integration is isolated behind `SableBridge` and
`SableClientBridge`. Normal LT call sites use those wrappers, while direct
`dev.ryanhcode.sable.*` imports stay in bridge implementation classes. The
bridge checks whether the `sable` mod is loaded before calling Sable code.

The exception is `SubLevelAssemblyHelperLTRotateMixin`, which intentionally
targets a Sable class for assembly rotation compatibility:

```java
@Pseudo
@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class SubLevelAssemblyHelperLTRotateMixin
```

It is marked `@Pseudo` and its injection uses `require = 0`, but it still has
compile-time Sable API references. If LittleTiles is expected to run without
Sable installed, a no-Sable runtime test should remain part of release checks.

## Clean branch scope

This branch should be reviewed as one Sable compatibility pass. In clean-branch
order, the work is:

- Sable compatibility groundwork: bridge classes, local compile-only
  dependencies, render/cache hooks, tool and preview projection, interaction
  projection, assembly rotation support, neighbor update handling, and Sable
  selection helper paths.
- Sable selection overlay fixes: selection and preview rendering are tied to
  the active Sable context instead of drifting back to global-level space.
- Sable-hosted LT animation rendering: opened animations compose the Sable
  render pose with LT's own animation origin and use the vanilla-compatible LT
  animation render path in Sable sublevels.
- Sable-hosted LT animation interaction: hit results, right-click actions, and
  server handling preserve the Sable sublevel context.
- Sable-hosted LT animation collision and removal: collision keeps the composed
  Sable/LT origin, and Sable entity tags define how LT animation entities behave
  during normal sublevel movement and sublevel removal.
- Sable shader-state cleanup: Sable render state is reset after Sable-hosted LT
  animation buffers are drawn.

## Code structure

### Bridge layer

Common Sable access lives in:

- `common/mod/sable/SableBridge`
- `common/mod/sable/SableBridgeImpl`

Client-only Sable access lives in:

- `client/mod/sable/SableClientBridge`
- `client/mod/sable/SableClientBridgeImpl`
- `client/mod/sable/ShadelessBlockAndTintGetter`
- `client/mod/sable/SubLevelNormalConsumer`

`SableBridge.Context` is an opaque LT wrapper around the active Sable sublevel.
Most LT code passes that context around instead of importing Sable classes
directly.

### Render cache and lighting

Sable-hosted LT block entities cannot use every LT render path unchanged,
because the Sable sublevel supplies another moving render pose and shader
context. The relevant cache and render files are:

- `RenderingBlockQueue`
- `RenderingThread`
- `RenderingBlockContext`
- `LittleRenderPipelineForge`
- `BufferHolder`

The current behavior is:

- Sable-hosted LT block entities use the vanilla-compatible render path.
- Sable sublevel blocks are marked dirty again after async rebuilds so the
  moving sublevel receives the refreshed render data.
- When Sable dynamic directional shading is active, LT avoids baking the same
  directional shade into its CPU-side colors.
- With Iris shader packs, the code keeps the path that avoids corrupting Iris'
  extended vertex tracking.

### Selection outlines, tools, and previews

LT selection overlays and tools need Sable-local context when the player is
looking at a Sable sublevel. The affected paths include:

- `WorldBorderSableSubLevelMixin`
- `PreviewManager`
- `PreviewRenderer`
- `LittleToolPlacer`
- `LittleToolSelection`
- `LittleToolTransformer`
- `LittleToolShaper`
- `ShapePosition`
- `GuiScrewdriver`
- `ItemLittleScrewdriver`

The important rule is that multi-step tools keep the Sable context from their
starting point. This prevents a shape, transform, or screwdriver action from
starting in a Sable sublevel and finishing in the global level's coordinate
space.

### Block hits and interaction

LT block selection and interaction need the player ray to pass through both
coordinate systems:

1. real-world player ray
2. Sable-local ray, if the target is inside a Sable sublevel
3. LT tile or LT animation-local ray
4. real-world hit location for distance comparison and packet handling

The main common/server paths are:

- `BETiles`
- `LittleHitResult`
- `LittleActionInteract`
- `BlockPacket`
- `ChangedPosPacket`
- `LittleTilesServer`

This is the path that makes ordinary right-click interaction work for LT
content inside Sable sublevels. `BlockPacket` must preserve the projected
position/look data instead of reconstructing the action as if it happened in the
real level directly.

### Animation rendering and collision

Opened LT animations inside Sable sublevels combine two moving transforms:

1. the Sable sublevel render pose
2. the LT animation `IVecOrigin`

The relevant render entry points are:

- `LittleAnimationHandlerClient`
- `LittleEntityRenderManager`
- `LittleAnimationRenderManager`
- `LittleEntityRenderer`
- `RenderingLevelHandler`
- `SodiumWorldRendererMixin`

Sable-hosted LT animations use the vanilla-compatible LT animation render path.
Sable itself supports vanilla rendering in this area, so the code avoids relying
on LT's Sodium animation renderer while the animation is inside a moving Sable
sublevel. Shader state is switched while Sable-hosted animation buffers render
and reset immediately after.

Collision stays in LT's oriented-box path. Sable adds another moving origin, so
the collision code composes the LT animation origin with the Sable sublevel
origin instead of reducing everything to a transformed AABB too early. The main
files are:

- `LittleAnimationHandler`
- `LittleEntityPhysic`
- `SableBridgeImpl.SableComposedOrigin`

### Sable assembly, updates, and lifecycle

Sable assembly rotation compatibility is handled by:

- `mixin/common/sable/SubLevelAssemblyHelperLTRotateMixin`

Neighbor and chunk update visibility for LT changes inside Sable is handled by:

- `server/level/util/NeighborUpdateOrganizer`

Sable entity lifetime behavior is declared through data tags:

- `data/sable/tags/entity_type/retain_in_sub_level.json`
- `data/sable/tags/entity_type/destroy_with_sub_level.json`

`retain_in_sub_level` keeps LT animation entities with the Sable sublevel during
normal movement. `destroy_with_sub_level` makes `/sable remove` destroy those
entities instead of kicking them back into the global world.

## Problems fixed

### 1. LT block entities in Sable sublevels used the wrong render assumptions

Early Sable support forced Sable-hosted LT block entities through the
vanilla-compatible render path, refreshed dirty state after async rebuilds, and
adjusted CPU-side shading so Sable and Iris shader paths do not fight over the
same vertex data.

### 2. Selection outlines, previews, and tools lost Sable-local context

Selection overlays now have a Sable-aware world-border path, and LT tools ask
the bridge for the active Sable sublevel before producing positions, previews,
or multi-click selections. The same context is reused across multi-step tool
actions.

### 3. LT block hits and right-click interaction lost the Sable context

The player ray is transformed into Sable-local space before LT tile or animation
ray tracing. Hit results are transformed back to real space before distance
checks and packet dispatch. This preserves Sable context through
`LittleHitResult`, `LittleActionInteract`, `BlockPacket`, and server handling.

### 4. Sable assembly movement rotated LT tile data incorrectly

`SubLevelAssemblyHelperLTRotateMixin` keeps LT tile orientation consistent when
Sable disassembles or rotates sublevel content.

### 5. Opened LT animations disappeared or rendered with the wrong transform

Opened LT animations inside Sable sublevels now compose the Sable render pose
with the LT animation origin. The render path also resets Sable shader state
after drawing LT animation buffers, which avoids leaking Sable uniforms into
later vanilla rendering.

### 6. Opened and transitioning animation collision was inaccurate

Collision keeps LT oriented boxes and composes their origin with the Sable
sublevel origin. Transition pushing converts search boxes and movement vectors
between Sable world space and Sable local space before applying the final
movement.

### 7. Removed Sable sublevels could leave old LT animations behind

Sable entity tags tell Sable to retain LT animation entities during normal
sublevel movement but destroy them when the sublevel itself is removed. This
prevents old animations from reappearing after `/sable remove @e` followed by
`sable spawn block`.

## Known boundary

Opened LT animations inside Sable sublevels can still be expensive to render.
This compatibility pass focuses on correctness: coordinate conversion,
selection, interaction, render transforms, shader state, lighting behavior,
collision, and removal lifecycle. It does not include a broad LT animation
performance rewrite.

## Validation done locally

The branch has been built with:

```powershell
.\gradlew.bat :LittleTiles:build
```

Manual testing covered:

- LT block selection outlines and previews on Sable sublevels
- LT tool placement, selection, transform, and screwdriver behavior on Sable
  sublevels
- empty-hand right-click toggling of LT animation structures in Sable sublevels
- selecting and closing opened LT animations in Sable sublevels
- opened animation rendering without the old black or sky-colored planes
- collision on opened and transitioning animation structures
- `/sable remove @e` followed by `sable spawn block` without resurrecting the
  previously removed LT animation entity
