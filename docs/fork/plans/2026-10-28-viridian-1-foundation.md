# Viridian 1: the workspace and both grafts — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the private `Enchyria/viridian` Nx workspace with chaplet's tooling surface, bring PKForge in as `grafts/pkforge` and PokeDaisy as `grafts/pokedaisy`, and ship both from it through Nx release: PKForge Fork `3.1.1-fork.7` (the park keepsake) and PokeDaisy Fork `1.3.2-fork.2` (in-app update check off).

**Architecture:** A bare Nx 23 workspace (mise for tools, pnpm, lefthook + committed, oxlint/oxfmt) copied from chaplet at `1a801a1f29a6`. Each fork arrives by `git subtree add` with its own history; a local workspace plugin infers each graft's Nx targets from its files (`PKForge.sln` for .NET, `settings.gradle.kts` + `app/build.gradle.kts` for Gradle) so the grafts' build files stay upstream's. One CI workflow runs `nx affected`; one Release workflow (chaplet's shape) versions with a csproj version action, signs, verifies and publishes.

**Tech Stack:** Nx 23.3.0, TypeScript 5.9.3, Vitest 5.0.3, oxlint 1.85.0, oxfmt 0.70.0, Node 24.21.0, pnpm 12.5.1, mise ≥ 2026.9.14, lefthook 2.1.14, committed 1.1.11, .NET 10 (MAUI Android workload in CI), Java 21 (temurin).

**Spec:** `docs/fork/viridian-design.md` in the PokeDaisy fork (in viridian: `grafts/pokedaisy/docs/fork/viridian-design.md` from Task 3).

Later plans, each its own document: 2 the cache server (Hono + Effect v4 Worker on R2), 3 graft CLI tools and `apps/viridian-e2e`, 4 the self-hosted Mac runner.

## Global Constraints

- Repo `Enchyria/viridian`, **private**. Verify it with `git ls-remote git@github.com:Enchyria/viridian.git` (SSH), not the `gh` API (the `gabzidevs` token isn't an Enchyria member).
- Never push to `sofianeelhor/PKForge` or `lidor30/pokedaisy`; their remotes' push URLs are `DISABLED`.
- Tool pins exactly as chaplet's: node `24.21.0`, pnpm `12.5.1`, lefthook `2.1.14`, committed `1.1.11`, mise `min_version = "2026.9.14"`; npm packages nx/@nx/* `23.3.0`, typescript `5.9.3`, vitest `5.0.3`, oxlint `1.85.0`, oxfmt `0.70.0`. Add dotnet `10` and java `temurin-21` to mise.
- TypeScript: `strict`, `exactOptionalPropertyTypes`, `noUncheckedIndexedAccess`, `verbatimModuleSyntax` in `tsconfig.base.json`; no `any` across a module boundary.
- Commits: Conventional Commits (chaplet's `committed.toml` style, lower-case subjects). A commit touching `grafts/pokedaisy/` carries no Claude/Anthropic attribution; other commits may carry `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- PKForge identity: package `io.github.gabzidevs.pkforge`, label `PKForge Fork`, release certificate SHA-256 `27:0D:11:4E:C7:64:2A:0D:2D:86:3C:15:4D:1A:99:42:2E:F6:BB:38:82:04:C9:19:36:11:52:06:18:AA:BF:1A`. Version codes only grow; floor `318` for `3.1.1-fork.7`.
- PokeDaisy identity: package `io.github.enchyrn.pokedaisy`, label `PokeDaisy Fork`, release certificate SHA-256 `5D:83:66:6F:B1:B2:54:40:56:8E:55:B4:C9:EB:FF:2B:85:B1:5E:9A:B3:EB:D9:64:C0:28:D4:F0:38:54:A5:94`. Version code floor `16` (`1.3.2-fork.1`, released). Its upstream rules hold inside `grafts/pokedaisy/`: no ROMs, saves or secrets committed; home paths written as `~`; the manual rules in its `CLAUDE.md`.
- PokeDaisy secrets keep their names: `POKEDAISY_FORK_KEYSTORE_BASE64`, `POKEDAISY_FORK_KEYSTORE_PASSWORD`, `POKEDAISY_FORK_KEY_ALIAS`, `POKEDAISY_FORK_KEY_PASSWORD`, variable `POKEDAISY_FORK_RELEASE_CERT_SHA256`.
- PKForge secrets keep their names: `PKFORGE_ANDROID_KEYSTORE_BASE64`, `PKFORGE_ANDROID_KEYSTORE_PASSWORD`, `PKFORGE_ANDROID_KEY_ALIAS`, `PKFORGE_ANDROID_KEY_PASSWORD`, variable `PKFORGE_RELEASE_CERT_SHA256`; diagnostic `PKFORGE_DIAGNOSTIC_KEYSTORE_BASE64`, variable `PKFORGE_DIAGNOSTIC_CERT_SHA256`. The owner sets them; the agent checks names only.
- Never cached: signed release APKs, anything holding a secret, anything from ROMs or the owner's saves. `.local-testdata/` stays gitignored.
- Release assets: `PKForge-Fork-v<version>.apk`, tag `pkforge-v<version>`, title `PKForge Fork <version>`; `PokeDaisy-Fork-v<version>.apk`, tag `pokedaisy-v<version>`, title `PokeDaisy Fork <version>`. Full releases (not pre-release). Each Obtainium entry filters by title (`^PKForge`, `^PokeDaisy`), so "latest" can go to whichever released last.

## Review Focus

1. **An upstream merge that lowers the version code** (a conflict resolved toward upstream's `ApplicationVersion` or `versionCode`, which lidor30 bumps on every release): the release must refuse rather than ship a code Android won't install over. Pinned in Task 7 for both version actions.
2. **A submodule under `grafts/` with no root `.gitmodules` entry** (upstream adds one; PokeDaisy has three, mGBA, rcheevos and melonDS): CI checks out without it and fails far from the cause. Pinned in Task 2, extended in Task 3.
3. **A Release run without its secrets, or off `main`**: it must stop before tagging or publishing. Pinned in Task 7.
4. **A signed APK restored from the cache** instead of built and verified: `release-apk` is never cacheable, in either graft. Pinned in Task 4.
5. **A Claude co-author line on a commit touching `grafts/pokedaisy/`** (even in a commit that also touches pkforge): rejected. Pinned in Task 5.

---

### Task 1: The workspace

**Files:**
- Create: `mise.toml`, `package.json`, `pnpm-workspace.yaml`, `nx.json`, `tsconfig.base.json`, `tsconfig.json`, `.oxlintrc.json`, `.oxfmtrc.json`, `.gitignore`, `.gitattributes`, `README.md`, `CLAUDE.md`
- Create: `tools/checks/project.json`, `tools/checks/toolchain.test.mjs`

**Interfaces:**
- Produces: Nx project `workspace` with target `checks` (`vitest run tools/checks`); root scripts `verify` = `nx run-many -t typecheck lint test`, `test` = `nx run workspace:checks`.

- [ ] **Step 1:** Create `Enchyria/viridian` as a private, empty repo (`gh repo create Enchyria/viridian --private` with the `enchyrn` account active; confirm with `git ls-remote` over SSH). Clone it to `~/Dev/GabziOSS/viridian`.
- [ ] **Step 2:** Copy chaplet's root tooling files at `1a801a1f29a6` (`mise.toml`, `pnpm-workspace.yaml`, `.oxlintrc.json`, `.oxfmtrc.json`, `.gitattributes`, `tsconfig.base.json`, `tools/checks/toolchain.test.mjs`), dropping everything chaplet-specific (Panda, Solid, TanStack, Playwright, wrangler, effect, the `legacy/` freeze, `minimumReleaseAgeExclude` entries for workerd/wrangler/effect). Add `dotnet = "10"` and `java = "temurin-21"` to `mise.toml`. `package.json`: name `viridian`, private, `packageManager: pnpm@12.5.1`, `engines.node: 24.21.0`, devDependencies limited to `nx`, `@nx/devkit`, `@nx/js`, `@nx/oxlint`, `@nx/vitest`, `typescript`, `vitest`, `oxlint`, `oxfmt`, `@types/node` at the pinned versions. `nx.json`: chaplet's `targetDefaults`, `namedInputs`, `@nx/oxlint` and `@nx/vitest` plugins, `analytics: false`.
- [ ] **Step 3: Write the failing check** in `tools/checks/toolchain.test.mjs` (adapted from chaplet's): `mise.toml`'s `min_version` equals every workflow's `jdx/mise-action` `version:`; `package.json` `engines.node` equals mise's `node`; `packageManager` equals mise's `pnpm`; `tsconfig.base.json` has `strict`, `exactOptionalPropertyTypes`, `noUncheckedIndexedAccess`, `verbatimModuleSyntax` all `true`.
- [ ] **Step 4:** Run `mise install && pnpm install && pnpm exec nx run workspace:checks`. Expected: FAIL until `tsconfig.base.json` has all four flags; set them; then PASS.
- [ ] **Step 5:** `README.md`: what viridian is, the layout (`grafts/`, `apps/`, `libs/`, `tools/`, `docs/`), and that the design, plans, roadmap and research live in `grafts/pokedaisy/docs/fork/` (they arrive with the graft in Task 3). `CLAUDE.md`: the Global Constraints above, plus "verify the repo with git ls-remote, not gh".
- [ ] **Step 6: Commit** `chore(workspace): start viridian from chaplet's tooling` and push `main`.

### Task 2: The PKForge graft

**Files:**
- Create: `grafts/pkforge/**` (subtree), `.gitmodules`, `tools/checks/grafts.test.mjs`, `docs/grafts.md`
- Modify: `grafts/pkforge/src/PKForge.App/PKForge.App.csproj` (version back to the last release)

**Interfaces:**
- Produces: remotes `pkforge-fork` (`git@github-enchyrn:enchyrn/PKForge.git`) and `pkforge-upstream` (`https://github.com/sofianeelhor/PKForge.git`, push `DISABLED`); `docs/grafts.md` with the one-line upstream sync command per graft.

- [ ] **Step 1:** Add the two remotes from Interfaces (`git remote set-url --push pkforge-upstream DISABLED`), fetch `pkforge-fork`, then `git subtree add --prefix grafts/pkforge pkforge-fork main` (full history, no squash). Expected: `grafts/pkforge/PKForge.sln` exists; `git log --oneline -- grafts/pkforge | wc -l` ≥ 217.
- [ ] **Step 2: Write the failing check** `tools/checks/grafts.test.mjs`: for every gitlink in `git ls-files -s grafts` (mode `160000`), the root `.gitmodules` has a section whose `path` equals it and whose `url` equals the graft's own `.gitmodules` entry for the unprefixed path.
- [ ] **Step 3:** Run `pnpm exec nx run workspace:checks`. Expected: FAIL naming `grafts/pkforge/external/PKHeX` and `grafts/pkforge/external/PKHeX-Plugins`.
- [ ] **Step 4:** Add both to the root `.gitmodules` (paths prefixed, URLs `https://github.com/sofianeelhor/PKForge-PKHeX.git` and `https://github.com/santacrab2/PKHeX-Plugins.git`); `git submodule update --init --recursive`. Re-run: PASS.
- [ ] **Step 5:** Set `ApplicationDisplayVersion` to `3.1.1-fork.6` and `ApplicationVersion` to `317` in the csproj (the last released state, so viridian's own release makes fork.7). Copy the owner's local `.local-testdata/unbound-fork-tyran.srm` into `grafts/pkforge/.local-testdata/` (gitignored; never committed).
- [ ] **Step 6:** Run the graft's suites from their new place: `mise x -- dotnet test grafts/pkforge/tests/PKForge.Engine.Tests -c Release` (expected 1020 passed) and `.../PKForge.Domain.Tests` (expected 890 passed).
- [ ] **Step 7:** Write `docs/grafts.md`: sync with `git subtree pull --prefix grafts/pkforge pkforge-upstream main`, then resolve version-line conflicts toward the larger version code; a new or moved upstream submodule means a root `.gitmodules` edit (the grafts check says which).
- [ ] **Step 8: Commit** `chore(pkforge): graft PKForge with its history` and push.

### Task 3: The PokeDaisy graft

**Files:**
- Create: `grafts/pokedaisy/**` (subtree)
- Modify: `.gitmodules`, `docs/grafts.md`, `README.md`

**Interfaces:**
- Consumes: `tools/checks/grafts.test.mjs` (Task 2).
- Produces: remotes `pokedaisy-fork` (`git@github-enchyrn:enchyrn/pokedaisy.git`) and `pokedaisy-upstream` (`https://github.com/lidor30/pokedaisy.git`, push `DISABLED`, `tagOpt --no-tags`).

- [ ] **Step 1:** Add both remotes, fetch `pokedaisy-fork`, `git subtree add --prefix grafts/pokedaisy pokedaisy-fork main`. Expected: `grafts/pokedaisy/settings.gradle.kts` exists; at least 153 commits under the prefix.
- [ ] **Step 2:** Run `pnpm exec nx run workspace:checks`. Expected: FAIL naming `grafts/pokedaisy/third_party/mgba`, `…/rcheevos`, `…/melonds`.
- [ ] **Step 3:** Add the three to the root `.gitmodules` (URLs `https://github.com/mgba-emu/mgba.git`, `https://github.com/RetroAchievements/rcheevos.git`, `https://github.com/melonDS-emu/melonDS.git`); `git submodule update --init --recursive`. Re-run: PASS.
- [ ] **Step 4:** Check what runs here without the Android SDK: `mise x -- ./gradlew -p grafts/pokedaisy/ui-preview stress docsSearchCheck keyedStateCheck` (Gradle 8.10.2 cached; Java 21) passes, and `python3 grafts/pokedaisy/scripts/check_translations.py` reports 0 problems.
- [ ] **Step 5:** `docs/grafts.md`: PokeDaisy's sync line (`git subtree pull --prefix grafts/pokedaisy pokedaisy-upstream main`), and that `versionName`/`versionCode` conflicts resolve toward the larger code. The roadmap, research notes and this design now live in `grafts/pokedaisy/docs/fork/`; `README.md` points there.
- [ ] **Step 6: Commit** `chore(pokedaisy): graft PokeDaisy with its history` (no attribution) and push.

### Task 4: Inferred targets for the grafts

**Files:**
- Create: `tools/workspace-plugin/{package.json,project.json,tsconfig.json,tsconfig.lib.json,tsconfig.spec.json,vitest.config.mts}`, `tools/workspace-plugin/src/index.ts`, `tools/workspace-plugin/src/dotnet-graft.ts`, `tools/workspace-plugin/src/dotnet-graft.spec.ts`, `tools/workspace-plugin/src/gradle-graft.ts`, `tools/workspace-plugin/src/gradle-graft.spec.ts`
- Modify: `nx.json` (register `./tools/workspace-plugin`)

**Interfaces:**
- Produces: `export const createNodesV2: CreateNodesV2` in `src/index.ts`, matching `grafts/*/PKForge.sln` and `grafts/*/settings.gradle.kts`; `export function gradleGraftTargets(root: string): Record<string, TargetConfiguration>` in `src/gradle-graft.ts`; `export function dotnetGraftTargets(root: string): Record<string, TargetConfiguration>` in `src/dotnet-graft.ts`. Project `pkforge` (root `grafts/pkforge`) with targets:
  - `test`: `dotnet test tests/PKForge.Domain.Tests -c Release` then `tests/PKForge.Engine.Tests`, cwd the graft, `cache: true`, inputs `{projectRoot}/src/**`, `{projectRoot}/tests/**`, `{projectRoot}/external/**`, `{projectRoot}/*.sln`.
  - `build-app`: `dotnet build src/PKForge.App/PKForge.App.csproj -c Release -f net10.0-android -p:DiagnosticBuild=true`, `cache: true`, output `{projectRoot}/src/PKForge.App/bin/Release/net10.0-android`.
  - `release-apk`: the signed Release build, `cache: false`.

  Project `pokedaisy` (root `grafts/pokedaisy`), all run with `./gradlew` from the graft:
  - `translations`: `python3 scripts/check_translations.py`, `cache: true`.
  - `ui-preview`: `./gradlew -p ui-preview stress docsSearchCheck keyedStateCheck`, `cache: true` (light: no Android SDK).
  - `docs`: the docs job of its `ci.yml` (`make docs-check`), `cache: true`.
  - `website`: `npm ci && npm test && npm run build` in `website/`, `cache: true`, output `{projectRoot}/website/dist`. No deploy.
  - `test`: `./gradlew :app:testDebugUnitTest`, `cache: true` (medium: Android SDK).
  - `build-app`: `./gradlew :app:assembleDebug`, `cache: true`, output `{projectRoot}/app/build/outputs/apk/debug` (heavy: NDK).
  - `release-apk`: `./gradlew :app:assembleRelease`, `cache: false`.

- [ ] **Step 1: Write the failing tests.** `dotnet-graft.spec.ts`: `dotnetGraftTargets('grafts/pkforge')` has keys `test`, `build-app`, `release-apk`; `test.cache === true`; `release-apk.cache === false`; `test.inputs` contains `'{projectRoot}/tests/**'`. `gradle-graft.spec.ts`: `gradleGraftTargets('grafts/pokedaisy')` has keys `translations`, `ui-preview`, `docs`, `website`, `test`, `build-app`, `release-apk`; `release-apk.cache === false`; `website.command` contains no `deploy`.
- [ ] **Step 2:** Run `pnpm exec nx test workspace-plugin`. Expected: FAIL (module not found).
- [ ] **Step 3:** Implement `dotnetGraftTargets` and `createNodesV2` (Nx `createNodesFromFiles`, project name from the folder).
- [ ] **Step 4:** Run the specs (PASS), then `nx show project pkforge --json` and `nx show project pokedaisy --json` list their targets; `nx run pkforge:test` and `nx run pokedaisy:ui-preview` each pass twice, the second as a local cache hit.
- [ ] **Step 5: Commit** `feat(workspace-plugin): infer both grafts' targets`.

### Task 5: Hooks and the commit-message rules

**Files:**
- Create: `lefthook.yml`, `committed.toml`, `tools/checks/attribution.mjs`, `tools/checks/attribution.test.mjs`

**Interfaces:**
- Produces: `export function attributionProblem(message: string, paths: readonly string[]): string | null` in `tools/checks/attribution.mjs`; CLI `node tools/checks/attribution.mjs <message-file>` reading staged paths, exit 1 with the problem.

- [ ] **Step 1: Write the failing test** `attribution.test.mjs`: returns a problem for `Co-Authored-By: Claude …` with paths `['grafts/pokedaisy/a.kt']` and with `['grafts/pkforge/a.cs','grafts/pokedaisy/b.kt']`; also for `Generated with [Claude Code]` there; `null` for the same message with only `['grafts/pkforge/a.cs']`; `null` for a clean message touching pokedaisy.
- [ ] **Step 2:** Run `pnpm exec nx run workspace:checks`. Expected: FAIL (module missing).
- [ ] **Step 3:** Implement `attributionProblem` (case-insensitive match on `co-authored-by:.*(claude|anthropic)` and `generated with \[?claude`).
- [ ] **Step 4:** `committed.toml` from chaplet's with `allowed_scopes = ["pkforge", "pokedaisy", "workspace", "workspace-plugin", "docs", "ci", "deps", "release"]`; `lefthook.yml`: pre-commit oxfmt and oxlint on staged files (chaplet's jobs, without the legacy freeze), commit-msg `committed --commit-file {1}` and `node tools/checks/attribution.mjs {1}`. Add a check in `toolchain.test.mjs`: every Nx project name is in `allowed_scopes`.
- [ ] **Step 5:** Run the checks (PASS); `lefthook install`; a test commit with a bad scope is refused.
- [ ] **Step 6: Commit** `ci(workspace): commit hooks and the per-path attribution rule`.

### Task 6: CI

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `grafts/pkforge/src/PKForge.Domain/AppUpdate.cs`, `grafts/pkforge/src/PKForge.App/Services/AppUpdateService.cs`, `grafts/pkforge/tests/PKForge.Domain.Tests/AppUpdateRulesTests.cs`, `grafts/pkforge/docs/fork/README.md`
- Modify: `grafts/pokedaisy/app/src/main/kotlin/com/pokedaisy/app/AppUpdater.kt`, `grafts/pokedaisy/app/src/test/kotlin/com/pokedaisy/app/AppUpdaterForkTest.kt` (create), `grafts/pokedaisy/docs/fork/README.md`
- Delete (after the first green run): `grafts/pokedaisy/.github/` and `grafts/pkforge/.github/` (GitHub never runs a subfolder's workflows; viridian's replace them)

**Interfaces:**
- Consumes: both grafts' targets (Task 4).
- Produces: `public const bool ChecksForUpdates = false;` on PKForge's `AppUpdateRules`, read by `AppUpdateService` before any request; `const val CHECKS_FOR_UPDATES = false` on PokeDaisy's `AppUpdater`, read before any request (its Settings > VERSION shows the installed version and says updates come through Obtainium).

- [ ] **Step 1: Write the failing test** in `AppUpdateRulesTests.cs`: `Assert.False(AppUpdateRules.ChecksForUpdates)` (the in-app check is off: releases are private, Obtainium is the update path).
- [ ] **Step 2:** Run it: FAIL. Add the constant; make `AppUpdateService` return "no update" without a request when it's false; note it in `grafts/pkforge/docs/fork/README.md`. Run: PASS.
- [ ] **Step 2b:** Same for PokeDaisy, test first: `AppUpdaterForkTest.checksForUpdatesIsOff` asserts `!AppUpdater.CHECKS_FOR_UPDATES`; it fails, then the constant and the guard make it pass (CI runs it, as `pokedaisy:test` needs the Android SDK). Any visible wording change follows its manual rules (update `docs/manual/` in the same commit, no dashes).
- [ ] **Step 3:** `ci.yml`: on push to `main` and pull requests; checkout with `submodules: recursive` and `fetch-depth: 0`; `jdx/mise-action` at `version: 2026.9.14`; `pnpm install`; `committed` over the pull request's commits and the attribution check; `nx affected -t lint test` (light); a job `pkforge-app`, only when `nx show projects --affected` includes `pkforge`, installs `maui-android` and runs `nx run pkforge:build-app` with the diagnostic key when set (PKForge's `build.yml` signing and aapt2 label check); a job `pokedaisy-app`, only when `pokedaisy` is affected, sets up Android (`android-actions/setup-android`, `sdkmanager --install "ndk;28.2.13676358" "cmake;3.22.1" "platforms;android-34"`, Java 17 as its CI uses) and runs `nx run-many -p pokedaisy -t test build-app`. All action references pinned to commit SHAs, as PokeDaisy's workflows are.
- [ ] **Step 4:** Push; the run on `Enchyria/viridian` is green (read it with `gh run list -R Enchyria/viridian` as `enchyrn`; if `gh` resolves the wrong account, use the Actions page).
- [ ] **Step 5:** Delete both grafts' `.github/` folders once the run is green.
- [ ] **Step 6: Commit** `fix(pkforge): switch the in-app update check off`, `fix(pokedaisy): switch the in-app update check off` (no attribution) and `ci(workspace): affected tests and both app builds`.

### Task 7: Release, the chaplet way

**Files:**
- Create: `tools/workspace-plugin/src/version-actions/csproj.ts`, `tools/workspace-plugin/src/version-actions/csproj.spec.ts`, `tools/workspace-plugin/src/version-actions/gradle.ts`, `tools/workspace-plugin/src/version-actions/gradle.spec.ts`, `tools/changelog-renderer.cjs` (chaplet's), `tools/release/verify-apk.sh`, `.github/workflows/release.yml`, `.nx/version-plans/pkforge-keepsake.md`, `.nx/version-plans/pokedaisy-updates-off.md`
- Modify: `nx.json` (`release` block)

**Interfaces:**
- Produces: version actions class `CsprojVersionActions` (Nx `VersionActions`) reading/writing `ApplicationDisplayVersion` and `ApplicationVersion` in `src/PKForge.App/PKForge.App.csproj`; `GradleVersionActions` reading/writing `versionName` and `versionCode` in `app/build.gradle.kts`, same rules; `tools/release/verify-apk.sh <apk>` using env `EXPECTED_PACKAGE`, `EXPECTED_LABEL`, `EXPECTED_CERT_SHA256`, shared by both groups.

- [ ] **Step 1: Write the failing test** `csproj.spec.ts`: from `3.1.1-fork.6`/`317`, a `prerelease` bump with preid `fork` writes `3.1.1-fork.7`/`318`; the version code always becomes current + 1; given a csproj whose code would go down (a requested code below the current one), it throws `version code must only grow`. `gradle.spec.ts`: from `1.3.2-fork.1`/`16` a `prerelease` bump with preid `fork` writes `1.3.2-fork.2`/`17`, and the same refusal.
- [ ] **Step 2:** Run: FAIL. Implement; run: PASS.
- [ ] **Step 3:** `nx.json` `release`: group `pkforge` (projects `["pkforge"]`, `releaseTag.pattern` `pkforge-v{version}`, version actions the csproj class, `preid: "fork"`, changelog `{workspaceRoot}/docs/changelogs/pkforge.md` with chaplet's renderer, `createRelease: "github"`); git commit message `chore(release): publish`; `versionPlans` on. Group `pokedaisy` the same way: tag `pokedaisy-v{version}`, the Gradle class, `preid: "fork"`, changelog `{workspaceRoot}/docs/changelogs/pokedaisy.md`. Version plans: `pkforge-keepsake.md` (`pkforge: prerelease`, the park keepsake) and `pokedaisy-updates-off.md` (`pokedaisy: prerelease`, updates now come through Obtainium). Each changelog starts with a line pointing to the old fork repo's releases (`enchyrn/PKForge` fork.1 to fork.6, `enchyrn/pokedaisy` fork.1).
- [ ] **Step 4:** `tools/release/verify-apk.sh`: port `build.yml`'s checks: empty or malformed `EXPECTED_CERT_SHA256` fails with its message; aapt2 package and label must equal the env values; apksigner's SHA-256 must equal the fingerprint.
- [ ] **Step 5:** `release.yml` in chaplet's shape: `workflow_dispatch` with `dry-run` default `true`; job `if: github.ref == 'refs/heads/main'`; fails before any versioning if a released group's secrets or certificate variable are missing (unless dry run); runs `pnpm verify`; `nx release --skip-publish` (or `--dry-run`); for each group released, builds its `release-apk` with its key (PokeDaisy's through a `keystore.properties` written to the runner's temp folder, as its `fork-release.yml` does), runs `verify-apk.sh` with that app's package, label and fingerprint, and uploads its APK to its release, full, not pre-release.
- [ ] **Step 6:** Run the Release workflow with `dry-run: true`. Expected: `pkforge 3.1.1-fork.6 → 3.1.1-fork.7` (code `317 → 318`) and `pokedaisy 1.3.2-fork.1 → 1.3.2-fork.2` (code `16 → 17`), both changelog entries, then it stops before writing.
- [ ] **Step 7: Commit** `feat(release): release both grafts from viridian`.

### Task 8: The first releases (owner steps marked)

- [ ] **Step 1 (owner):** In their own terminal: `RESUME=1 REPO=Enchyria/viridian grafts/pkforge/scripts/fork/create-release-key.sh`, `REPO=Enchyria/viridian grafts/pkforge/scripts/fork/create-diagnostic-key.sh`, and `RESUME=1 REPO=Enchyria/viridian grafts/pokedaisy/scripts/fork/create-release-key.sh`. The agent then checks names only: `gh secret list -R Enchyria/viridian` shows the four `PKFORGE_ANDROID_*`, the diagnostic secret and the four `POKEDAISY_FORK_*`; `gh variable list` shows the three certificate variables.
- [ ] **Step 2:** Run Release with `dry-run: false`. Expected: tags `pkforge-v3.1.1-fork.7` and `pokedaisy-v1.3.2-fork.2`; releases `PKForge Fork 3.1.1-fork.7` (asset `PKForge-Fork-v3.1.1-fork.7.apk`, verified `27:0D:11:4E:…:18:AA:BF:1A`) and `PokeDaisy Fork 1.3.2-fork.2` (asset `PokeDaisy-Fork-v1.3.2-fork.2.apk`, verified `5D:83:66:6F:…:A5:94`), neither a pre-release.
- [ ] **Step 3 (owner):** In Obtainium, add `https://github.com/Enchyria/viridian` twice with a fine-grained read-only token (contents: read, this repo only), filters `^PKForge` and `^PokeDaisy`. Update PKForge Fork over the installed fork.6: the Poképark is intact and "Save keepsake now" writes `pkforge-keepsake-1.json` into the saves folder. Install PokeDaisy Fork (first install; upstream PokeDaisy stays until its saves and states are moved, `grafts/pokedaisy/docs/fork/RESEARCH.md` "Moving to the fork apps").
- [ ] **Step 4 (owner decides):** Archive `enchyrn/PKForge` and `enchyrn/pokedaisy` (Settings > Archive). Update `docs/grafts.md` and `grafts/pokedaisy/docs/fork/ROADMAP.md` "Where things stand": both forks live in viridian.
