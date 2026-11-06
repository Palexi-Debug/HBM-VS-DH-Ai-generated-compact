package com.hbm.compat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Switches of the bombs-only fork, read once from {@code config/hbm_bombs.properties}.
 *
 * <p>{@code missiles=false} (default): the missile code stays in the mod, but the missiles, warheads, launch pads and
 * designators are not shown in the creative tab. Set {@code missiles=true} to show them again.
 */
public final class BombsForkConfig {

    private static Boolean missiles;

    private BombsForkConfig() {}

    public static synchronized boolean missilesEnabled() {
        if (missiles != null) return missiles;
        Path file = Paths.get("config", "hbm_bombs.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (var in = Files.newInputStream(file)) {
                    p.load(in);
                }
            } else {
                Files.createDirectories(file.getParent());
                p.setProperty("missiles", "false");
                try (var out = Files.newOutputStream(file)) {
                    p.store(out, "HBM bombs fork. missiles=true shows the (still uncraftable) missiles, launch pads and designators in the creative tab");
                }
            }
        } catch (IOException e) {
            com.hbm.Constants.LOG.warn("Could not read {}: {}", file, e.toString());
        }
        missiles = Boolean.parseBoolean(p.getProperty("missiles", "false").trim());
        com.hbm.Constants.LOG.info("Missiles are {} (config/hbm_bombs.properties)", missiles ? "enabled" : "disabled");
        return missiles;
    }

    /**
     * FORK (bombs only): switches off everything of the normal HBM that is not a bomb: meteors, ore and structure
     * generation, hostile HBM mobs and their gear, hives, raids, gas pockets and environmental hazards. Applied after
     * hbm.cfg is read and not written back, so the file keeps its values.
     */
    public static void applyOverrides() {
        com.hbm.config.WorldConfig.enableMeteorStrikes = false;
        com.hbm.config.WorldConfig.enableMeteorShowers = false;
        com.hbm.config.WorldConfig.enableSpecialMeteors = false;
        com.hbm.config.WorldConfig.enableMeteorTails = false;
        com.hbm.config.WorldConfig.overworldOre = false;
        com.hbm.config.WorldConfig.netherOre = false;
        com.hbm.config.WorldConfig.endOre = false;
        com.hbm.config.WorldConfig.enableSulfurCave = false;
        com.hbm.config.WorldConfig.enableAsbestosCave = false;
        com.hbm.config.WorldConfig.enableHematite = false;
        com.hbm.config.WorldConfig.enableMalachite = false;
        com.hbm.config.WorldConfig.enableBauxite = false;
        com.hbm.config.DungeonConfig.enableMines = false;
        com.hbm.config.DungeonConfig.enableVaults = false;
        com.hbm.config.GeneralConfig.enableRadon = false;
        com.hbm.config.GeneralConfig.enableAsbestosDust = false;
        com.hbm.config.GeneralConfig.enableCarbonMonoxide = false;
        com.hbm.config.GeneralConfig.enableFlammableGas = false;
        com.hbm.config.GeneralConfig.enableExplosiveGas = false;
        com.hbm.config.RadiationConfig.enableLeadPoisoning = false;
        com.hbm.config.RadiationConfig.enablePoison = false;
        com.hbm.config.RadiationConfig.enableSootFog = false;
        com.hbm.config.RadiationConfig.enablePollution = false;
        com.hbm.config.MobConfig.enableRaids = false;
        com.hbm.config.MobConfig.enableHives = false;
        com.hbm.config.MobConfig.enableInfestation = false;
        com.hbm.config.MobConfig.enableMaskman = false;
        com.hbm.config.MobConfig.enableElementals = false;
        com.hbm.config.MobConfig.enableMobGear = false;
        com.hbm.config.MobConfig.enableMobWeapons = false;
        com.hbm.config.MobConfig.rampantNaturalScoutSpawn = false;
        com.hbm.config.MobConfig.scoutInitialSpawn = false;
        com.hbm.config.StructureConfig.enableRuins = false;
        com.hbm.config.StructureConfig.enableOceanStructures = false;
        for (java.lang.reflect.Field f : com.hbm.config.StructureConfig.class.getDeclaredFields()) {
            try {
                if (f.getType() == int.class && f.getName().endsWith("SpawnWeight")
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && !java.lang.reflect.Modifier.isFinal(f.getModifiers())) f.setInt(null, 0);
            } catch (ReflectiveOperationException ignored) {
                // the structure stays at its configured weight
            }
        }
    }
}
