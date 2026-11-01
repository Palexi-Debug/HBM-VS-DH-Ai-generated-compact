package com.hbm.platform.forge.dh;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The one packet channel of the Distant Horizons torex sync. */
public final class DhTorexNet {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("hbm", "dh_torex"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private DhTorexNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, DhTorexPacket.class, DhTorexPacket::encode, DhTorexPacket::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DhTorexClient.onPacket(packet)));
            ctx.get().setPacketHandled(true);
        });
    }
}
