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

    /** Supplies an argument for one constructor/factory parameter, or NO when the type is unknown. */
    private static final Object NO = new Object();

    private static Object arg(Class<?> t, ServerLevel level, BlockPos pos) {
        if (t == BlockPos.class) return pos;
        if (t == int.class) return NEXT_INT[0]++ % 3 == 0 ? pos.getX() : NEXT_INT[0] % 3 == 1 ? pos.getY() : pos.getZ();
        if (t == double.class) return NEXT_INT[0]++ % 3 == 0 ? (double) pos.getX() : NEXT_INT[0] % 3 == 1 ? (double) pos.getY() : (double) pos.getZ();
        if (t.isAssignableFrom(level.dimension().getClass())) return level.dimension();
        if (t.isAssignableFrom(level.getClass())) return level;
        if (t.isAssignableFrom(GlobalPos.class)) return GlobalPos.of(level.dimension(), pos);
        return NO;
    }

    private static final int[] NEXT_INT = {0};

    /** Builds the first argument of the source overload from whatever type this Mekanism version expects. */
    private static Object where(Class<?> type, ServerLevel level, BlockPos pos) throws ReflectiveOperationException {
        if (type.isAssignableFrom(GlobalPos.class)) return GlobalPos.of(level.dimension(), pos);
        for (Constructor<?> c : type.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            Object[] a = new Object[p.length];
            NEXT_INT[0] = 0;
            boolean ok = true;
            for (int i = 0; i < p.length && ok; i++) {
                // ints/doubles are x, y, z in order
                if (p[i] == int.class) a[i] = NEXT_INT[0] == 0 ? pos.getX() : NEXT_INT[0] == 1 ? pos.getY() : pos.getZ();
                else if (p[i] == double.class) a[i] = NEXT_INT[0] == 0 ? (double) pos.getX() : NEXT_INT[0] == 1 ? (double) pos.getY() : (double) pos.getZ();
                else { a[i] = arg(p[i], level, pos); ok = a[i] != NO; continue; }
                NEXT_INT[0]++;
            }
            if (ok && p.length > 0) return c.newInstance(a);
        }
        for (java.lang.reflect.Method m : type.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.getReturnType() != type) continue;
            Class<?>[] p = m.getParameterTypes();
            Object[] a = new Object[p.length];
            boolean ok = p.length > 0;
            for (int i = 0; i < p.length && ok; i++) { NEXT_INT[0] = 0; a[i] = arg(p[i], level, pos); ok = a[i] != NO; }
            if (ok) return m.invoke(null, a);
        }
        StringBuilder sb = new StringBuilder("no usable constructor for " + type.getName() + ":");
        for (Constructor<?> c : type.getConstructors()) sb.append(' ').append(c);
        throw new NoSuchMethodException(sb.toString());
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
