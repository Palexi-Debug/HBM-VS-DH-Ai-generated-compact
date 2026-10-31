package com.chunky.hbmnukemeka.server;

import com.chunky.hbmnukemeka.HbmNukeMeka;
import com.chunky.hbmnukemeka.net.Net;
import com.chunky.hbmnukemeka.net.TorexPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side: Continuum's torex entity is only tracked by clients within a limited range, so a player far
 * away (where Distant Horizons draws) never receives it. This tracker remembers every torex on the server and
 * sends its state to all players of the dimension twice per second. No Continuum classes are referenced at
 * compile time; scale, max age and type are read reflectively.
 */
public final class TorexServerTracker {
    private static final ResourceLocation TOREX_ID = new ResourceLocation("hbm", "entity_effect_torex");
    private static final int SEND_INTERVAL = 10;

    private final Map<Level, List<Entity>> tracked = new ConcurrentHashMap<>();

    private static Method getScale;
    private static Method getMaxAge;
    private static Method getTorexType;
    private static boolean reflectionTried;

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (!TOREX_ID.equals(key)) return;
        tracked.computeIfAbsent(event.getLevel(), l -> new ArrayList<>()).add(event.getEntity());
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<Entity> list = tracked.get(level);
        if (list == null || list.isEmpty()) return;
        list.removeIf(Entity::isRemoved);
        if (list.isEmpty()) {
            sendClear(level);
            tracked.remove(level);
            return;
        }
        if (level.getGameTime() % SEND_INTERVAL != 0) return;

        List<TorexPacket.Entry> entries = new ArrayList<>(list.size());
        for (Entity e : list) {
            entries.add(new TorexPacket.Entry(e.getId(), e.getX(), e.getY(), e.getZ(),
                    readScale(e), e.tickCount, readMaxAge(e), readType(e)));
        }
        Net.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new TorexPacket(entries));
    }

    private void sendClear(ServerLevel level) {
        Net.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new TorexPacket(List.of()));
    }

    private static void initReflection(Entity e) {
        if (reflectionTried) return;
        reflectionTried = true;
        try {
            Class<?> c = e.getClass();
            getScale = c.getMethod("getScale");
            getMaxAge = c.getMethod("getMaxAge");
            getTorexType = c.getMethod("getTorexType");
        } catch (ReflectiveOperationException ex) {
            HbmNukeMeka.LOGGER.warn("Could not bind Continuum torex accessors, using defaults", ex);
        }
    }

    private static float readScale(Entity e) {
        initReflection(e);
        try {
            if (getScale != null) return ((Number) getScale.invoke(e)).floatValue();
        } catch (ReflectiveOperationException ignored) {}
        return 1.0F;
    }

    private static int readMaxAge(Entity e) {
        initReflection(e);
        try {
            if (getMaxAge != null) return ((Number) getMaxAge.invoke(e)).intValue();
        } catch (ReflectiveOperationException ignored) {}
        return 900;
    }

    private static byte readType(Entity e) {
        initReflection(e);
        try {
            if (getTorexType != null) return ((Number) getTorexType.invoke(e)).byteValue();
        } catch (ReflectiveOperationException ignored) {}
        return 0;
    }
}
