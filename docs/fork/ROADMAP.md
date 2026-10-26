# Roadmap

Brainstorm so far, as decisions and open ideas. Nothing here is built yet unless it says so. For the fork itself see [README.md](README.md). The brainstorm started in the [PKForge fork](https://github.com/enchyrn/PKForge/blob/main/docs/fork/ROADMAP.md) and moved here on 2026-10-26, once PokeDaisy became the hub.

## Principle: organic, not cheating

Companion features should make the game more convenient and more alive, not skip it.

- Rewards are earned by playing, and scaled to what Emerald itself gives: its Pickup lists, the Berry Master, daily limits.
- They're gated by story progress (badges and so on). A feature the real game only opens partway through, such as the Day Care, opens at the same point here.
- Events, such as the Eon Ticket, arrive in story order.
- Time-based rewards (AFK, time away from the game) are capped per day, so they can't be farmed.

The PKForge fork's daycare pace change (Off / Slow / Gentle / Original) follows the same idea.

## Target

This fork is the hub. Features are added as modules behind a small host interface:

- read saves
- read live memory, through `MemoryReader`
- write to the running game at safe moments
- add companion screens

Keep the modules clean enough to become separate-app plugins later. PKForge stays a design reference, not a code dependency: PKHeX.Core is C#, and the organic features don't need it. Upstream changes almost daily, so the fork's changes to upstream's own files stay as small as possible; modules live in their own code area.

## What PokeDaisy already does (read 2026-10-26, upstream `371d34f`)

- **Live changes to the running game.** The companion's ITEMS tab uses medicine, PP items and Repels, gives and takes held items, flies and opens the PC anywhere. It calls the game's own functions and writes RAM through `GameCalls` (`call`, `read`, `write`), on the emulator thread between frames (`EmulatorEngine.requestFieldItemJob`). If the game is in a battle, a menu or the region map, the request answers NOT NOW.
- **Offline save edits.** The library's PC BOXES editor (`SaveEditorActivity`, `Gen3SaveData`) edits the bag, held items, moves, money, flags and the dex, and trades between two saves. `SaveFileEdits` only writes while the game is closed: the file must be unchanged since it was read, a backup must exist, the write goes through a temp file and is read back, and the next launch boots from the save instead of a resume state.
- **Cheats.** GameShark / Action Replay / CodeBreaker codes through mGBA. Not something to build on.
- No daycare of its own.

## Decisions

### Setups

The modules make feature sets a player choice:

- **Core:** PokeDaisy's own quality-of-life features. Nothing added to the game.
- **Organic:** core plus the features that follow the principle above.
- **Full:** everything, including the deliberate breaks from the real games (two-way transfers, the Cobblemon bridge).
- **Custom:** the player turns modules on and off.

A new save picks any setup. A save in progress keeps its setup until it reaches a story point where a change makes sense; this may loosen later. Each module declares its name, the setups that include it, what it reads and writes, and whether it counts as organic. A setup is a named list of modules. Turning a module off must leave the save working, with nothing half-applied.

### How modules change the game

- During play, only through the live path above, at safe overworld moments. The game saves the result the next time the player saves.
- Pack-up is the visible moment rewards land. It isn't tied to saving.
- The offline save editor is for work done while the game is closed, such as Bank transfers.

### Daycare

Emerald's Day Care (Route 117) stays the game's: 1 EXP per step, nothing while the game is off, and (to confirm against pokeemerald) moves learned on level-up, no evolution. A Pokémon in it isn't in the party or boxes. The fork's daycare opens when the story reaches the Route 117 Day Care, and grows into a Poképark-style version later.

PKForge's Poképark daycare counts real time instead (Gentle: about a level per 8 hours), writes exact EXP and recalculated stats while the game is closed, and leaves moves alone. It writes back to the same individual (trainer ID and PID, `ParkIdentity`) however it was played since; until 2026-10-26 it refused any change at all, so one friendship point from walking blocked the pickup. For scale: a level 20 Pokémon needs about 1,260 EXP for level 21, which is about 1,260 steps in the game's Day Care, or about 8 hours at Gentle.

When this fork's modules give EXP, it goes through the game's own level-up code, so moves and stats behave as the game would; evolution waits for the next level gained in battle.

### Camp

Camp comes first, before the daycare: rough training out in the wild, there from early on.

Why camp earns anything at all: benefits from outside normal play are only fair when the player is doing something for them. Setting up and running a camp is that something, extra gameplay in its own right, so its rewards are tied to it rather than handed out.

Early camps are rudimentary, so pitching one is quick. The payoff grows with what the player puts in: where it is, what was built and upgraded, how it is tended.

- A camp is a place: the player builds a campsite on a route or a town's outskirts. Nobody is dropped off; whoever is with the player is camping.
- The party trains by sparring with teammates (mostly friendship, a little EXP) and by meeting the place's own wild Pokémon, drawn from its encounter table at its levels, with EXP worked out as the game would for beating them. That limits itself: Route 101 does almost nothing for a level 40 team.
- Camping can be active (playing the route while camp runs alongside) or AFK. Closing the game while camped counts too: the time away is worked out on return, PKForge-style, into a short log.
- A companion screen shows the camp as a scene, Poképark-style, with the route's wild Pokémon visiting.
- Moving a campsite means packing up.
- This fork owns camp: its rules, its state, and every write to the game. Camp state goes in a small file beside the save (a camp journal, in a written-down format), so PKForge's Poképark can later read it and make camps walkable, queueing anything done there for this fork to apply at the next pack-up. PKForge never edits the save for camp. This fork comes first; the PKForge side is a later step.

#### Tending

All of these are wanted; each leans on something Emerald already has.

1. **Campfire cooking.** Berries from the bag cooked into meals. Natures like or dislike flavours as in the game, so a liked meal gives friendship, and later contest condition the way Pokéblocks do.
2. **Field-move chores.** Cut clears brush to grow the camp, Rock Smash breaks stones for materials, Strength moves logs, Flash lights the camp at night, Surf fishes for food. Field moves need their badges, so chores grow with the story.
3. **Foraging.** A Pokémon with Pickup rolls Emerald's own Pickup table for its level; others bring berries or materials. Capped per day.
4. **Sparring drills.** The player picks who spars with whom (type matchups matter): friendship, a little EXP, some effort values.
5. **Luring visitors.** Berries (later Pokéblocks) left out, like the Safari Zone feeders, draw the route's wild Pokémon into camp: the player learns who lives there, and they're marked seen in the Pokédex.
6. **Route-flavoured upkeep.** Weather changes the chores: rain puts the fire out, Route 113's ash is swept up as a material, sandstorms wear the tent.
7. **Decorating.** Secret Base decorations from the PC furnish the camp.
8. **Campfire stories.** Pokémon who camp together build relationships and a shared journal (PKForge's Poképark social layer).

Order for the owner's run (started 2026-10-26, Treecko "Enchyrn", in Oldale Town), by when Emerald opens each one:

1. From Routes 101-104, with two or more Pokémon: sparring, campfire stories, cooking and luring with picked berries, foraging (Zigzagoon has Pickup right there).
2. Stone Badge (Rustboro): Cut chores.
3. Dewford to Mauville: Flash and Rock Smash, Route 113's ash, Secret Power on Route 111 (decorating, the Secret Base link).
4. Later: Strength, Surf fishing, sandstorm upkeep (Go-Goggles), cooking for contest conditions (Lilycove).

#### Wild visitors you can fight and catch

- A visitor can start a real wild battle: the same script trick PokeDaisy's "PC anywhere" uses, at a safe field moment, with a Pokémon from that route's encounter table at its levels. The player fights it in the game and catches it with real Poké Balls.
- Befriending makes catching easier the game's way. Gen 3's catch formula has no friendship term, so a visitor comfortable at camp arrives drowsy (asleep doubles the catch chance in Gen 3) or worn out (lower HP), and once caught starts with higher friendship, like a Friend Ball.
- Auto-catch is a late upgrade: only visitors befriended over several visits, a Poké Ball spent from the bag, the real catch formula rolled (a miss still costs the ball), capped per day, and the Pokémon given through the game's own path so its met location and the Pokédex read as a catch.

#### Also fits

- **Camp berry patch:** berries planted at camp grow on Emerald's own growth times and go to the bag when picked.
- **Resting at the campfire:** a tent upgrade that slowly restores HP while camped or away.
- **Rare camp events:** Emerald's rarities (Mirage Island days, swarms) hinted at camp, never forced.
- **Kept out:** nothing changes shiny odds, natures or IVs; visitors are rolled as the game would roll them.

## Open threads

- **Standing camps.** A site can be left up with some Pokémon there, or taken down. The ones left behind would sit in a PC box (one per camp, named after it) and train like a scaled-down daycare; boxed Gen 3 Pokémon store EXP but not level or stats (to confirm), so adding EXP to them is clean. Standing camps become mini hideouts to revisit. Emerald's Secret Bases are real in-game hideouts on routes (unlocked by Secret Power on Route 111, one at a time), so the main camp could be the Secret Base, walked into in the game, with other standing camps in the companion.
- **Travel training.** Parked: still vague. It could be in-game walking between campsites, real-world walking (Pokéwalker, below), or both. Walking in the game already raises friendship and battles give EXP.
- **Pokéwalker.** The phone counts real steps even with the game closed. Take a Pokémon on a walk: real steps become EXP, friendship and finds, from courses that unlock with story progress. Its currency (HGSS's "watts") could be where materials come from.
- **Materials.** Earned by playing (camp finds, real walking), spent on building and improving camps and unlocking modes.
- **Treasures as items.** Poképark-style finds becoming real items: maybe too cheaty, but "a grind is still a grind". Pickup-style finds from Emerald's own lists, badge-gated and capped per day, are the likely middle ground.
- **Poképark pieces to borrow:** relationships and a shared journal between Pokémon, an offline journal, released Pokémon returning as visitors with hearts, berry tastes by nature.
- **Monorepo.** Modules from PKForge and PokeDaisy in one combined repo, with a companion that doesn't care which emulator runs the game.

### ROM hack territory

Anything seen inside the game world: tents on routes, more than one real hideout to walk into, camp Pokémon walking around, new scripts or maps. That waits for the modernized Emerald hack.

### Choosing hacks (compared 2026-10-26, partly unverified)

Unbound (CFRU on FireRed) can't send Pokémon back to vanilla games legally: its PC record drops met data and friendship, and its growth curves and moves differ. That comes from the expanded engine, not from FireRed. No polished Emerald story hack avoids it either: Lazarus, Heart & Soul, Seaglass, Quetzal and Elite Redux all add species and moves, so their saves aren't plain Emerald.

- **Decision for now:** stay on Unbound and treat crossing between game universes as one-way, with a penalty (see the work items).
- **Heart & Soul** is the strongest Emerald-based alternative: it has public source, and PokeDaisy fully supports it (guide and area data). PKForge only opens it read-only.
- Lazarus, Seaglass and Quetzal get PokeDaisy's partial support (next boss and current location); Elite Redux has only test fixtures; Crown wasn't found.
- **Still to research:** light-edit Emerald hacks that keep Emerald's save layout and only the original 386 species. These are the only kind that could keep transfers to vanilla Emerald legal.

Camp is unaffected by all of this. It never moves Pokémon between games, and each hack would need only its own route tables.

## Work items

Small, concrete follow-ups, apart from the ordered steps below.

### PokeDaisy

- **Controller mappings per device.** Every binding goes in one `controls.properties`, keyed by Android key name. `GbaControls.setBinding` replaces the button's whole key list with the key just pressed, so binding A on the Nova Lite drops the keyboard's A. The keyboard and the controller can't both keep their own maps.
  - Keep one map per input device, chosen by the device that sent the key (`InputDevice` descriptor, falling back to its name), and use the map of whichever device is pressing.
  - Add a "Reset to default" per device, built from `GbaControls.DEFAULTS`.
  - Existing files become the map of the device that's connected first, or of every device.

### PKForge

- **Cross-game EXP rule.** When a Pokémon enters a game whose growth curve differs (for example Unbound's Medium Slow Larvitar), decide between keeping its level and resetting its progress inside that level, or keeping half the EXP gap. Whichever is chosen counts as the "penalty for crossing universes".
- **Unbound daycare that writes EXP only.** The Poképark daycare writes EXP to Unbound Pokémon and leaves levelling up and stats to the game, recovering part of the lost EXP organically.
- **Heart & Soul save engine.** Only if a move to Heart & Soul is chosen.

## Order

1. This fork and the module host.
2. Camp, as the first module on the host.
3. Bank and transfers. Both directions are allowed, a deliberate break from the real games.
4. Daycare, opening with the Route 117 Day Care, then Poképark.
5. Cobblemon bridge.
6. A modernized Emerald hack (pokeemerald-expansion), as a separate, later project.

## Setup

- Phone: Redmi Note 15 Pro 5G (HyperOS 3, Android 16) with a Nova Lite controller.
- The game runs on the Mac through AirSync's native desktop mode (a scrcpy virtual display), with the touch screen and companion on the phone.
- A telescopic controller later.

## RetroArch option

RetroArch's Android build gained network commands (`READ_CORE_MEMORY`, UDP 55355) on 2026-03-25. They are in nightly builds only, after 1.22.2. A companion could use them to read a game running in RetroArch. PokeDaisy's action features (Fly, item use) call game code in-process, so they wouldn't work that way.

## Contributions

Forks only for now; contributions upstream can come later.
