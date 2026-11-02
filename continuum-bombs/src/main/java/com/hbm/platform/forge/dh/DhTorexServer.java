package com.hbm.platform.forge.dh;

import com.hbm.entity.effect.EntityNukeTorex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server side: a torex is only tracked by clients within a limited range, so a player far away (where Distant
 * Horizons draws) would never receive it. This remembers every torex of a dimension and sends its state to all
 * players of the dimension twice per second.
 */
public final class DhTorexServer {

    private static final int SEND_INTERVAL = 10;
    private final Map<Level, List<EntityNukeTorex>> tracked = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityNukeTorex torex)) return;
        tracked.computeIfAbsent(event.getLevel(), l -> new ArrayList<>()).add(torex);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<EntityNukeTorex> list = tracked.get(level);
        if (list == null || list.isEmpty()) return;
        list.removeIf(EntityNukeTorex::isRemoved);
        if (list.isEmpty()) {
            tracked.remove(level);
            DhTorexNet.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new DhTorexPacket(List.of()));
            return;
        }
        if (level.getGameTime() % SEND_INTERVAL != 0) return;
        List<DhTorexPacket.Entry> entries = new ArrayList<>(list.size());
        for (EntityNukeTorex e : list) {
            var d = e.getEntityData();
            entries.add(new DhTorexPacket.Entry(e.getId(), e.getX(), e.getY(), e.getZ(),
                    d.get(EntityNukeTorex.SCALE), e.tickCount, e.getMaxAge(), d.get(EntityNukeTorex.TYPE),
                    d.get(EntityNukeTorex.RANDOM_SEED_LOW), d.get(EntityNukeTorex.RANDOM_SEED_HIGH),
                    d.get(EntityNukeTorex.START_TIME_LOW), d.get(EntityNukeTorex.START_TIME_HIGH),
                    d.get(EntityNukeTorex.INITIAL_SPAWN_TARGET), d.get(EntityNukeTorex.FINAL_SPAWN_TARGET),
                    d.get(EntityNukeTorex.TERRAIN_CHANGE_AGE), d.get(EntityNukeTorex.HUMIDITY),
                    d.get(EntityNukeTorex.VACUUM)));
        }
        DhTorexNet.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new DhTorexPacket(entries));
    }
}
