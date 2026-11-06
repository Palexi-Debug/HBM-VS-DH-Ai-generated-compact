# Fork changelog (modifications to NTM Continuum)

This mod is a **modified version** of *NTM Continuum* (HBM's Nuclear Tech Mod: Continuum, by Warfactory-Official /
Golem, https://github.com/Warfactory-Official/NTM-Continuum), licensed under the **GNU LGPL-3.0**
(see `LICENSE` and `LICENSE.LESSER`). It is not the original mod and is not affiliated with or endorsed by its authors.
All original copyright and license notices are kept. This file lists the changes made in this fork, as required by the
license. The upstream release history stays in `CHANGELOG.md`.

Base: upstream NTM Continuum 1.0.3, Minecraft 1.20.1 Forge, built against Golem Core commit
`5675ea76693dc0300a4c9fb59af032da5ab4825a` (https://github.com/Warfactory-Official/Golem-Core).

## Changes

### Gameplay
- Bombs are crafted with Mekanism machines (Mekanism is now a required dependency); HBM's own recipes are removed.
  Recipes are in `src/main/resources/data/hbmbombs/recipes/` and were made more expensive (more uranium, plutonium,
  polonium, refined obsidian, longer reaction times and higher gas amounts).
- Fallout radiation is a Mekanism radiation source over the whole fallout zone (`compat/mekanism/MekanismRadiation`).
- Tooltips show Mekanism radiation (Sv/h) and the bomb weight (`explosion/BlastInfo`).
- Added the Bomb Drop Rack (`nuke_drop_rack`) and the falling bomb entity (`platform/forge/drop/*`,
  `blocks/bomb/BlockBombRack`, `BombDropHook`), with optional Valkyrien Skies support (read by reflection) and real
  bomb masses for Valkyrien Skies (`data/hbm/vs_mass/bomb_masses.json`).
- Missiles are kept but disabled by default (`config/hbm_bombs.properties`, `missiles=true` enables them).
  Not a recipe path anymore: see `compat/BombsForkConfig`, `creativetabs/ModTabs`.

### Rendering
- Mushroom clouds are rendered in Distant Horizons (optional): quality LOW / MEDIUM / HIGH / HIGH_PLUS, AUTO chooses by
  hardware (`platform/forge/dh/*`, ghost client simulation in `entity/effect/EntityNukeTorex`).
- Bomb orientation when dropped and rendered (`platform/forge/drop/DroppedBombRenderer`).

### Removed / disabled
- Mixins that interfered with ParCool; the O-to-duck key, HBM jetpack/dash/gun key input handling, HBM player pose,
  flight pose and camera hooks (`client/HbmKeybindsClient`, `client/ArmorSetClient`, `client/HbmClientRegistrations`,
  `config/MobConfig` duck default off).
- All HBM key bindings are hidden from the Controls screen (`platform/forge/ForgeClientInit`).
- The entity-renderer coverage check no longer reports `hbm:dropped_bomb` (`client/render/HbmEntityRenderers`).

### Metadata
- Display name, description and README changed to describe the fork (`gradle.properties`, `README.md`).
  The mod id `hbm` is unchanged, so it cannot be loaded together with the original NTM Continuum.
- `mods.toml`: Mekanism (required), Distant Horizons and Valkyrien Skies (optional) declared.
- CI workflow `.github/workflows/continuum.yml` builds the fork against a pinned Golem Core commit.

## Source
The full source of this modified version, including every change above, is in this repository
(https://github.com/Palexi-Debug/NTM-VS-DH-Ai-generated-compact), under `continuum-bombs/`.
