# The Other

[![Build](https://github.com/Sekai0NI0itamio/The-Other/actions/workflows/build.yml/badge.svg)](https://github.com/Sekai0NI0itamio/The-Other/actions/workflows/build.yml)

A Minecraft **Forge 1.20.1** horror mod and modpack built around one promise: the world gives you one life in **The Other**.

> Status: first playable slice. The title screen has only Play and Settings; Play creates or loads an infinite wheat field with subtle hills; natural mob spawning is disabled in every world. House copying is a separate mod, [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy). The dimension, progression, and one-life systems are documented in [`docs/`](docs/README.md) and are the next implementation phases.

- **Minecraft:** 1.20.1
- **Loader:** Forge 47.4.28
- **Java:** 17
- **Build:** ModDevGradle Legacy 2.0.148, Gradle 8.14.5, Parchment `2023.09.03`
- **Mod id:** `theother`
- **Package:** `io.github.sekai0ni0itamio.theother`

## Build (GitHub Actions only)

This project is built **online only**. Do not run a local build.

1. Push to `main`.
2. Open **Actions → Build** in the GitHub repository.
3. Download the `theother-jar` artifact from the green run.
4. Install the jar in a clean Forge 1.20.1 instance.

The workflow is [`.github/workflows/build.yml`](.github/workflows/build.yml). Pushing a tag such as `v0.1.0` runs [`.github/workflows/release.yml`](.github/workflows/release.yml), which builds online and attaches the jar to a GitHub release.

Requires JDK 17 for the mod toolchain; the workflow runs Gradle on JDK 21. See [Build and test](docs/10-build-and-test.md) for CI logs, artifacts, and troubleshooting.

## Documentation

| Guide | What it covers |
| --- | --- |
| [Template choice](docs/00-template-choice.md) | Why this repo uses ModDevGradle Legacy instead of the official ForgeGradle MDK, Architectury, or NeoForge |
| [Architecture](docs/01-architecture.md) | Project layout, mod lifecycle, event buses, sides, registries, networking |
| [Title screen](docs/02-title-screen.md) | Resource-pack, Forge-event, and custom-`Screen` approaches with 1.20.1 code |
| [Custom dimensions](docs/03-custom-dimensions.md) | `dimension_type` and `dimension` JSON, custom chunk generators, teleporting, dimension effects |
| [World generation](docs/04-world-generation.md) | Superflat start, biomes, features, structures, NBT houses, jigsaw pools |
| [Progression and state](docs/05-progression-and-state.md) | Phase machine, `SavedData`, chest/sign triggers, world swaps |
| [One life](docs/06-one-life.md) | Permadeath enforcement, install tokens, edge cases, anti-tamper reality |
| [Atmosphere](docs/07-atmosphere.md) | Eternal night, fog, the darkening cinematic, sound, screen effects |
| [Assets and data](docs/08-assets-and-data.md) | Models, textures, sounds, loot tables, tags, data generation |
| [Modpack](docs/09-modpack.md) | Forge 1.20.1 performance stack, distribution formats, compatibility |
| [Build and test](docs/10-build-and-test.md) | Commands, CI, common failures, release checklist |
| [References](docs/11-references.md) | Primary sources and further reading |
| [Wheat world](docs/12-wheat-world.md) | The Play flow, the wheat generator, and the global no-spawn rule |
| [Ready-to-run test pack](docs/14-ready-modpack.md) | The local performance/QoL mod jars, Forge installer, and install steps |
| [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy) | The separate structure-export mod and the five house maps |

## Repository layout

```text
The Other/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/main/java/io/github/sekai0ni0itamio/theother/
│   ├── TheOther.java                  common entry point
│   └── client/                        client-only code
│       ├── TheOtherClient.java        title screen hook
│       └── TheOtherTitleScreen.java   custom title screen
├── src/main/resources/                assets and data shipped in the jar
├── src/main/templates/                expanded metadata (mods.toml, pack.mcmeta)
├── src/generated/resources/           data generator output
├── docs/                              implementation guides
└── .github/workflows/build.yml        CI build
```

## Feature Plan

*Based strictly on what has been described so far. No additions or gap-filling.*

---

## 1. Overview

This is both a **modpack** and a **new mod** from the creator.

- **Modpack**: Performance mods + additional mods
- **Mod**: The actual horror content

**Target version**: Latest version of Minecraft, or most downloaded versions (e.g., 1.20.1 Fabric/Forge)

---

## 2. Core Mechanic: One Life

- The modpack/mod only gives the player **one life** when they enter **"The Other"** world
- If the player dies in The Other, they **cannot play this world anymore**
- To play again, they must **redownload the modpack** and **reinstall** it
- On the surface (forest dimension), gamemode is **survival** (not hardcore) — player can live there indefinitely

---

## 3. World Flow & Progression

### Phase 1: The Peaceful World (Starting World)

- Player clicks play → spawns in a **seemingly peaceful super flat world**
- Terrain: hills of wheat, golden hills
- One house in the distance
- No mobs spawn there
- Player can choose to live in this peaceful world forever

### Phase 2: The Secret (Inside the House)

- Player digs out the **carpets** inside the house
- Under the carpets: a **secret trap door**
- Opening the trap door leads to a **ladder going down a few 10 blocks**
- At the bottom: a room further back
- In the room: a **chest** and a **sign** saying: *"do not open, or else"*
- The sign is described as "unfinished"

### Phase 3: The Trigger (Opening the Chest)

- When the chest is opened, it contains **one enchanted rose**
- At this moment, the **entire world outside changes**
- The bottom basement stays the same, **except** the sign changes to: *"take the rose"*

### Phase 4: The Forest Dimension

- When the player climbs back up, the trap door **no longer leads to the house**
- It now leads to a **ghosty smoke forest**
- In the middle of a **mud road**
- (Modern version preferred — use newest mud blocks)
- The forest allows the user to walk
- Leads to a **wide circular podium**
- On the podium: a **statue of a rose in stone**
- At the very top of the statue: a **pot** to place the rose

### Phase 5: Player Choice (Forest Dimension)

- Player can choose to **not place the rose**
- Player can **live in this dimension**:
  - Forever night
  - Time does pass
  - Waterways present
  - Trees and grass present
  - Player can start farms, etc.
- Gamemode: **survival** (not hardcore)

### Phase 6: "The Other" World

- When the player places the rose in the pot:
  - Chunks darken very fast from the distance to the player
  - Everything becomes black
  - Terrain changes again
  - Player is now officially in a new world named: **"The Other"**
- In The Other: **one life only**
  - If the player dies, they cannot play this world anymore
  - Must redownload the modpack and reinstall to play again

---

## 4. Summary of Player Journey

```text
[Peaceful Super Flat World]
  ↓ (live forever, no mobs)
[House] → dig carpets → [Trap Door] → ladder down → [Basement Room]
  ↓
[Chest + Sign: "do not open, or else"]
  ↓ (open chest)
[Enchanted Rose] → world outside changes → sign changes to "take the rose"
  ↓ (climb back up)
[Ghosty Smoke Forest] (forever night, mud road, waterways, trees, grass)
  ↓ (walk)
[Circular Podium + Stone Rose Statue + Pot]
  ↓
  ├── [Don't place rose] → live in forest forever (survival)
  └── [Place rose] → chunks darken → "The Other" (one life, permadeath on death)
```

---

## 5. Modpack Contents

- The Other (described above; no hostile entities)
- Performance mods
- Quality-of-life mods such as Mouse Tweaks, JEI, and AppleSkin

Natural mob spawning is disabled by the mod. The pack does not add entity, horror, stalker, or shader mods.

version/loader of mod:
Primary	1.20.1	Forge	The Boiled One has 826K downloads on 1.20.1 Forge alone. Twixxel's Stalker is 1.20.1 Forge. The horror audience is here.
