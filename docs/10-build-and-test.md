# Build and test: GitHub Actions is the build

## The rule for this repository

**Do not build locally. Push, and let GitHub Actions build.**

The local machine is for editing and reviewing. The canonical build is the `Build` workflow in `.github/workflows/build.yml`, and the artifact it uploads is the build you ship. This keeps the result reproducible and avoids "works on my machine" drift.

If you need to inspect what a Gradle task does, read `build.gradle` and the CI log. Do not run `./gradlew build` on the workstation and treat that as verification.

## The CI workflow

```yaml
name: Build

on: [push, pull_request]

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          fetch-tags: true

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build with Gradle
        run: ./gradlew build

      - name: Upload mod jar
        uses: actions/upload-artifact@v4
        with:
          name: theother-jar
          path: build/libs/*.jar
          if-no-files-found: error
```

What each part does:

- **`on: [push, pull_request]`** — every push and every PR builds. A broken `main` is visible immediately.
- **JDK 21** — Gradle runs on Java 21. The mod itself targets Java 17 through the Gradle toolchain, so the produced jar is 1.20.1-compatible.
- **`gradle/actions/setup-gradle@v4`** — caches Gradle distributions and dependencies. The first run is slow; later runs are much faster.
- **`./gradlew build`** — compiles, processes resources, generates metadata, reobfuscates, and runs tests.
- **`actions/upload-artifact@v4`** — stores `build/libs/*.jar` as a downloadable artifact. `if-no-files-found: error` turns a silent empty upload into a failure.
- **`permissions: contents: read`** — least privilege. The build does not need write access.

`CI=true` is set by GitHub Actions automatically. ModDevGradle Legacy 2.0.124+ disables decompilation/recompilation when it detects CI, which is why the online build is much faster than a cold local setup.

## Watching a build

With the GitHub CLI:

```bash
gh run list --limit 5
gh run watch <run-id>
gh run view <run-id> --log-failed
gh run view <run-id> --log
```

Download the artifact:

```bash
gh run download <run-id> -n theother-jar -D artifacts/
```

The jar is `artifacts/theother-0.1.0.jar` (the version comes from `mod_version`). The non-reobfuscated development jar, if produced, is under `build/devlibs`; do not ship it.

In the browser: **Actions → Build → the run → Summary → Artifacts**.

## What a green run means

- The Java sources compile against the pinned Minecraft/Forge mappings.
- `generateModMetadata` expanded `mods.toml` and `pack.mcmeta`.
- Resources and models were processed.
- The jar was reobfuscated for production.
- The workflow uploaded a non-empty artifact.

A green run does **not** prove gameplay works. It proves the code builds. Runtime checks still need a client/server launch, which is a separate verification step (see below).

## Reading a failure

1. Open the failed job, not the whole run.
2. Read the **first** error, not the last stack trace.
3. The failing task name tells you where to look:
   - `compileJava` — Java code, imports, mappings.
   - `processResources` — metadata placeholders, missing resource.
   - `createMinecraftArtifacts` — Forge/Minecraft download, version mismatch.
   - `generateModMetadata` — `mods.toml` placeholder or `gradle.properties`.
   - `reobfJar` — mapping/reobfuscation issue.
   - `test` — a real test failure.
4. Fix the cause, commit, push. The workflow reruns automatically.
5. Never "fix" CI by deleting the failing check or weakening a test. That is a defect, not a fix.

Common failures in this repository:

| Failure | Cause | Fix |
| --- | --- | --- |
| `cannot find symbol: SelectWorldScreen` | 1.20.1 moved it to `...screens.worldselection` | Fix the import (this happened in the first build) |
| `cannot find symbol: JoinMultiplayerScreen` | 1.20.1 moved it to `...screens.multiplayer` | Fix the import |
| `Could not find net.minecraftforge:forge:1.20.1-...` | Forge version does not exist | Check the Forge Maven metadata and `forge_version` |
| `Unsupported class file major version` | Gradle running on an old JDK | The workflow uses JDK 21; check a fork's workflow |
| Toolchain not found | No JDK 17 and foojay cannot download | Keep the foojay resolver in `settings.gradle` |
| `Permission denied: ./gradlew` | Executable bit lost | `git update-index --chmod=+x gradlew` |
| `mods.toml` placeholder error | A `${...}` key is missing in `gradle.properties` | Add the property or remove the placeholder |
| Artifact upload fails | `build/libs/*.jar` empty | The `jar`/`reobfJar` task failed; read earlier logs |
| Mixin refmap warning/crash | Mixins enabled without the refmap | Add the `mixin` block and the annotation processor |

## Runtime verification

The build artifact must still be launched. Use a clean 1.20.1 Forge instance:

1. Install Forge 47.4.28 for 1.20.1.
2. Put the downloaded jar in `mods/`.
3. Launch the client.
4. Confirm:
   - The title screen is the custom dark screen.
   - Singleplayer, Multiplayer, Options, and Quit work.
   - A world loads without a missing registry crash.
   - The log contains `The Other is loading`.
5. For dimensions/progression, use the test commands in the relevant guides.

If a runtime failure appears, fix the code, push, and download a new artifact. Do not patch the jar by hand.

## Release workflow

A tag build is the cleanest way to publish. Add `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build with Gradle
        run: ./gradlew build

      - name: Create GitHub release
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: gh release create "$GITHUB_REF_NAME" build/libs/*.jar --generate-notes
```

Release steps:

1. Bump `mod_version` in `gradle.properties`.
2. Update the changelog.
3. Commit and push to `main`; wait for the `Build` workflow to pass.
4. Tag: `git tag v0.1.0 && git push origin v0.1.0`.
5. The `Release` workflow builds online and attaches the jar to the GitHub release.
6. Download the release asset and verify it launches in a clean instance.
7. Upload the same jar to CurseForge/Modrinth if those platforms are used.

Never upload a locally built jar. The release asset must come from Actions.

## Version bumps

Change one axis at a time:

1. **Forge**: update `forge_version` and `forge_version_range`, push, watch CI.
2. **ModDevGradle Legacy**: update the plugin version in `build.gradle`, push, watch CI.
3. **Gradle**: update `gradle/wrapper/gradle-wrapper.properties`, push, watch CI.
4. **Parchment**: update `parchment_mappings_version` and `parchment_minecraft_version`, push, watch CI.

If CI fails, revert the bump before changing mod code. Mixing a buildsystem change with a gameplay change makes the failure ambiguous.

## Tests

The workflow runs `./gradlew build`, which runs the `test` task if tests exist. This scaffold has no tests. Add pure-JVM tests for logic that does not need Minecraft classes:

- Phase transition guards.
- Installation-token comparison.
- Life-record serialization (with a `CompoundTag`; this needs Minecraft on the test classpath, so keep it in a game test or a separate integration test).
- Command parsing and config validation.

Game tests can be added with the `gameTestServer` run and `@GameTest`; they are slower and better suited to world interactions. The CI workflow currently does not run `runGameTestServer`; add a separate job if the test suite grows.

## Debugging without a local build

- Add temporary `LOGGER.info` lines, push, download the artifact, run it, and read `logs/latest.log`.
- Use `/reload` for data changes and restart for dimension/code changes.
- Use `/execute in <dimension> run tp` to test dimension availability.
- Use `Spark` or the built-in debug profiler for performance, not guesswork.
- If the bug only appears with the full modpack, reproduce with the pack's exact mod list and configs.

## Security and supply chain

- Pin GitHub Actions to a major version (`@v4`); for higher assurance, pin a commit SHA.
- Do not put secrets in the workflow. The default `GITHUB_TOKEN` is enough for a release.
- Do not run untrusted PR code with write permissions. The build workflow is read-only.
- Review dependency additions; a mod build pulls Minecraft, Forge, mappings, and Gradle plugins from external repositories.
- The mod jar is unsigned. Signing is optional for Forge mods; if you sign, document the key custody.

## Verification standard

A change is done when:

1. The `Build` workflow is green on the commit.
2. The artifact downloads and launches in a clean instance.
3. The specific feature passes its manual test (title screen, dimension, chest, etc.).
4. The log has no new errors or missing-registry warnings.
5. The diff contains only intended files.

A local build is not part of this standard.

## Sources

- GitHub Actions: <https://docs.github.com/en/actions>
- `actions/setup-java`: <https://github.com/actions/setup-java>
- `gradle/actions`: <https://github.com/gradle/actions>
- `actions/upload-artifact`: <https://github.com/actions/upload-artifact>
- ModDevGradle Legacy: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>
- Forge 1.20.1 downloads: <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html>
