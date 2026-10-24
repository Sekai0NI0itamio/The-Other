# Ready-to-run test pack

A local bundle was assembled for testing The Other with a performance and quality-of-life stack. The bundle itself is **not committed** (third-party jars); this guide records what it contains and how to install it.

House copying is a separate mod and has its own single-folder kit: [HouseCopyKit](../../HouseCopyKit) and [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy).

## Bundle contents

| Path | Contents |
| --- | --- |
| `modpack/mods/` | 20 jars: The Other CI jar, performance mods, Mouse Tweaks, JEI, and QoL |
| `modpack/forge-1.20.1-47.4.28-installer.jar` | Forge installer |
| `modpack/MODS.md` | Generated list of every jar with its mod id and version |

The bundle does not contain world saves. The five house maps live in `../maps/` and the ready-to-copy kit at `../HouseCopyKit/`.

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
4. Launch.

## Use

- **Play** on the title screen creates/loads the wheat world.
- No natural mobs spawn anywhere. The mod blocks natural, chunk-generation, spawner, structure, patrol, and reinforcement spawns. Spawn eggs, `/summon`, and player-driven breeding still work for testing.
- For house copying, install the separate HouseCopy mod. It does not change the title screen.

## Why the bundle is not in git

- The mod jars are third-party content.
- The repository should stay a code/documentation project; the bundle is a local test artifact.
- `.gitignore` excludes `modpack/` and `maps/`.

## Caveats

- The wheat world is generated once. Delete `saves/TheOther` to regenerate it after a generator change.
- The Other does not contain the house export command. Use HouseCopy.

## See also

- [Wheat world](12-wheat-world.md)
- [Modpack planning](09-modpack.md)
- [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy)
- [House maps (HouseCopy)](https://github.com/Sekai0NI0itamio/HouseCopy/blob/main/docs/house-maps.md)
