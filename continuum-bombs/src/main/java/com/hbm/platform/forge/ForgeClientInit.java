package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.client.HbmClientRegistrations;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmPackets;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/^* Client bootstrap, isolated from {@link ForgeInit} so dedicated servers never load client classes. ^/
final class ForgeClientInit {

    private ForgeClientInit() {}

    static void init(IEventBus modBus) {
        HbmPackets.registerClientHandlers();
        com.hbm.platform.forge.dh.DhTorexClient.init(); // Distant Horizons far torex (no-op without DH)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(
                com.hbm.platform.forge.drop.DropBombs.TYPE.get(), com.hbm.platform.forge.drop.DroppedBombRenderer::new));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.handler.JetpackHandler.clearClientState());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.client.gun.GunSmokeClient.onDisconnect());
        com.hbm.client.particle.HbmParticleHandlers.register();
        com.hbm.client.particle.SingularityParticleHandlers.register();
        GolemNetwork.setClientContextSupplier(() -> new GolemNetwork.ClientContext(
                Minecraft.getInstance().level, Minecraft.getInstance().player));
        GolemNetwork.flushClientRegistrations();

        com.hbm.client.ClientGameTime.install();
        // The material-shape sprites are computed onto the atlas, not shipped; this has to be declared
        // before the first resource load. The fluid droplets are the same, one per registered fluid.
        com.hbm.client.AutogenSprites.install();
        com.hbm.client.FluidSprites.install();
        HbmClientRegistrations.installDebugLines();
        HbmClientRegistrations.installTints();
        HbmClientRegistrations.installItemModelProperties();
        HbmClientRegistrations.installKeybinds();
        HbmClientRegistrations.installGunAnimations();
        HbmClientRegistrations.installEntityEffects();
        HbmClientRegistrations.installArmorLayers();
        dev.golem.core.render.ForgeItemRenderers.install(com.hbm.client.render.MachineItemRenderers::render);
        modBus.addListener((net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
                            com.hbm.client.render.MachineItemModels.reload(resources);
                        }));
        // FORK: hide every HBM key binding from the Controls screen (ParCool / clean controls).
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ForgeClientInit::hideHbmKeys));
        HbmClientRegistrations.registerBlockColors();
        HbmClientRegistrations.installScreensAndRenderers();
        // Core's Hud.draw owns the per-layer composition rule: on 1.20.1 each layer must start on a clear
        // depth buffer, or the chat's z-50 lines hide every HBM overlay drawn where they are (the
        // power-armour charge bars vanished that way while 26.1 drew them).
        modBus.addListener((RegisterGuiOverlaysEvent event) -> {
            HbmClientRegistrations.registerHud((id, renderer) ->
                    event.registerAboveAll(id, (gui, graphics, partialTick, width, height) ->
                            dev.golem.core.client.Hud.draw(graphics, renderer::accept)));
        });
    }

    /^* Removes all HBM key mappings from {@code Options.keyMappings} (found by type, so it survives obfuscation). ^/
    private static void hideHbmKeys() {
        try {
            net.minecraft.client.Options options = Minecraft.getInstance().options;
            for (java.lang.reflect.Field f : net.minecraft.client.Options.class.getDeclaredFields()) {
                if (f.getType() != net.minecraft.client.KeyMapping[].class) continue;
                f.setAccessible(true);
                net.minecraft.client.KeyMapping[] all = (net.minecraft.client.KeyMapping[]) f.get(options);
                net.minecraft.client.KeyMapping[] kept = java.util.Arrays.stream(all)
                        .filter(k -> !(k.getName().startsWith("key.hbm.") || k.getName().startsWith("key.jetpack_")))
                        .toArray(net.minecraft.client.KeyMapping[]::new);
                if (kept.length != all.length) f.set(options, kept);
            }
        } catch (Throwable t) {
            // purely cosmetic; leave the bindings visible if this loader refuses
        }
    }
}
*///?}
