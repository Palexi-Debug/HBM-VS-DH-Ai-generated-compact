package com.chunky.hbmnukemeka.drop;

import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client side of the drop system; only loaded on the physical client. */
public final class DropSystemClient {

    private DropSystemClient() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DropSystem.TYPE.get(), DroppedBombRenderer::new);
    }
}
