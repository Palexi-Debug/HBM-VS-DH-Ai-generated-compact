package com.chunky.hbmnukemeka.net;

import com.chunky.hbmnukemeka.HbmNukeMeka;
import com.chunky.hbmnukemeka.client.FarTorexClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

import net.minecraftforge.network.NetworkDirection;

public final class Net {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HbmNukeMeka.MOD_ID, "main"),
            () -> PROTOCOL,
            // the addon is optional on the client: accept a missing channel as well
            v -> true,
            v -> true);

    private Net() {}

    public static void init() {
        CHANNEL.registerMessage(0, TorexPacket.class, TorexPacket::encode, TorexPacket::decode,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> FarTorexClient.onPacket(msg));
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
