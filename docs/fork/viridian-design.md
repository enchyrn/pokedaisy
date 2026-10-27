# Viridian: design

A private Nx monorepo under the Enchyria GitHub org that brings the PKForge and PokeDaisy forks together, with chaplet's tooling surface, a shared build cache, and one test-and-release flow. Agreed in conversation with the owner on 2026-10-27 and 2026-10-28; anything marked *builder's call* or *my call* was not decided by the owner.

## Intent

The two forks are one project now: PokeDaisy is the hub for an "organic, not cheating" Emerald companion (roadmap: `docs/fork/ROADMAP.md`), PKForge is its Poképark, daycare and Bank. As two repos they mean two of everything: two CI setups, two release flows, cross-app checks done by hand (the 2026-10-27 save round-trips), and no way to change both in one commit. This Mac can't build either app, so every app change waits on CI with nothing reused.

The longer arc (owner, 2026-10-28): the forks are a transition. The main architectural goal is to port them to our own stack, most likely TypeScript (or Rust), with Effect throughout. So the grafts' behaviour is pinned down by TypeScript tests that talk to them through small command-line tools; a port later has to pass the same tests.

Success: one private repo where a change spanning both apps plus its integration test is one commit; `nx affected` runs only what a change touches, locally where cheap and from cache where heavy; one Release button ships either app the way chaplet ships its products.

## Goals

1. Both forks live in one repo, each still taking upstream's changes with one command.
2. Every existing test, build and check of both forks runs as an Nx target, unchanged in what it proves.
3. Fork customizations get tests of their own, separate from upstream's, and cross-app integration tests become possible.
4. A shared, self-hosted Nx remote cache: CI writes, this Mac reads, so heavy outputs (APKs, native builds) are restored here instead of rebuilt.
5. Releases through Nx release (version plans, release groups, changelogs, one Release workflow), verifying what ships.
6. Strict TypeScript for everything written in TypeScript, starting with the cache server.

## Non-goals

- Changing what either app does, beyond what moving requires (the in-app update check, below).
- Rewriting the forks' build systems: Gradle and MSBuild stay; Nx calls them.
- Shared libraries before two things need the same code.
- Making the repo or its releases public. Distributed task execution (Nx Agents). Nx Cloud.

## Anti-goals (would make it a failure even if it works)

From the realm's own record (`enchyria/realm`, `the-enchyr/06-the-attempts.md`): build the thing before the apparatus; elaborate before proven is waste; add nothing without deleting something.

- Apparatus that outweighs the work: every tool added must replace something the forks do today (their own CI files, release scripts, manual checks), and the old thing is deleted in the same change.
- An upstream merge that becomes a project. If pulling upstream needs more than the merge itself and a conflict fix, the layout is wrong.
- A second place for the same fact: one roadmap (hub `docs/fork/ROADMAP.md`, moved into viridian), one changelog per release group.
- Leaking ROMs, saves, keys or the owner's data into git, CI logs or the cache.

## Constraints

- **Private repo**, under the Enchyria org (`Enchyria/viridian`). Note from chaplet's `CONTEXT.md`: `gh` may resolve to the `gabzidevs` token, which isn't an Enchyria member; verify with `git ls-remote` over SSH.
- **Upstreams change almost daily**: sofianeelhor/PKForge and lidor30/pokedaisy. Never push to them.
- **Nested submodules**: PKForge has `external/PKHeX` and `external/PKHeX-Plugins`; PokeDaisy has `third_party/mgba`, `third_party/rcheevos`, `third_party/melonds`.
- **Per-graft rules carry over**: PokeDaisy commits carry no Claude attribution and its public-repo rules (no ROMs, saves or secrets; home paths as `~`) still hold; PKForge commits carry the co-author line. A commit touching both follows the stricter rule (no attribution). *My call.*
- **App identities and keys stay**: installed apps must keep updating in place.
  - PKForge Fork `io.github.gabzidevs.pkforge`, key in Bitwarden ("PKForge Fork release key"), certificate SHA-256 `27:0D:11:4E:C7:64:2A:0D:2D:86:3C:15:4D:1A:99:42:2E:F6:BB:38:82:04:C9:19:36:11:52:06:18:AA:BF:1A`.
  - PokeDaisy Fork `io.github.enchyrn.pokedaisy`, key in Bitwarden ("PokeDaisy Fork release key"), certificate SHA-256 `5D:83:66:6F:B1:B2:54:40:56:8E:55:B4:C9:EB:FF:2B:85:B1:5E:9A:B3:EB:D9:64:C0:28:D4:F0:38:54:A5:94`. (`~/.android/pokedaisey-release.jks` in upstream's notes is upstream's key, not the fork's.)
  - The owner puts the keys into viridian's secrets themselves, by running each fork's `scripts/fork/create-release-key.sh` with `RESUME=1` and `REPO=Enchyria/viridian`; the scripts need their secret names changed to one prefix per release group first. The agent never handles key material and checks only secret names. Until they're in place, the Release workflow's dry run (no signing) is the rehearsal path.
  - No Firebase: the fork has no `google-services.json`, so crash reports stay off as they are today.
- **This Mac** (M4, 48 GB): .NET 10, Java 21, Node through mise work; no Android SDK or NDK; the MAUI app doesn't build here.
- **Releases are private**: the apps' in-app updaters can't read them (owner accepted: Obtainium with a read-only token is the update path for now).

## Decided

**Name and layout.** `Enchyria/viridian`: Viridian Forest, where most players first meet the wild, and the green of the realm's Verdant. It may later move into `enchyria/realm` as a branch.

```
grafts/
  pkforge/        the PKForge fork (git subtree of enchyrn/PKForge, upstream sofianeelhor/PKForge)
  pokedaisy/      the PokeDaisy fork (git subtree of enchyrn/pokedaisy, upstream lidor30/pokedaisy)
apps/
  cache/          the Nx remote cache server (name: builder's call)
  viridian-e2e/   cross-graft integration tests (name: builder's call)
libs/             empty until two projects share code
tools/            workspace plugin, checks (chaplet's shape)
docs/             roadmap, research, this design (moved from the hub)
```

`grafts/` holds forks of other people's work; `apps/` holds what's ours from the start (later: the modern Emerald hack, maybe our own companion).

**Grafts are git subtrees.** Each fork's history is merged in under its prefix; upstream changes come in with `git subtree pull --prefix grafts/<name> <upstream> main`. Their submodules are declared in viridian's root `.gitmodules` with prefixed paths; upstream's submodule version bumps arrive by themselves, and only an added or moved submodule needs a hand edit. The enchyrn fork repos are made read-only (archived) only after viridian has cut a verified release of that graft.

**Tooling surface from `Enchyria/chaplet`.** mise owns tools with exact pins and a `min_version` (Node, pnpm, nub, lefthook, committed, plus .NET 10 and Java 21 here); Nx owns tasks; lefthook runs oxfmt, oxlint and the commit-message check (committed) as git hooks and CI runs the same; `tools/checks` tests that the toolchain pins agree (chaplet's `toolchain.test.mjs`); a local `tools/workspace-plugin`.

**Scope of "every existing check"** (*my call*): exactly what each fork's CI runs today. PKForge: both test suites and the app build (`build.yml`). PokeDaisy: unit tests and debug build, ui-preview and stress, docs search and the docs check (`ci.yml`), plus the website's build and tests (`website.yml`, without its Firebase deploy, which belongs to upstream's domain). Not carried: upstream's `release.yml` (already disabled on the fork), the website deploy, the 3DS port (`3ds/`, not in the fork's CI), the ROM-backed tests (they need a self-hosted runner with ROMs that the fork doesn't have; they skip as they do today).

**Tasks.** The workspace plugin infers each graft's targets from its files, so the forks' own build files stay as upstream wrote them:

- pkforge: `test` (Engine and Domain test projects), `build-app` (the MAUI APK), `release-apk` (signed, CI only).
- pokedaisy: `test` (Gradle unit tests), `build-app` (debug APK, native mGBA/melonDS), `ui-preview` (render and stress), `docs` (manual check), `release-apk` (signed, CI only).

**CI minutes** (owner: both of these). Viridian is private, so Actions minutes count against the org's allowance. (1) Heavy jobs (native builds, both APKs) run only for a release or when their graft is affected; light ones on every push. (2) A self-hosted runner on this Mac (M4, 48 GB) takes the heavy jobs, at no minutes; GitHub-hosted runners take them only when the Mac runner is offline for a release (*my call*). The runner serves this private repo only.

Weight tiers decide where things run: light (PKForge tests, ui-preview, docs, lint, integration tests at the save level) always run locally; medium (PokeDaisy unit tests: Android SDK, Gradle) run locally once the SDK is installed, else come from cache; heavy (native builds, both APKs) build in CI and are restored here from the cache; signing is CI only.

**Tests.** Each graft's own suites run as they are. Fork customizations' tests live in a fork-owned folder beside each graft, separate from upstream's (exact location: builder's call, following today's `docs/fork/` and `scripts/fork/` pattern). Cross-graft integration tests live in `apps/viridian-e2e`, written in strict TypeScript with Effect (owner's choice). They drive each graft through a small command-line tool added to it (a fork customization, like the `ubexp` tool in `grafts/pkforge/scripts/fork/save-tools/`): read a save, write daycare EXP, report a Pokémon's facts, as JSON. These tools and the tests that call them are the contract a future port must keep. First candidates are a save round-trip between the two apps' readers and daycare EXP landing in real Emerald and Unbound saves with valid checksums (fixtures must be synthetic or gitignored, never the owner's saves in git). One command, `nx affected -t test e2e`, runs everything a change touches; CI runs the same.

**What may be cached** (*my call*): test results, lint, ui-preview renders and unsigned debug builds. Never cached: signed release APKs, anything holding a secret, anything derived from ROMs or the owner's saves.

**Remote cache: our own server.** Nx's self-hosted cache OpenAPI spec (`PUT`/`GET /v1/cache/{hash}`, bearer tokens, 403 for a read-only token writing, 409 for an existing entry), configured with `NX_SELF_HOSTED_REMOTE_CACHE_SERVER` and `NX_SELF_HOSTED_REMOTE_CACHE_ACCESS_TOKEN`. Built with Hono for HTTP and Effect v4 for the logic (typed errors per spec answer, storage and tokens as services), deployed as a Cloudflare Worker storing in R2, the way chaplet deploys Workers. CI on `main` holds the write token; this Mac and pull requests get read-only tokens. Tested with Vitest in the Workers runtime plus a contract test where the real `nx` CLI stores and restores a task through it.

**TypeScript hardening.** `strict` plus `exactOptionalPropertyTypes`, `noUncheckedIndexedAccess`, `verbatimModuleSyntax`; the Effect language service; oxlint with type-aware rules; no `any` across a module boundary. *My defaults, not yet confirmed by the owner.*

**Releases: chaplet's way.** Nx release groups with their own tags and changelogs: `pkforge` (`pkforge-v3.1.1-fork.7`), `pokedaisy` (`pokedaisy-v1.3.2-fork.2`), `cache` (`cache-v…`, deployed). Changes declare bumps in version plans (`.nx/version-plans/`). Fork versions stay prereleases of upstream's with `fork` as the preid; after an upstream merge, a version plan moves the base and restarts at `fork.1`. Version codes must only ever grow (*my call*): each release sets `versionCode`/`ApplicationVersion` to one above the larger of its current value and upstream's, and the version action refuses to go lower. The current values are the floor (PKForge 318, PokeDaisy 16). Tags fork.1 to fork.6 stay in the archived fork repos; viridian's changelogs start at its first release, with a line pointing to the old releases. Version actions in the workspace plugin edit `ApplicationDisplayVersion`/`ApplicationVersion` (csproj) and `versionName`/`versionCode` (Gradle). One Release workflow (`workflow_dispatch`, dry run by default, `main` only) runs every check, versions, commits `chore(release): publish`, tags, builds and signs, verifies each APK (certificate fingerprint, package, label) or the deployed cache (live version), and publishes GitHub Releases as full releases.

**Commit-message rule** (*my call*): enforced per touched path by the commit-message hook and CI. A commit touching `grafts/pokedaisy/` carries no Claude attribution; other commits may carry the co-author line. One repo-wide check, written once.

**Updates.** Each graft's in-app update check is switched off (a small fork customization each); Obtainium follows the private repo with a read-only fine-grained token, one entry per app filtered by release title (`^PKForge`, `^PokeDaisy`).

**Migration order.** PKForge first (smaller, one CI file), then PokeDaisy. The pending `3.1.1-fork.7` (park keepsake, on enchyrn/PKForge `main` at `acf76df`, not yet tagged) becomes viridian's first PKForge release, cut from viridian's `main` once the graft is imported (*my call*). Each fork repo is archived only after viridian has released and verified that graft.

## Left to the builder

- Exact names for the cache app and the e2e app.
- How the workspace plugin finds targets (file globs, project.json where inference is awkward).
- Where fork-customization tests sit inside or beside each graft.
- The Worker's internals and R2 layout, retention (an expiry so the bucket stays in the free tier), token issuing.
- How CI installs the Android SDK, NDK and .NET workloads (mise where it can, setup actions where it can't).
- Whether nub is worth carrying (chaplet uses it; pnpm stays authoritative either way).
