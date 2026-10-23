# Template choice: ModDevGradle Legacy for Forge 1.20.1

## Decision

This repository uses the **ModDevGradle Legacy Forge** plugin, pinned to **2.0.148**, against **Minecraft 1.20.1 + Forge 47.4.28**, with **Parchment `2023.09.03`**, **Gradle 8.14.5**, and a **Java 17** toolchain.

The starting point is the NeoForged-maintained 1.20.1 mirror:

- <https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle>

That template is a copy of the 1.20.1 branch of the NeoForge MDK, adapted to the `net.neoforged.moddev.legacyforge` plugin. It is the most current maintained 1.20.1 Forge template we could verify. Its template files are MIT-licensed; the notice is kept in [`TEMPLATE_LICENSE.txt`](../TEMPLATE_LICENSE.txt).

The mod's own code and assets are **All Rights Reserved** until a licence is chosen. Change `mod_license` in `gradle.properties` and add a `LICENSE` file when that decision is made.

## Why this template

| Requirement | Why ModDevGradle Legacy wins |
| --- | --- |
| Modern Gradle | Ships Gradle 8.14.5, configuration cache, and toolchain support |
| Modern Java | Gradle runs on JDK 21 in CI while the mod targets Java 17 |
| Mapping quality | Parchment parameter names and Javadoc are built into the template |
| Setup speed | MDG caches the Minecraft artifact and skips decompilation in CI (`CI=true`) |
| Maintenance | The legacy plugin is released alongside ModDevGradle; 2.0.148 shipped in September 2026 |
| Mixins | A documented `mixin` block adds the refmap and manifest attribute when needed |
| Forge compatibility | Produces a normal reobfuscated Forge jar for 1.20.1 |

## What we compared

### Official Forge MDK (ForgeGradle 6)

The canonical option is the `mdk` folder from the `1.20.1` branch of MinecraftForge, or the `forge-1.20.1-47.4.28-mdk.zip` artifact from Forge Maven.

**Pros:** maximum compatibility with old tutorials; the exact buildscript Forge documents for 1.20.1; every 1.20.1 modding guide assumes it.

**Cons:** ForgeGradle 6 is in maintenance mode, the MDK pins Gradle 8.8, setup is slower, and Parchment requires extra configuration. It is still a valid fallback if ModDevGradle ever blocks a dependency.

**Verdict:** keep as the fallback, not the default.

### Architectury template

Architectury generates a multi-loader workspace for Forge, Fabric, and NeoForge.

**Pros:** one codebase for several loaders; useful if a Fabric release is a hard requirement later.

**Cons:** an extra abstraction layer, an Architectury API dependency, more build complexity, and more places for 1.20.1-specific behaviour to break. The target audience described in the plan is the 1.20.1 **Forge** horror audience, so this cost buys nothing today.

**Verdict:** rejected for the initial release. Revisit only if a Fabric port becomes a product requirement.

### NeoForge

NeoForge's first release targets 1.20.2. It does not exist for 1.20.1.

**Verdict:** impossible for this target.

### Fabric

Fabric would mean Yarn mappings, Fabric API, a different event model, and a different modpack ecosystem. The plan explicitly anchors the horror audience to 1.20.1 Forge (The Boiled One, Twixxel's Stalker).

**Verdict:** rejected.

### Hand-rolled ForgeGradle or a bare Gradle build

No advantage over the official MDK and a large maintenance burden.

**Verdict:** rejected.

## Pinned versions

| Component | Value | Where |
| --- | --- | --- |
| Minecraft | `1.20.1` | `gradle.properties` |
| Forge | `47.4.28` (latest 1.20.1 build at time of writing) | `gradle.properties` |
| ModDevGradle Legacy | `2.0.148` | `build.gradle` |
| Gradle | `8.14.5` | `gradle/wrapper/gradle-wrapper.properties` |
| Java toolchain | `17` | `build.gradle` |
| Parchment Minecraft | `1.20.1` | `gradle.properties` |
| Parchment mappings | `2023.09.03` | `gradle.properties` |
| Pack format | `15` | `src/main/templates/pack.mcmeta` |

Forge 1.20.1 builds are published at <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html>. The latest version can be checked from Maven metadata:

```text
https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml
```

The Parchment version is listed at <https://parchmentmc.org/docs/getting-started>.

## What the template changed for this mod

- Mod id: `examplemod` → `theother`
- Package: `com.example.examplemod` → `io.github.sekai0ni0itamio.theother`
- Display name: `Example Mod` → `The Other`
- Forge: `47.1.3` → `47.4.28`
- ModDevGradle: `2.0.91` → `2.0.148`
- Example blocks, items, config, and creative tab removed
- A dark custom title screen added as the first client feature
- GitHub Actions upgraded to upload the built jar as an artifact
- Documentation added under `docs/`

## Upgrade policy

1. Read the ModDevGradle Legacy notes: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>.
2. Bump one axis at a time: Forge, then MDG, then Gradle.
3. Push to `main` and let the GitHub Actions workflow build. Do not rely on a local build.
4. If the build fails, read the CI log and revert the bump before changing mod code.
5. Keep `minecraft_version_range` and `forge_version_range` honest. A 1.20.1 mod must not claim compatibility with 1.21.

## Sources

- Forge 1.20.1 documentation: <https://docs.minecraftforge.net/en/1.20.1/>
- Forge 1.20.1 MDK: <https://github.com/MinecraftForge/MinecraftForge/tree/1.20.1/mdk>
- ModDevGradle Legacy: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>
- NeoForged 1.20.1 Forge template: <https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle>
- Parchment: <https://parchmentmc.org/docs/getting-started>
- Forge downloads: <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html>
