package com.chunky.hbmnukemeka;

import com.chunky.hbmnukemeka.client.FarTorexClient;
import com.chunky.hbmnukemeka.net.Net;
import com.chunky.hbmnukemeka.server.TorexServerTracker;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Addon for HBM's Nuclear Tech Mod: Continuum. Adds Mekanism Combiner recipes for the Continuum nukes
 * (the recipes are plain data, see data/hbmnukemeka/recipes) plus four intermediate items, and draws the
 * explosion cloud in Distant Horizons LODs.
 */
@Mod(HbmNukeMeka.MOD_ID)
public final class HbmNukeMeka {
    public static final String MOD_ID = "hbmnukemeka";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> BOMB_CASING = simple("bomb_casing");
    public static final RegistryObject<Item> FISSILE_CORE = simple("fissile_core");
    public static final RegistryObject<Item> THERMONUCLEAR_STAGE = simple("thermonuclear_stage");
    public static final RegistryObject<Item> EXOTIC_CORE = simple("exotic_core");

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public HbmNukeMeka() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToTabs);

        // Server side: watch Continuum's torex (mushroom cloud) entities and tell every player about them.
        MinecraftForge.EVENT_BUS.register(new TorexServerTracker());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            FarTorexClient.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::init);
    }

    private void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(BOMB_CASING);
            event.accept(FISSILE_CORE);
            event.accept(THERMONUCLEAR_STAGE);
            event.accept(EXOTIC_CORE);
        }
    }
}
