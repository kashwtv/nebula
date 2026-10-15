# Nebula

Forge **1.20.1** client mod (Forge 47.4.10) for Kash's private server with friends.

## Features

- Combat, movement, visuals, HUD modules (see in-game Click GUI)
- **Structure Finder** (all dimensions) + end portal / stronghold locate
- **AutoBuild** from `.litematic` / `.schem` / `.nbt` schematics
- **Standalone Updater** in [`updater/`](updater/) — downloads the newest `nebula*.jar` from this repo's [Releases](https://github.com/kashwtv/nebula/releases/latest) into your `mods` folder

## Build

JDK 17:

```bash
./gradlew build
```

Jar: `build/libs/nebula-1.0.0.jar`

## Update (friends)

1. Grab `nebula-updater.jar` + `Nebula Updater.bat` from a Release (or build `updater/`).
2. Set `updater.json` url to:
   `https://api.github.com/repos/kashwtv/nebula/releases/latest`
3. Point `modsFolder` at your instance `mods` folder and click **Check & Update**.

## License

See `LICENSE.txt`.
