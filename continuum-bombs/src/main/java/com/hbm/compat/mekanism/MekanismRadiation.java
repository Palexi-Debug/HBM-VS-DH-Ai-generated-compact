package com.hbm.compat.mekanism;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Constructor;
import org.slf4j.Logger;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * FORK (bombs only): the radiation of the nukes runs through Mekanism's radiation system; this fork keeps no radiation
 * system of its own. Mekanism is reached by reflection ({@code mekanism.api.radiation.IRadiationManager.INSTANCE}), so the
 * fork needs no compile-time Mekanism dependency and tolerates the API differences between Mekanism versions. If a
 * method cannot be bound, one clear line is logged and the radiation is simply not applied.
 */
public final class MekanismRadiation {

    /** HBM rad to Mekanism Sv: 1000 rad (lethal in HBM) is 1 Sv. */
    public static double RAD_TO_SV = 0.001D;
    /** Fallout source strength in Sv/h per block of blast radius (Fat Man, radius 175: about 875 Sv/h at the centre). */
    public static double FALLOUT_SV_PER_BLOCK = 5.0D;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved;
    private static Object manager;
    private static Method radiateEntity;
    private static Method radiatePos;
    private static Constructor<?> coordCtor;     // Mekanism's Coord4D (older API), null when the API takes a GlobalPos

    private MekanismRadiation() {}

    private static synchronized boolean ensureResolved() {
        if (resolved) return manager != null;
        resolved = true;
        try {
            Class<?> api = Class.forName("mekanism.api.radiation.IRadiationManager");
            manager = api.getField("INSTANCE").get(null);
            for (Method m : api.getMethods()) {
                if (!m.getName().equals("radiate") || m.getParameterCount() != 2) continue;
                Class<?> first = m.getParameterTypes()[0];
                if (first == LivingEntity.class) {
                    radiateEntity = m;
                } else if (first == GlobalPos.class) {
                    radiatePos = m;
                } else if (first.getSimpleName().equals("Coord4D")) {
                    radiatePos = m;
                    for (Constructor<?> c : first.getConstructors()) {
                        Class<?>[] p = c.getParameterTypes();
                        if (p.length == 2 && p[0] == BlockPos.class) coordCtor = c;           // (BlockPos, ResourceKey<Level>)
                    }
                }
            }
            LOGGER.info("Mekanism radiation bridge: entity={}, source={}",
                    radiateEntity != null, radiatePos != null);
        } catch (ReflectiveOperationException | LinkageError e) {
            manager = null;
            LOGGER.warn("Mekanism radiation API not found, nuke radiation is NOT applied: {}", e.toString());
        }
        return manager != null;
    }

    /** Irradiates a living entity with HBM-scaled radiation. Replaces HBM's own radiation for every source. */
    public static boolean radiate(LivingEntity entity, double hbmRad) {
        if (hbmRad <= 0 || entity.level().isClientSide()) return false;
        if (entity instanceof Player p && (p.isSpectator() || p.isCreative())) return false;
        if (!ensureResolved() || radiateEntity == null) return false;
        try {
            radiateEntity.invoke(manager, entity, hbmRad * RAD_TO_SV);
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Mekanism radiate(entity) failed: {}", e.toString());
            return false;
        }
    }

    /** Creates a decaying Mekanism radiation source, used for the fallout zone of a nuke. */
    public static void source(ServerLevel level, BlockPos pos, double svPerHour) {
        if (svPerHour <= 0 || !ensureResolved() || radiatePos == null) return;
        try {
            Object where = coordCtor != null ? coordCtor.newInstance(pos, level.dimension())
                                             : GlobalPos.of(level.dimension(), pos);
            radiatePos.invoke(manager, where, svPerHour);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Mekanism radiate(source) failed: {}", e.toString());
        }
    }
}
