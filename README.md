# NTM-VS-DH

**Nuclear bombs for Minecraft 1.20.1 (Forge): crafted with Mekanism, radiation from Mekanism, mushroom clouds in Distant Horizons, dropped from Valkyrien Skies ships.**

NTM-VS-DH is a compact fork of *HBM's Nuclear Tech Mod: Continuum*. It keeps the bombs and removes everything else of the original mod.

## Features

- **Bombs only.** Little Boy, Fat Man, Gadget, Ivy Mike, Tsar Bomba, N2, Solinium and the Balefire Bomb, with all the parts they need.
- **Crafted with Mekanism.** Every recipe is a Mekanism machine recipe (combining, infusing, chemical reaction, nucleosynthesizing). Every bomb needs plutonium, the big ones need a lot of it. Mekanism is required.
- **Mekanism radiation.** Fallout contaminates the whole blast area with Mekanism radiation. Tooltips show the radiation (Sv/h) and the real weight of each bomb.
- **Distant Horizons.** The mushroom cloud is visible from far away (LOW / MEDIUM / HIGH / HIGH_PLUS, automatic by default).
- **Bomb Drop Rack.** Hang a bomb under the rack, send a redstone signal, it falls. On a Valkyrien Skies ship the bomb keeps the ship's speed. Bomb masses follow the real values.
- **Nothing else of HBM.** No HBM ores, structures, meteors, hostile mobs, mob drops, gases, pollution, hazards, advancements, keybinds or movement hooks.
- **Missiles** are still in the code but disabled. Set `missiles=true` in `config/hbm_bombs.properties` to show them again.

## Requirements

| Mod | Needed |
|---|---|
| Minecraft 1.20.1, Forge | required |
| [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) | **required** |
| [Distant Horizons](https://www.curseforge.com/minecraft/mc-mods/distant-horizons) | optional (far cloud rendering) |
| [Valkyrien Skies](https://www.curseforge.com/minecraft/mc-mods/valkyrien-skies) | optional (drop from ships) |

The mod id is still `hbm`, so it cannot be loaded together with the original NTM Continuum.

## Addon for the original HBM

The folder [`addon/`](addon) is a small separate mod (`hbmnukemeka`) for people who run the **original** HBM: it adds the Distant Horizons cloud and the drop rack to it.

## AI disclosure and modification notice

Parts of this fork (the Distant Horizons rendering, the Mekanism radiation bridge, the drop rack and the recipes) were written with AI assistance (Claude by Anthropic) and tested by the author in a modpack. The original code is not written by the author of this fork. All changes to the original are listed in [`continuum-bombs/CHANGELOG-FORK.md`](continuum-bombs/CHANGELOG-FORK.md).

## License

GNU Lesser General Public License v3.0 (LGPL-3.0), see [`LICENSE`](LICENSE). This is a modified version of the original mod and is not affiliated with or endorsed by its authors. The source of this version is in this repository under [`continuum-bombs/`](continuum-bombs).

## Credits

- *HBM's Nuclear Tech Mod* by HbmMods (Bob).
- *NTM: Continuum* by Warfactory-Official ([source](https://github.com/Warfactory-Official/NTM-Continuum), with [Golem Core](https://github.com/Warfactory-Official/Golem-Core)).
- Mekanism, Distant Horizons and Valkyrien Skies by their authors.
