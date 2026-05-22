> [!IMPORTANT]
> VIBECODING PROJECT

# How to build this fork

This branch is meant to be built inside the shared CreativeMD Forge workspace.

1. Clone the Forge workspace:

```powershell
git clone https://github.com/CreativeMD/ForgeMods.git ForgeMods-1.21
cd ForgeMods-1.21
git checkout 1.21
git submodule update --init
```

2. Replace the workspace `CreativeCore` and `LittleTiles` folders with the
   matching forks for this branch. ForgeMods may already contain upstream
   versions of those folders; use the forked checkouts instead.

3. Keep `settings.gradle` including at least:

```gradle
include ':CreativeCore'
include ':LittleTiles'
```

4. Add the local Sable compile-only jars. They are not downloaded by Gradle and
   are not committed to this repository.

Put these two files in `LittleTiles/libs`:

```text
sable-neoforge-1.21.1-1.1.3.jar
sable-companion-common-1.21.1-1.6.0.jar
```

Put this file in `CreativeCore/libs`:

```text
sable-companion-common-1.21.1-1.6.0.jar
```

If all required Sable jars are stored in one external directory, pass that
directory once:

```powershell
.\gradlew.bat :LittleTiles:build -PsableLibDir=F:/path/to/sable-jars
```

5. Build from the ForgeMods root:

```powershell
.\gradlew.bat :LittleTiles:build
```

The Sable jars are compile-only. They are used to compile optional Sable
compatibility code and are not bundled into the LittleTiles jar.

Thanks to CreativeMd make this great mod

Special thanks to:  
   glm5.1 for very first try.  
   opus 4.6/7 for all basic work.  
   deepseek v4 flash for find and resolve critical issue.  
   codex5.5 for animation fix and whole refactor.  
## Original README

# LittleTiles

***This mod allows you to build anything you want. It adds a way to add more detail to everything. You can hammer blocks into tiles which can be 4096 (or even more) times smaller than an ordinary minecraft block. You can combine your tiles together to create doors, chairs, ladders, no-clip, storage and any kind of furniture structure. There are endless possibilities as shown in the [trailer]. So [download this mod] and start to go crazy (and maybe post some pics of your work).***

*Development for this mod started 2015, since then it has grown to a massive collection of features. Many things needed a lot of tweaking and time to fix all the bugs.*

## Addons

- [Kiros Basic Blocks] (by [Kiro])
- [ALET] (by [_Doc])
- [LittleTiles 3d Importer] (by [Timardo])
- [LittleOpener] (by [Alleluid])
- [LittleFrames]

[trailer]: https://youtu.be/2Hrn4pdV_ac
[download this mod]: https://www.curseforge.com/minecraft/mc-mods/littletiles
[Kiros Basic Blocks]: https://www.curseforge.com/minecraft/mc-mods/kiros-basic-blocks
[Kiro]: https://www.curseforge.com/members/kirokadura/projects
[ALET]: https://www.curseforge.com/minecraft/mc-mods/alet
[_Doc]: https://www.curseforge.com/members/ll_doc/projects
[LittleTiles 3d Importer]: https://www.curseforge.com/minecraft/mc-mods/littletiles-3d-importer
[Timardo]: https://www.curseforge.com/members/timardo/projects
[LittleOpener]: https://www.curseforge.com/minecraft/mc-mods/little-opener
[Alleluid]: https://www.curseforge.com/members/alleluid/projects
[LittleFrames]: https://www.curseforge.com/minecraft/mc-mods/littleframes
