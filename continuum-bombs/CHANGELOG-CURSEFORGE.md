# 1.0.1

Fork of HBM's Nuclear Tech Mod: Continuum (LGPL-3.0), bombs only. Minecraft 1.20.1, Forge. Needs Mekanism.
Optional: Distant Horizons, Valkyrien Skies.

## Changes since the last release

### Crafting
- All bomb recipes are now **harder**: more uranium, plutonium, polonium and refined obsidian, longer reaction times, double gas amounts, reinforced alloy for igniters.
- Beginner bombs are cheaper: Little Boy uses 2 fissile cores (was 4), Fat Man 12 plutonium pellets (was 24), Gadget 16 (was 32).
- **Every bomb needs plutonium** (directly, or through the fissile core).
- The creative tab lists only bombs and parts you can really craft. Removed from the tab: Prototype, all kits, and every bomb without a recipe.

### Removed
- HBM mob drops and HBM gear on mobs.
- Fleija and Blue Rinse bombs.
- Coal dust gas that appeared around broken coal ore.
- Meteors, HBM ores, structures, ruins and dungeons.
- Hostile HBM mobs, hives, raids, scouts, elementals.
- Gas pockets, radon, asbestos, carbon monoxide, lead poison, soot and pollution.
- HBM item hazards: no more radiation or particle effects from holding uranium, coal and similar items.
- HBM advancements.
- HBM machine recipe categories in JEI (faster loading).

### Fixes
- Gadget wiring recipe used a copper ingot item that does not exist (now the vanilla copper ingot).
- The Balefire Bomb recipe pointed at a wrong item and did not load. It works now and is listed in the creative tab.

### Other
- The Bomb Drop Rack has a new look: separate top, side and bottom textures (steel plate with warning light, hazard stripes, open bay with hook rail).
- Recipes added for parts that were missing: Solinium core, igniter and propellant, the Balefire egg, and the Spark battery (needed to arm the Balefire Bomb).
- New textures for the Gadget, Fat Man, Mike and Tsar Bomba cores: each has its own glow colour (green, orange, blue, red) so they no longer look the same. New Bomb Drop Rack texture.
- Update check: the mod list shows a notice when a newer version exists.
- New name and description. Fork changelog with all changes: `CHANGELOG-FORK.md`.
- ERROR line about the falling bomb renderer in the log is gone.
- Mod id is still `hbm`, so it cannot run together with the original NTM Continuum.
