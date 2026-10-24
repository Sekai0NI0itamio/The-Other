# Ready-to-run test pack

A local bundle was assembled for testing the wheat world and exporting a house. The bundle itself is **not committed** (third-party jars and map saves); this guide records what it contains and how to install it.

## House-copy kit

For the minimal house-copy workflow there is a separate single folder:

```text
house-copy-kit/
├── mods/theother-0.1.0.jar
├── saves/          (the five maps)
└── forge-1.20.1-47.4.28-installer.jar
```

Use this when you only want to open a downloaded world and export a house. Use the larger `modpack/` when you want the performance/QoL stack too.

## Bundle contents

| Path | Contents |
| --- | --- |
| `modpack/mods/` | 20 jars: The Other CI jar, performance mods, Mouse Tweaks, JEI, and QoL |
| `modpack/saves/` | The five downloaded house maps, unzipped |
| `modpack/forge-1.20.1-47.4.28-installer.jar` | Forge installer |
| `modpack/MODS.md` | Generated list of every jar with its mod id and version |

## Core jar list

| Mod | Version | File |
| --- | --- | --- |
| The Other | `0.1.0` | `theother-0.1.0.jar` |
| Embeddium | `0.3.31+mc1.20.1` | `embeddium-0.3.31+mc1.20.1.jar` |
| Ferrite Core | `6.0.1` | `ferritecore-6.0.1-forge.jar` |
| ModernFix | `5.27.89+mc1.20.1` | `modernfix-forge-5.27.89+mc1.20.1.jar` |
| Canary | `0.3.3` | `canary-mc1.20.1-0.3.3.jar` |
| Saturn | `0.1.3` | `saturn-mc1.20.1-0.1.3.jar` |
| Starlight | `1.1.2+forge.1cda73c` | `starlight-1.1.2+forge.1cda73c.jar` |
| Entity Culling | `1.11.3` | `entityculling-forge-1.11.3-mc1.20.1.jar` |
| ImmediatelyFast | `1.5.5+1.20.4` | `ImmediatelyFast-Forge-1.5.5+1.20.4.jar` |
| Clumps | `12.0.0.4` | `Clumps-forge-1.20.1-12.0.0.4.jar` |
| AI Improvements | `0.5.2` | `AI-Improvements-1.20-0.5.2.jar` |
| Memory Leak Fix | `1.1.5` | `memoryleakfix-forge-1.17+-1.1.5.jar` |
| Packet Fixer | `3.3.2` | `packetfixer-3.3.2-1.18-1.20.4-merged.jar` |
| JEI | `15.56.0.205` | `jei-1.20.1-forge-15.56.0.205.jar` |
| Controlling | `12.0.2` | `Controlling-forge-1.20.1-12.0.2.jar` |
| Searchables (dependency) | `1.0.3` | `Searchables-forge-1.20.1-1.0.3.jar` |
| Toast Control | `8.0.3` | `ToastControl-1.20.1-8.0.3.jar` |
| Placebo (dependency) | `8.6.3` | `Placebo-1.20.1-8.6.3.jar` |
| AppleSkin | `2.5.1+mc1.20.1` | `appleskin-forge-mc1.20.1-2.5.1.jar` |
| Mouse Tweaks | `1.20.1-2.25.1` | `MouseTweaks-forge-mc1.20.1-2.25.1.jar` |

All files were downloaded from Modrinth and verified against their SHA-1 hashes. The Other jar is the GitHub Actions artifact.

## Install

1. Install Java 17.
2. Create a 1.20.1 Forge **47.4.28** instance, or run `forge-1.20.1-47.4.28-installer.jar`.
3. Copy `modpack/mods/*.jar` to the instance's `mods/`.
4. Copy `modpack/saves/*` to the instance's `saves/`.
5. Launch.

## Use

- **Play** on the title screen creates/loads the wheat world.
- No natural mobs spawn anywhere. The mod blocks natural, chunk-generation, spawner, structure, patrol, and reinforcement spawns. Spawn eggs, `/summon`, and player-driven breeding still work for testing.
- **Shift + Play** opens the vanilla world list, so a downloaded map can be opened and its house edited.
- Export the house with:
  ```mcfunction
  /housecopy export house_name x1 y1 z1 x2 y2 z2
  ```
- The file is written to `<instance>/.minecraft/theother-exports/house_name.nbt`. The `/theother export` alias also works.

## Why the bundle is not in git

- The map saves and mod jars are third-party content and are large (about 190 MB of saves plus jars).
- The repository should stay a code/documentation project; the bundle is a local test artifact.
- `.gitignore` excludes `modpack/` and `maps/`.

## Caveats

- Two maps (`CASE`, `home`) are older worlds and will be upgraded by 1.20.1 on first load.
- The wheat world is generated once. Delete `saves/TheOther` to regenerate it after a generator change.
- The export command excludes entities and caps the selection at 128 blocks per axis.

## See also

- [Wheat world and export](12-wheat-world-and-export.md)
- [House maps](13-house-maps.md)
- [Modpack planning](09-modpack.md)
