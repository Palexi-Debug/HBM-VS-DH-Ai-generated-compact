package com.chunky.hbmnukemeka.net;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the current state of every active Continuum torex in the player's dimension. */
public final class TorexPacket {

    public record Entry(int entityId, double x, double y, double z, float scale, int age, int maxAge, byte type) {}

    public final List<Entry> entries;

    public TorexPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void encode(TorexPacket p, FriendlyByteBuf buf) {
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

    public static TorexPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readByte()));
        }
        return new TorexPacket(list);
    }
}
