package com.hbm.platform.forge.dh;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client: the state of every active nuke torex in the player's dimension (for Distant Horizons). */
public final class DhTorexPacket {

    public record Entry(int entityId, double x, double y, double z, float scale, int age, int maxAge, byte type) {}

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
        }
    }

    public static DhTorexPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readByte()));
        }
        return new DhTorexPacket(list);
    }
}
