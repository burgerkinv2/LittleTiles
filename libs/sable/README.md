# Local Sable compile-only jars

LittleTiles uses Sable APIs only for optional compatibility code. The Sable jars
are compile-only dependencies and are not bundled in the LittleTiles mod jar.

Place these files in this directory before building:

```text
sable-neoforge-1.21.1-1.1.3.jar
sable-companion-common-1.21.1-1.6.0.jar
```

The repository intentionally does not commit those jars. Sable is licensed under
PolyForm Shield License 1.0.0, and Sable Companion is licensed separately.

If your jars are stored elsewhere, pass an absolute or project-relative path:

```powershell
.\gradlew.bat :LittleTiles:build -PsableLibDir=F:/path/to/sable-jars
```
