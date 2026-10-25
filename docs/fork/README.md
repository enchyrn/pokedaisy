# PokeDaisy Fork

Personal fork of [PokeDaisy](https://github.com/lidor30/pokedaisy) at https://github.com/gabzidevs/pokedaisy (upstream: `lidor30/pokedaisy`). It's meant to become the hub for an "organic, not cheating" Emerald companion: features added as modules behind a small host interface. Nothing is built yet; the brainstorm and its decisions are in [ROADMAP.md](ROADMAP.md).

Its sibling is the [PKForge fork](https://github.com/gabzidevs/PKForge), which serves as a design reference (Poképark, daycare, Bank) and will later read this fork's camp data.

## Syncing with upstream

```sh
git config remote.upstream.tagOpt --no-tags   # once: keep upstream's tags out of this repo
git fetch upstream
git merge upstream/main
```

The upstream remote is fetch-only (its push URL is set to `DISABLED`). Upstream's `CLAUDE.md` is left as it is, so merges stay clean; fork-only notes for agents are in `.claude/CLAUDE.md`.
