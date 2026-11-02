package com.hbm.platform.forge.dh;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client: the state of every active nuke torex in the player's dimension (for Distant Horizons). */
public final class DhTorexPacket {

    /** The timeline fields (seed .. vacuum) let a client rebuild the identical cloud animation for HIGH_PLUS. */
    public record Entry(int entityId, double x, double y, double z, float scale, int age, int maxAge, byte type,
                        int seedLow, int seedHigh, int startLow, int startHigh,
                        int initialTarget, int finalTarget, int terrainAge, float humidity, boolean vacuum) {}

    public final List<Entry> entries;

    public DhTorexPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void encode(DhTorexPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entries.size());
        for (Entry e : p.entries) {
            buf.writeVarInt(e.entityId);
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeFloat(e.scale);
            buf.writeVarInt(e.age);
            buf.writeVarInt(e.maxAge);
            buf.writeByte(e.type);
            buf.writeInt(e.seedLow);
            buf.writeInt(e.seedHigh);
            buf.writeInt(e.startLow);
            buf.writeInt(e.startHigh);
            buf.writeInt(e.initialTarget);
            buf.writeInt(e.finalTarget);
            buf.writeInt(e.terrainAge);
            buf.writeFloat(e.humidity);
            buf.writeBoolean(e.vacuum);
        }
    }

    public static DhTorexPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readByte(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readBoolean()));
        }
        return new DhTorexPacket(list);
    }
}
