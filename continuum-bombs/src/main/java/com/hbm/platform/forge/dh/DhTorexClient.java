package com.hbm.platform.forge.dh;

import com.hbm.platform.forge.dh.DhTorexPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client side bookkeeping for far away torex clouds. The state arrives from the server in
 * {@link DhTorexPacket}s; the Distant Horizons part lives in {@link DhTorexRenderer} and is only touched when
 * Distant Horizons is installed, so the mod also runs without it.
 */
public final class DhTorexClient {

    /** Last known state of one torex plus when it was received (client game time). */
    static final class State {
        DhTorexPacket.Entry entry;
        long receivedAt;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static boolean dhPresent;
    private static boolean dhBroken;
    private static ClientLevel lastLevel;

    private DhTorexClient() {}

    public static void init() {
        dhPresent = ModList.get().isLoaded("distanthorizons");
        MinecraftForge.EVENT_BUS.register(new DhTorexClient.Ticker());
        com.hbm.Constants.LOG.info("Distant Horizons {}", dhPresent ? "found, far torex rendering on" : "not installed");
    }

    public static void onPacket(DhTorexPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (!packet.entries.isEmpty() && STATES.isEmpty()) com.hbm.Constants.LOG.info("DH torex: first packet, {} cloud(s)", packet.entries.size());
        long now = level.getGameTime();
        Map<Integer, State> next = new HashMap<>();
        for (DhTorexPacket.Entry e : packet.entries) {
            State s = STATES.get(e.entityId());
            if (s == null) {
                s = new State();
                farFlash(level, e);
            }
            s.entry = e;
            s.receivedAt = now;
            next.put(e.entityId(), s);
        }
        // anything the server no longer lists is gone
        for (Iterator<Map.Entry<Integer, State>> it = STATES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, State> old = it.next();
            if (!next.containsKey(old.getKey()) && dhPresent && !dhBroken) {
                safe(() -> DhTorexRenderer.remove(old.getKey()));
            }
        }
        STATES.clear();
        STATES.putAll(next);
    }

    /** Maximum distance in blocks at which a far blast still whites out the screen (weaker with distance). */
    private static final double FLASH_RANGE = 4000.0D;

    /** The blinding white flash for a blast the client does not track itself (it is beyond the entity range). */
    private static void farFlash(ClientLevel level, DhTorexPacket.Entry e) {
        if (e.age() > 40 || level.getEntity(e.entityId()) != null) return; // old cloud, or the real effect handles it
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        double dist = Math.sqrt(player.distanceToSqr(e.x(), e.y(), e.z()));
        if (dist >= FLASH_RANGE) return;
        com.hbm.client.NukeScreenEffects.flashStrength = (float) Math.max(0.1D, 1.0D - dist / FLASH_RANGE);
        com.hbm.client.NukeScreenEffects.flashTimestamp = System.currentTimeMillis();
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            // a Distant Horizons API mismatch must never crash the game
            dhBroken = true;
            com.hbm.Constants.LOG.error("Distant Horizons far torex rendering disabled after an error", t);
        }
    }

    public static final class Ticker {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level != lastLevel) {
                // dimension change or disconnect: forget everything
                if (dhPresent && !dhBroken) safe(DhTorexRenderer::clear);
                STATES.clear();
                lastLevel = level;
            }
            if (level == null || !dhPresent || dhBroken) return;
            // refresh the geometry once per second, the packets arrive every half second
            if (level.getGameTime() % 20 != 0) return;
            final long now = level.getGameTime();
            for (State s : STATES.values()) {
                final DhTorexPacket.Entry e = s.entry;
                // the real entity renders while the client tracks it, Distant Horizons only draws the far ones
                final boolean nearAndRendered = level.getEntity(e.entityId()) != null;
                final int age = e.age() + (int) (now - s.receivedAt);
                safe(() -> DhTorexRenderer.update(level, e, age, nearAndRendered));
            }
        }
    }
}
