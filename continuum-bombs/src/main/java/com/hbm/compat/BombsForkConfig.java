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
}
