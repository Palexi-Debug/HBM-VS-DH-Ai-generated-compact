package com.chunky.hbmnukemeka.client;

import com.chunky.hbmnukemeka.HbmNukeMeka;

import com.mojang.blaze3d.platform.GlUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Detail level of the Distant Horizons nuke cloud. {@code config/hbm_dh_cloud.properties} holds {@code quality}:
 * {@code AUTO} (default, chosen from the machine), {@code LOW}, {@code MEDIUM}, or {@code HIGH}. AUTO picks LOW on
 * integrated Intel graphics or four cores and fewer, MEDIUM below eight cores or 4 GB heap and HIGH otherwise.
 */
public enum DhQuality {
    //  stem rings, torus around, torus tube, fireball layers, dust boxes, ticks between rebuilds
    LOW(5, 12, 5, 4, 8, 20),
    MEDIUM(9, 18, 7, 6, 14, 10),
    HIGH(14, 24, 10, 9, 20, 10);

    public final int stemRings, torusAround, torusTube, fireLayers, dustBoxes, updateTicks;

    DhQuality(int stemRings, int torusAround, int torusTube, int fireLayers, int dustBoxes, int updateTicks) {
        this.stemRings = stemRings;
        this.torusAround = torusAround;
        this.torusTube = torusTube;
        this.fireLayers = fireLayers;
        this.dustBoxes = dustBoxes;
        this.updateTicks = updateTicks;
    }

    private static DhQuality resolved;

    /** The level in use; read from the file and the machine once, on first use (render thread). */
    public static synchronized DhQuality current() {
        if (resolved != null) return resolved;
        String setting = readSetting();
        DhQuality q = null;
        if (!setting.equals("AUTO")) {
            try {
                q = valueOf(setting);
            } catch (IllegalArgumentException ignored) {}
        }
        String gpu = "unknown";
        try {
            gpu = String.valueOf(GlUtil.getRenderer());
        } catch (Throwable ignored) {}
        int cores = Runtime.getRuntime().availableProcessors();
        long heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (q == null) {
            String g = gpu.toLowerCase(Locale.ROOT);
            boolean integratedIntel = g.contains("intel") && (g.contains("uhd") || g.contains("hd graphics") || g.contains("iris"));
            if (integratedIntel || cores <= 4) q = LOW;
            else if (cores < 8 || heapMb < 4096) q = MEDIUM;
            else q = HIGH;
        }
        HbmNukeMeka.LOGGER.info("DH torex quality {} (setting {}, gpu '{}', {} cores, {} MB heap)", q, setting, gpu, cores, heapMb);
        resolved = q;
        return q;
    }

    private static String readSetting() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("hbm_dh_cloud.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (var in = Files.newInputStream(file)) {
                    p.load(in);
                }
            } else {
                p.setProperty("quality", "AUTO");
                try (var out = Files.newOutputStream(file)) {
                    p.store(out, "Distant Horizons nuke cloud detail: AUTO, LOW, MEDIUM, HIGH (AUTO picks from your hardware)");
                }
            }
        } catch (IOException e) {
            HbmNukeMeka.LOGGER.warn("DH torex: could not read {}: {}", file, e.toString());
        }
        return p.getProperty("quality", "AUTO").trim().toUpperCase(Locale.ROOT);
    }
}
