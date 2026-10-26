# PokeDaisy Fork

Personal fork of [PokeDaisy](https://github.com/lidor30/pokedaisy) at https://github.com/enchyrn/pokedaisy (upstream: `lidor30/pokedaisy`). It's meant to become the hub for an "organic, not cheating" Emerald companion: features added as modules behind a small host interface. The modules aren't built yet; the brainstorm and its decisions are in [ROADMAP.md](ROADMAP.md).

Its sibling is the [PKForge fork](https://github.com/enchyrn/PKForge), which serves as a design reference (Poképark, daycare, Bank) and will later read this fork's camp data.

## Changes so far

- **GAME BUTTONS per controller** (2026-10-26): each controller and keyboard keeps its own map, made on its first rebind from the shared one, and GAME BUTTONS has RESET TO DEFAULT. `GbaControls.Pad`, tests in `GbaControlsPadTest`.

## Fork identity

- `app/build.gradle.kts`: app ID `io.github.enchyrn.pokedaisy`, version `1.3.2-fork.1`, version code `16`. With its own ID, the fork installs beside upstream's PokeDaisy, never over it: upstream's install, its saves and its settings stay as they are. The fork's data lives under `Android/data/io.github.enchyrn.pokedaisy/`; point it at the same ROMs and saves folders to share them.
- `app/src/main/res/values/strings.xml`: the app is named "PokeDaisy Fork".
- `AppUpdater.REPO` is `enchyrn/pokedaisy`, so the in-app updater offers the fork's releases. It skips GitHub pre-releases, so fork releases are published as full releases.
- Crash reports stay off: the fork has no Firebase config. The ScreenScraper and save-editor feature flags stay off as upstream ships them.

## Releases and signing

Upstream's `release.yml` is disabled on the fork (Actions > Release > Disable workflow): it would sign with upstream's key and deploy upstream's website. The fork uses `.github/workflows/fork-release.yml`, which runs on tags like `v1.3.2-fork.1`:

- The tag must be on main and match `versionName`.
- The whole CI suite (`ci.yml`) must pass on the tagged commit.
- `aapt2` must report package `io.github.enchyrn.pokedaisy` and label `PokeDaisy Fork`.
- The APK's certificate must match the variable `POKEDAISY_FORK_RELEASE_CERT_SHA256` (keytool's `AA:BB:...` form). An empty variable fails the release.
- It publishes `PokeDaisy-Fork-<tag>.apk` as the latest release (one APK per release, so Obtainium needs no filters).

Signing setup, in the repo's Actions settings:

- Secrets: `POKEDAISY_FORK_KEYSTORE_BASE64` (the PKCS12 keystore, base64), `POKEDAISY_FORK_KEYSTORE_PASSWORD`, `POKEDAISY_FORK_KEY_ALIAS`, `POKEDAISY_FORK_KEY_PASSWORD`.
- Variable: `POKEDAISY_FORK_RELEASE_CERT_SHA256`.

`scripts/fork/create-release-key.sh` (the PKForge fork's script, renamed) does all of it once, run by the owner in their own terminal: it makes the key, backs it up to Bitwarden as "PokeDaisy Fork release key" (the `.p12` as base64 in the note, attached too with Premium), then sets the secrets and the variable with `gh`. It never prints a secret and refuses to make a second key. `DRY_RUN=1` makes a throwaway key and stops; `RESUME=1` restores the GitHub secrets from the Bitwarden item.

Keep that backup: Android only installs an update signed with the installed app's key.

To cut a release: bump `versionName` (`1.3.2-fork.N`) and `versionCode` in `app/build.gradle.kts`, commit, push main, then tag `v<versionName>` and push the tag. After an upstream merge that bumps the version, take upstream's version with `-fork.1` and a version code above both.

## Syncing with upstream

```sh
git config remote.upstream.tagOpt --no-tags   # once: keep upstream's tags out of this repo
git fetch upstream
git merge upstream/main
```

The upstream remote is fetch-only (its push URL is set to `DISABLED`). Upstream's `CLAUDE.md` is left as it is, so merges stay clean; fork-only notes for agents are in `.claude/CLAUDE.md`.
