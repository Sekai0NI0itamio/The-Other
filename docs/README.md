# The Other — implementation documentation

This directory is the working handbook for building **The Other** on Minecraft Forge 1.20.1. It explains both the code theory and the concrete 1.20.1 APIs, so the feature plan can be implemented without guesswork.

## Reading order

| # | Guide | Read it when |
| --- | --- | --- |
| 00 | [Template choice](00-template-choice.md) | You want to know why this repo is built the way it is |
| 01 | [Architecture](01-architecture.md) | You are adding the first real feature or registering content |
| 02 | [Title screen](02-title-screen.md) | You are changing the menu, panorama, logo, splash, or music |
| 03 | [Custom dimensions](03-custom-dimensions.md) | You are creating the peaceful world, the forest, or The Other |
| 04 | [World generation](04-world-generation.md) | You are generating terrain, biomes, the house, the podium, or structures |
| 05 | [Progression and state](05-progression-and-state.md) | You are wiring the chest, sign, trapdoor, and phase transitions |
| 06 | [One life](06-one-life.md) | You are implementing permadeath and the reinstall reset |
| 07 | [Atmosphere](07-atmosphere.md) | You are implementing darkness, fog, the chunk-darkening cinematic, or sound |
| 08 | [Assets and data](08-assets-and-data.md) | You need models, textures, sounds, loot tables, tags, or datagen |
| 09 | [Modpack](09-modpack.md) | You are assembling the Forge 1.20.1 modpack and performance stack |
| 10 | [Build and test](10-build-and-test.md) | You are running CI, debugging a build, or preparing a release |
| 11 | [References](11-references.md) | You need the primary documentation |
| 12 | [Wheat world](12-wheat-world.md) | You are testing Play or the wheat generator |
| 14 | [Ready-to-run test pack](14-ready-modpack.md) | You are installing the local performance/QoL jars and Forge installer |

## Ground rules for this project

- **Forge 1.20.1 only.** Do not introduce Fabric, NeoForge, or Architectury APIs. NeoForge did not exist for 1.20.1.
- **Server authority.** World state, progression, and the one-life check belong on the logical server. The client may render atmosphere but must not decide whether a life is spent.
- **Data over code where possible.** Dimensions, biomes, features, structures, loot, and recipes are JSON + NBT. Use code only for behaviour that JSON cannot express.
- **Client code stays in `client/`.** Dedicated servers cannot load client classes. Guard client-only code with `Dist.CLIENT` and `@Mod.EventBusSubscriber`.
- **No hardcoded coordinates across dimensions.** Store dimension ids and anchor positions in one place, and resolve levels through `ServerLevel`.
- **The plan is the source of truth.** When a detail is missing, document the decision instead of silently inventing gameplay.

## Current state

| Area | State |
| --- | --- |
| Build + CI | Working scaffold; GitHub Actions builds every push |
| Title screen | Implemented as a dark custom `Screen` with only Play and Settings |
| Registries | Wheat chunk generator registered; items, blocks, sounds, and entities are next |
| Wheat world | Play creates or loads `TheOther`, an infinite wheat field with subtle hills |
| House copy | Separate mod: [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy) |
| Dimensions | Documented, not yet defined |
| World generation | Documented, not yet defined |
| Progression | Documented, not yet implemented |
| One life | Documented, not yet implemented |
| Modpack | Documented, not yet assembled |

## How to use a guide

Each guide follows the same shape:

1. **What the game does** — the vanilla pipeline and the classes involved.
2. **How to hook in** — the Forge event, registry, or data file.
3. **A 1.20.1 example** — code or JSON that matches this repository's package and mod id.
4. **Traps** — the mistakes that cost the most time.
5. **Verification** — what to run in-game and what success looks like.

The examples target the mappings pinned in `gradle.properties` (official mappings + Parchment `2023.09.03`). If a signature differs, check the mapped class before copying.
