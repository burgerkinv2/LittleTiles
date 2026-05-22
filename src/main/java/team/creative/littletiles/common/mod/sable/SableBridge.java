package team.creative.littletiles.common.mod.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import team.creative.creativecore.common.util.math.box.ABB;
import team.creative.creativecore.common.util.math.matrix.IVecOrigin;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.level.little.LittleLevel;

/**
 * Optional Sable integration boundary.
 *
 * Keep Sable classes out of public LT call sites. Callers receive an opaque
 * {@link Context} and pass it back into this bridge, so LT animation/sub-level
 * code never depends on Sable types or class-loading semantics.
 */
public final class SableBridge {

    private static final String MODID = "sable";
    private static final boolean INSTALLED = ModList.get().isLoaded(MODID);

    private SableBridge() {}

    public static boolean isAvailable() {
        return INSTALLED;
    }

    public static boolean isInSableSubLevel(Level level, BlockPos pos) {
        return findContext(level, pos) != null;
    }

    public static Context findContext(Level level, BlockPos pos) {
        if (!INSTALLED || level == null || pos == null || level instanceof LittleLevel)
            return null;
        try {
            return SableBridgeImpl.findContext(level, pos);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to query sub-level at {}: {}", pos, t.toString());
            return null;
        }
    }

    public static Context findContext(Level level, Vec3 pos) {
        if (!INSTALLED || level == null || pos == null || level instanceof LittleLevel)
            return null;
        try {
            return SableBridgeImpl.findContext(level, pos);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to query sub-level at {}: {}", pos, t.toString());
            return null;
        }
    }

    public static Vec3 transformPointToSubLevelLocal(Context context, Vec3 point) {
        if (!INSTALLED || context == null || point == null)
            return null;
        try {
            return SableBridgeImpl.transformPointToSubLevelLocal(context, point);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform point: {}", t.toString());
            return null;
        }
    }

    public static Vec3 transformPointToSubLevelWorld(Context context, Vec3 point) {
        if (!INSTALLED || context == null || point == null)
            return null;
        try {
            return SableBridgeImpl.transformPointToSubLevelWorld(context, point);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform point to world: {}", t.toString());
            return null;
        }
    }

    public static Vec3 transformVectorToSubLevelWorld(Context context, Vec3 vector) {
        if (!INSTALLED || context == null || vector == null)
            return null;
        try {
            return SableBridgeImpl.transformVectorToSubLevelWorld(context, vector);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform vector to world: {}", t.toString());
            return null;
        }
    }

    public static Vec3 transformVectorToSubLevelLocal(Context context, Vec3 vector) {
        if (!INSTALLED || context == null || vector == null)
            return null;
        try {
            return SableBridgeImpl.transformVectorToSubLevelLocal(context, vector);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform vector to local: {}", t.toString());
            return null;
        }
    }

    public static AABB transformAABBToSubLevelLocal(Context context, AABB box) {
        if (!INSTALLED || context == null || box == null)
            return null;
        try {
            return SableBridgeImpl.transformAABBToSubLevelLocal(context, box);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform box to local: {}", t.toString());
            return null;
        }
    }

    public static AABB transformAABBToSubLevelWorld(Context context, AABB box) {
        if (!INSTALLED || context == null || box == null)
            return null;
        try {
            return SableBridgeImpl.transformAABBToSubLevelWorld(context, box);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform box to world: {}", t.toString());
            return null;
        }
    }

    public static ABB transformOBBToSubLevelWorld(Context context, ABB box, IVecOrigin origin) {
        if (!INSTALLED || context == null || box == null || origin == null)
            return null;
        try {
            return SableBridgeImpl.transformOBBToSubLevelWorld(context, box, origin);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform oriented box to world: {}", t.toString());
            return null;
        }
    }

    public static BlockHitResult raytraceInContext(Level level, Player player, Context context, float partialTick) {
        if (!INSTALLED || level == null || player == null)
            return null;
        try {
            return SableBridgeImpl.raytraceInContext(level, player, context, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to raytrace in context: {}", t.toString());
            return null;
        }
    }

    public static final class Context {

        private final Object subLevel;

        Context(Object subLevel) {
            this.subLevel = subLevel;
        }

        public Object unwrap() {
            return subLevel;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Context context && subLevel == context.subLevel;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(subLevel);
        }
    }
}
