# Research notes

What was found out while brainstorming and fixing things, so the next session doesn't redo it. Decisions live in [ROADMAP.md](ROADMAP.md); this file holds the reasons and the facts behind them. Gathered 2026-10-26 and 2026-10-27. Anything not checked against code, a save or a ROM says so.

## Cross-game transfers and legality

- **PKHeX only knows the official games' rules.** Legality can't be checked for CFRU hacks (Unbound, Radical Red, GS Chronicles) or Luminescent: PKForge's Unbound session reports `SupportsLegalityAnalysis = false`, and `GetMetInfo` isn't supported there.
- **Unbound's PC keeps a 58-byte compact record** (`RadicalRedFormat.PcMonSize`): PID at 0, trainer ID at 4, species at 0x1C, EXP at 0x20, the IV word at 0x36 (bit 30 is the egg flag). It has no met data, friendship, PP or status. The party record is 100 bytes (EXP at 0x24) and is stored plain, not encrypted.
- **Unbound to an official game comes out illegal.** PKForge's converter (`CfruPk3.ToPk3`, `UnboundEngineSession.ToPk3`) sets the game to FireRed, has no met data to give, and carries the EXP over as it is. A Pokémon that was ever in Unbound's PC has lost its met data and friendship, so a round trip (Enchyrn to Unbound and back to Emerald) comes back illegal. Treat a crossing as one-way.
- **Tyran (Unbound Larvitar) can't be made legal as itself:** no met data, Unbound's own PID and IV generation (not a Gen 3 method), and possibly moves Gen 3 lacks. The only route is a legal twin (ROADMAP work items), which is a different individual.
- **Growth curves differ.** Unbound uses the Dynamic Pokémon Expansion's tables: Larvitar is Medium Slow there, Slow in the official games. The same EXP is a different level in each game. The owner accepts a level loss when crossing ("there should at least be a penalty for crossing universes"); what's open is the rule (keep the level and reset its progress, or keep half the gap) and recovering some EXP organically.
- **The owner plays Unbound without its Stat Rebalancing option**, so base stats match the official games.
- **The DPE repository** (github.com/Skeli789/Dynamic-Pokemon-Expansion, `Unbound` branch, `src/`) matched the ROM's own tables where compared.
- **FireRed isn't the cause.** The transfer and legality problems come from expanded engines (CFRU on FireRed, pokeemerald-expansion on Emerald): more species and moves, their own save layouts. An Emerald story hack built on the expansion has the same problems.
- **Camp is unaffected.** It never moves Pokémon between games; each hack only needs its own route and encounter tables.

## Choosing a hack

The comparison is summarised in ROADMAP ("Choosing hacks"). Its sources, partly unverified (a quick web pass, plus this repo's own support tables):

- Lazarus: [moddb](https://www.moddb.com/games/pokmon-lazarus), v1.1 Nov 2025, a reported v2.0 ([Retro Handhelds](https://retrohandhelds.gg/pokemon-lazarus-gets-its-biggest-update-since-launch/)). PokeDaisy: NEXT BOSS and HERE only, egg icon index 1561 (more than 386 species).
- Heart & Soul: [releases](https://github.com/PokemonHnS-Development/pokemonHnS/releases), 1.2.1 Dec 2025, a 2.0 reported Aug 2026 ([Retro Dodo](https://retrododo.com/popular-gba-pokemon-rom-hack-heart-soul-gets-huge-2-0-update/), one source). PokeDaisy: full guide and area data from its public source.
- Quetzal: [repo page](https://retrogametalk.com/repo/gba-roms/pokemon-quetzal-pokemon-emerald-multiplayer-hack/), still alpha (Alpha 9, Moémon 0.8.4 Feb 2026). PokeDaisy: partial, no wild-encounter switch.
- Elite Redux: [PokeCommunity thread](https://www.pokecommunity.com/threads/499227/), 2.65.x beta, species ids to ~2900. PokeDaisy: fixtures and EXPAND, no NEXT BOSS.
- Seaglass: no web source found; PokeDaisy partial. Crown: not found anywhere.
- PKForge opens all of these read-only; it has full engines for Unbound, Radical Red, GS Chronicles and Luminescent.
- Not researched yet: light-edit Emerald hacks that keep Emerald's save layout and only the original 386 species.

## Save formats

**Gen 3 (Emerald, FireRed):** 128 KB, two slots of 14 sectors (0x1000 each). Each sector's footer at 0xFF4: section id (u16), checksum (u16), signature `0x08012025` (u32), save counter (u32). The checksum is the sum of the section's u32 words (its own size: 0xF2C for section 0, 0xF80 for 1-3, 0xF08 for 4, 0x7D0 for 13), folded to 16 bits. Emerald's party count is at section 1 + 0x234, the party at + 0x238 (100 bytes each). Each Pokémon's 48-byte data is XOR-encrypted with PID ^ trainer ID, its four parts in the order PID % 24, and checksummed as the sum of 16-bit words. The level byte in the party record only changes when the game levels the Pokémon up (after a battle that gives EXP); EXP alone can be raised safely.

**Unbound (CFRU):** sector signature `0x01121999`. The party section (1) is checksummed over 0xFF0 bytes. PKForge's Unbound session copies the party into section 1 of both save slots on every party edit; since PKForge Fork 3.1.1-fork.6 it copies the data only, so each slot keeps its own footer and save counter. The phone's Unbound save carries a 16-byte real-time-clock tail (131088 bytes); upstream's test fixture is the bare 131072, which is why upstream's ground-truth tests can't use the owner's save.

**PokeDaisy:** a resume state holds the save as it was. On launch, if the save file changed since the state was made, PokeDaisy sets the state aside (`resume.replaced`) and boots from the file, so an edited save is never overwritten. Anything played after the last in-game save then exists only in that set-aside state.

## Manual save edits (the careful way)

Used when an app couldn't write something itself. Tools are kept in the PKForge fork, `scripts/fork/save-tools/`.

1. The owner saves in-game, closes the game (PokeDaisy's CLOSE GAME, or back to the library) and swipes PKForge away, so neither app holds an older copy.
2. Check no app is running: `adb shell pidof com.pokedaisy.app io.github.gabzidevs.pkforge io.github.enchyrn.pokedaisy` prints nothing.
3. Pull the save and back it up twice: on the phone in `pokedaisy-backups/` beside it (PKForge doesn't scan that folder) and on the Mac.
4. Edit a copy: `add_exp.py` for Gen 3 (refuses if the EXP or a checksum isn't what's expected), the `ubexp` program for Unbound (writes through PKForge's own Unbound code, re-opens the result and checks every other slot is unchanged). Then check the byte difference is only what was meant (Gen 3: 6 bytes; Unbound party: 4 bytes in the live slot's party sector).
5. Push it, pull it back and compare byte for byte.

## Manual EXP reconciliations

Daycare stays the apps lost, added by hand:

| When | Pokémon | Save | EXP | Why |
| --- | --- | --- | --- | --- |
| 2026-10-26 | Enchyrn (Treecko) | Emerald | to 247 | The daycare refused about an hour (a friendship tick changed its bytes), then 2 more hours (pickup with the save not open) |
| 2026-10-27 | Enchyrn | Emerald | 247 → 456 (+209) | 17 h at Gentle, refused because the stay was checked in with the save closed |
| 2026-10-27 | Tyran (Larvitar) | Unbound | 807 → 1403 (+596) | The same 17 h, on Unbound's Medium Slow curve |

Backups of each save from before the 2026-10-27 edits are in `Emulation/ROMS/Saves/pokedaisy-backups/` on the phone (`*.before-daycare-17h-20261027.sav`).

## The owner's phone

- Redmi Note 15 Pro 5G, HyperOS 3, Android 16, Nova Lite controller. The game often runs on the Mac through AirSync's desktop mode.
- **Saves the games actually use:** `/sdcard/Emulation/ROMS/Saves/` (Emerald `Pokemon_-_Emerald_Version_(U).sav`, Unbound `Pokémon Unbound (v2.1.1.1).sav`). `/sdcard/Emulation/ROMS/gba/Saves/` holds an older Emerald save (2026-10-22). The copy under `Android/data/com.pokedaisy.app/files/saves/` didn't decode as a Gen 3 save.
- **Installed:** upstream PokeDaisy (`com.pokedaisy.app`), PKForge Fork (`io.github.gabzidevs.pkforge`), Obtainium. PokeDaisy Fork is published but wasn't installed yet.
- **adb over Wi-Fi:** the port changes whenever wireless debugging restarts (a reboot turns it off). `adb mdns services` lists the current `_adb-tls-connect` address; use the serial `adb devices` shows.
- **Release builds aren't debuggable,** so `run-as` can't read an app's own data: the daycare's state can't be inspected from adb. Logcat had nothing from PKForge's daycare.
- **"Can't use this folder"** in Android's folder picker blocked every folder once; restarting the storage provider didn't help, a reboot did. Don't force app-ops to work around it.
- A screenshot comes back black while the screen dozes.

## Moving to the fork apps

Uninstalling an app deletes its `Android/data/<package>/` folder. Before uninstalling upstream PokeDaisy:

- Saves in a folder the owner chose (like `Emulation/ROMS/Saves`) are safe; saves inside the app's own folder must be copied out first.
- A linked ROMs folder is safe; ROMs imported with + were copied into the app and must be added to the fork again.
- Resume and save states live in the app's folder and are lost; save in-game first. Android 16's file managers can't open `Android/data`, so copying states needs adb.
- Settings, button maps, cheats, covers and favorites don't carry over.

The same goes for upstream PKForge, if installed: its Bank and Poképark live in its own data.

## PokeDaisy internals worth knowing

- Live game changes go through `GameCalls` (`call`, `read`, `write`) on the emulator thread between frames (`EmulatorEngine.requestFieldItemJob`), answering NOT NOW in a battle, menu or map. Offline save edits go through `SaveFileEdits` (game closed, backup first, read back, then `boot_fresh`).
- Button bindings: `GbaControls` (`files/controls.properties`, per controller since 1.3.2-fork.1), read by `GbaInput` per key event's device. Rebinding is always by pressing the key (`KeyCapture`).
- Clearing PokeDaisy's storage removes its own folders (imported ROMs, states, settings, saves only if the saves folder is inside the app) but not a linked ROMs folder or a saves folder the owner picked.
- PokeDaisy's unit tests need the Android SDK, which this Mac doesn't have; they run in the fork's CI (GitHub Actions, enabled 2026-10-26). `ui-preview` builds here with Java 21 from mise and the cached Gradle 8.10.2, and renders screens for checking UI changes.
