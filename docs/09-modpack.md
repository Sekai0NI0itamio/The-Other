# The modpack: Forge 1.20.1 performance stack and distribution

The mod is one half of the product; the pack is the other. The pack must be stable enough that the horror reads as intentional, not as a crash or a stutter.

## Base

| Component | Value |
| --- | --- |
| Minecraft | 1.20.1 |
| Loader | Forge 47.4.28 |
| Java | 17 (bundled by the launcher or installed) |
| Memory | 4–6 GB recommended; 3 GB is a floor for a light pack |
| Renderer | Embeddium |

Do not ship OptiFine. It conflicts with Embeddium and with several modern performance mods.

## Core performance stack

Verify each mod has a 1.20.1 Forge build and read its compatibility notes before pinning a version. The list is a starting point, not a mandate.

| Mod | Role | Notes |
| --- | --- | --- |
| **Embeddium** | Sodium-style chunk rendering | The base renderer; do not stack with OptiFine |
| **FerriteCore** | Reduces blockstate/model memory | Safe, widely used |
| **ModernFix** | Startup, memory, and mixin fixes | Check the config; some options are aggressive |
| **Canary** | Lithium-style game-logic optimizations | Choose one Lithium port; do not stack Canary and Radium |
| **Saturn** | Memory/GC reductions | Lightweight |
| **Starlight** | Lighting engine | Do not combine with another lighting engine |
| **Entity Culling** | Skips hidden entity rendering | Safe; test with custom entities |
| **ImmediatelyFast** | Batches immediate-mode rendering | Safe; check GUI mod compatibility |
| **Clumps** | Merges XP orbs | Safe; changes XP visuals slightly |
| **AI Improvements** | Cheaper mob AI | No natural mobs expected; harmless if kept |
| **Memory Leak Fix** | Patches known leaks | Check the 1.20.1 build |
| **Packet Fixer** | Packet size/limits | Useful on large modpacks |
| **Spark** | Profiler | Keep it optional or dev-only |

Rules:

- One optimization per domain. Two Lithium ports, two lighting engines, or two renderers will fight.
- Add mods one at a time and test a new world, a loaded world, and the custom dimensions.
- Read the issue tracker for the exact versions you pin. A "latest" build can be a regression.
- Keep a `mods.txt` or a manifest comment with the reason for each mod. Future you will not remember.

## No entity, horror, or atmosphere add-ons

The Other does not add hostile entities, and the pack does not add any entity mods. The intended stack is:

- The Other
- performance mods
- quality-of-life mods

Do not add stalker, Herobrine, or mob mods. Natural mob spawning is disabled by the mod itself, so an entity mod would either fight the rule or be dead weight. Shader and sound-physics mods are also out of scope; the mod's own fog, darkness, and sound design carry the atmosphere.

## Quality of life

| Mod | Why |
| --- | --- |
| **Mouse Tweaks** | Drag-and-drop inventory management |
| **JEI** | Players need to understand the rose/pot recipe |
| **Controlling / Searchable** | Find keybinds |
| **AppleSkin** | Hunger/saturation clarity |
| **Toast Control** | Remove tutorial toasts that break the mood |
| **Jade / WTHIT** | Optional; block info reduces mystery, make it configurable |

Do not add quest mods, minimaps, or waypoint mods by default. They solve problems the horror is trying to create.

## JVM settings

A starting point for a 4 GB allocation:

```text
-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled
-XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC -XX:+AlwaysPreTouch
```

Notes:

- More memory is not always better. 8 GB with a 4 GB heap wastes RAM and can increase GC pauses.
- Do not copy 20-flag "gaming" lists. Test with Spark if the pack stutters.
- On macOS/Linux, ensure the launcher uses Java 17, not Java 8 or 25.
- The mod targets Java 17 bytecode. A newer JVM can run it, but the pack should standardize on 17 for support.

## Distribution formats

### CurseForge

`manifest.json` at the pack root:

```json
{
  "minecraft": {
    "version": "1.20.1",
    "modLoaders": [
      {
        "id": "forge-47.4.28",
        "primary": true
      }
    ]
  },
  "manifestType": "minecraftModpack",
  "manifestVersion": 1,
  "name": "The Other",
  "version": "0.1.0",
  "author": "Sekai0NI0itamio",
  "files": [
    {
      "projectID": 123456,
      "fileID": 789012,
      "required": true
    }
  ],
  "overrides": "overrides"
}
```

- `files` uses CurseForge project and file IDs, not jar names. The launcher downloads them.
- Configs, resource packs, and the prebuilt world go in `overrides/`.
- Do not bundle mod jars unless the licence explicitly allows redistribution.

### Modrinth

`.mrpack` with `modrinth.index.json`:

```json
{
  "formatVersion": 1,
  "game": "minecraft",
  "versionId": "0.1.0",
  "name": "The Other",
  "files": [
    {
      "path": "mods/example.jar",
      "hashes": {
        "sha1": "...",
        "sha512": "..."
      },
      "env": {
        "client": "required",
        "server": "unsupported"
      },
      "downloads": [
        "https://cdn.modrinth.com/data/.../versions/.../example.jar"
      ]
    }
  ],
  "dependencies": {
    "minecraft": "1.20.1",
    "forge": "47.4.28"
  }
}
```

- Generate it with the Modrinth pack format tooling rather than hand-editing hashes.
- `env` controls client/server installation. A client-only mod should be `client: required`, `server: unsupported`.
- Include `overrides/` for configs and the world.

### Prism / MultiMC

The simplest private distribution: a Prism instance folder with `.minecraft/mods`, `config`, `saves`, and `instance.cfg`. Zip it and share. This is fine for testing, not for a public pack with mod redistribution.

## Shipping the starting world

The plan's opening is authored, not procedural. The most reliable delivery is a prebuilt world in `overrides/saves/`:

1. Create the world in a dev instance with the final mod versions.
2. Build the house, basement, chest, sign, carpet, trapdoor, ladder, and the spawn area.
3. Set the world spawn.
4. Save and close.
5. Copy the world folder into `overrides/saves/The Other/`.
6. Test a fresh install: the world appears in the list, the player spawns correctly, and the chest triggers.

Do not ship the installation token in the world. It lives in `config/theother/installation.json` and is generated per install.

If the world is too large, trim explored chunks or use a smaller world border. A horror map does not need a 10,000-block explored radius.

## Test matrix

| Profile | What to test |
| --- | --- |
| Clean mod only | Title screen, wheat world, chest trigger, one-life lock |
| Full pack | Same, plus performance and QoL mods |
| Low-end | 4 GB, render distance 8 |
| Dedicated server | The mod loads without client classes; dimensions exist; one-life is enforced server-side |
| Fresh install | The prebuilt world appears; the token is created; the reset works |
| Reinstall | The token changes; the lock resets as designed |

## Release checklist

- [ ] `gradle.properties` versions are final (`mod_version`, Forge, Parchment).
- [ ] GitHub Actions `Build` is green on `main`.
- [ ] The jar is downloaded from the workflow artifact, not a local build.
- [ ] The jar loads in a clean 1.20.1 Forge instance.
- [ ] The title screen, dimensions, chest, sign, trapdoor, podium, and one-life flow work.
- [ ] The pack manifest pins every mod version and loads on a clean launcher.
- [ ] The prebuilt world is included and tested from a fresh install.
- [ ] Configs in `overrides/` are intentional; no dev-only settings.
- [ ] Credits and licences are complete.
- [ ] The changelog lists gameplay and pack changes.
- [ ] A backup of the previous pack version exists.

## Legal and platform notes

- Do not redistribute mods without permission. CurseForge and Modrinth provide author-controlled downloads; use them.
- Do not redistribute Mojang assets. The prebuilt world contains Mojang-generated terrain; that is normal for a world save, but do not extract and republish textures or sounds.
- Shader packs and resource packs have their own licences. Include them only if the licence allows it, or link to the author's page.
- Follow the platform's rules for paid access, advertising, and external links.
- A public modpack needs a support channel and a way to report crashes. A GitHub issue template is enough.

## Sources

- Forge 1.20.1 downloads: <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html>
- Embeddium: <https://www.curseforge.com/minecraft/mc-mods/embeddium>
- FerriteCore: <https://www.curseforge.com/minecraft/mc-mods/ferritecore>
- ModernFix: <https://www.curseforge.com/minecraft/mc-mods/modernfix>
- CurseForge manifest format: <https://support.curseforge.com/en/support/solutions/articles/9000197913-modpack-manifest>
- Modrinth pack format: <https://docs.modrinth.com/docs/modpacks/format/>
