package com.hbm.compat.mekanism;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.slf4j.Logger;
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
    private static boolean disabled;
    private static Object manager;
    private static Method radiateEntity;
    private static Method radiatePos;

    private MekanismRadiation() {}

    private static synchronized boolean ensureResolved() {
        if (disabled) return false;
        if (resolved) return manager != null;
        resolved = true;
        try {
            Class<?> api = Class.forName("mekanism.api.radiation.IRadiationManager");
            manager = api.getField("INSTANCE").get(null);
            for (Method m : api.getMethods()) {
                if (!m.getName().equals("radiate")) continue;
                LOGGER.info("Mekanism radiation overload: {}", m);
                if (m.getParameterCount() != 2) continue;
                Class<?> first = m.getParameterTypes()[0];
                if (first.isAssignableFrom(LivingEntity.class) && first != Object.class) radiateEntity = m;
                else radiatePos = m;
            }
            LOGGER.info("Mekanism radiation bridge: entity={}, source={}", radiateEntity != null, radiatePos != null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            manager = null;
            LOGGER.warn("Mekanism radiation API not found, nuke radiation is NOT applied: {}", e.toString());
        }
        return manager != null;
    }

    private static void fail(String what, Throwable e) {
        disabled = true;
        LOGGER.warn("Mekanism radiation bridge ({}) failed and is disabled: {}", what, e.toString());
    }

    /** Irradiates a living entity with HBM-scaled radiation. Replaces HBM's own radiation for every source. */
    public static boolean radiate(LivingEntity entity, double hbmRad) {
        if (hbmRad <= 0 || entity.level().isClientSide()) return false;
        if (entity instanceof Player p && (p.isSpectator() || p.isCreative())) return false;
        if (!ensureResolved() || radiateEntity == null) return false;
        try {
            radiateEntity.invoke(manager, entity, hbmRad * RAD_TO_SV);
            return true;
        } catch (Throwable e) {
            fail("entity", e);
            return false;
        }
    }

    /** Builds the first argument of the source overload from whatever type this Mekanism version expects. */
    private static Object where(Class<?> type, ServerLevel level, BlockPos pos) throws ReflectiveOperationException {
        if (type.isAssignableFrom(GlobalPos.class)) return GlobalPos.of(level.dimension(), pos);
        for (Constructor<?> c : type.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 2 && p[0] == BlockPos.class && p[1].isAssignableFrom(level.dimension().getClass())) {
                return c.newInstance(pos, level.dimension());
            }
            if (p.length == 2 && p[0] == BlockPos.class && p[1].isAssignableFrom(level.getClass())) {
                return c.newInstance(pos, level);
            }
            if (p.length == 4 && p[0] == int.class && p[3].isAssignableFrom(level.dimension().getClass())) {
                return c.newInstance(pos.getX(), pos.getY(), pos.getZ(), level.dimension());
            }
        }
        throw new NoSuchMethodException("no usable constructor for " + type.getName());
    }

    /** Creates a decaying Mekanism radiation source, used for the fallout zone of a nuke. */
    public static void source(ServerLevel level, BlockPos pos, double svPerHour) {
        if (svPerHour <= 0 || !ensureResolved() || radiatePos == null) return;
        try {
            Class<?> type = radiatePos.getParameterTypes()[0];
            radiatePos.invoke(manager, where(type, level, pos), svPerHour);
        } catch (Throwable e) {
            fail("source " + radiatePos, e);
        }
    }
}
