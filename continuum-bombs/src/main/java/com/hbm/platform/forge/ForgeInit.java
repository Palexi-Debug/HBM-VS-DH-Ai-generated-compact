package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardSystem;
import com.hbm.util.TooltipColours;
import com.hbm.packet.AnvilCraftPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.handler.radiation.RadiationSystemNT;
import dev.golem.core.registry.RegistryObject;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/^* Forge 1.20.1 bootstrap; client code is only referenced after the physical-side check. ^/
@Mod(com.hbm.Constants.MOD_ID)
public final class ForgeInit {

    public ForgeInit() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        dev.golem.core.platform.ModEntries.start(new com.hbm.HbmEntry());

        modBus.addListener(this::onBuildTabContents);

        // Forge 1.20.1 has no furnace-fuel data map: the burn times answer FurnaceFuelBurnTimeEvent (lazily
        // resolved, so registering before the registries flush is fine).
        dev.golem.core.registry.Fuels.register(com.hbm.items.ModFuelItems.BURN_TIMES);

        // Distant Horizons: the nuke torex is sent to every player of the dimension and drawn through DH's renderer
        com.hbm.platform.forge.dh.DhTorexNet.register();
        MinecraftForge.EVENT_BUS.register(new com.hbm.platform.forge.dh.DhTorexServer());
        // Drop system: the falling bomb entity and the rack's release hook (Valkyrien Skies is optional)
        com.hbm.platform.forge.drop.DropBombs.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientInit.init(modBus);
        }

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer());
            }
            if (event.phase == TickEvent.Phase.END) {
                RadiationSystemNT.tickServer(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                RadiationSystemNT.closeWorld(level);
                com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkLoaded(level, chunk);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkUnloaded(level, chunk);
            }
        });
        // FORK (bombs only): every crafting recipe of HBM is dropped after the recipe reload; the bombs are made with
        // Mekanism recipes only. Runs as a reload listener added after the vanilla ones, so the RecipeManager is filled.
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent event) -> {
            var recipes = event.getServerResources().getRecipeManager();
            event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
                var kept = new java.util.ArrayList<net.minecraft.world.item.crafting.Recipe<?>>();
                int removed = 0;
                for (var r : recipes.getRecipes()) {
                    if (com.hbm.Constants.MOD_ID.equals(r.getId().getNamespace())) removed++;
                    else kept.add(r);
                }
                recipes.replaceRecipes(kept);
                com.hbm.Constants.LOG.info("[HBM] Removed {} HBM crafting recipes, Mekanism recipes only", removed);
            });
        });
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            TooltipColours.apply(event.getItemStack(), event.getToolTip());
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity());
            com.hbm.handler.pollution.PollutionHandler.decorateMob(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            com.hbm.Constants.LOG.info("[HBM] Loaded {} total recipes",
                    event.getServer().getRecipeManager().getRecipes().size());
            auditWorldgen(event.getServer());
        });
    }

    private void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        ModTabs.accept(event.getTabKey(), event::accept);
    }

    private static void auditWorldgen(MinecraftServer server) {
        Set<Identifier> features = new TreeSet<>();
        server.registryAccess().registryOrThrow(Registries.BIOME).holders().forEach(biome ->
                biome.value().getGenerationSettings().features().forEach(step -> step.forEach(feature ->
                        feature.unwrapKey().map(ResourceKey::location)
                                .filter(id -> id.getNamespace().equals(com.hbm.Constants.MOD_ID))
                                .ifPresent(features::add))));
        // every hbm placed feature named by a loaded forge:add_features modifier must reach a biome
        Set<Identifier> expected = new TreeSet<>();
        server.registryAccess().registryOrThrow(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS)
                .forEach(modifier -> {
                    if (modifier instanceof net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier add) {
                        add.features().forEach(feature -> feature.unwrapKey().map(ResourceKey::location)
                                .filter(id -> id.getNamespace().equals(com.hbm.Constants.MOD_ID))
                                .ifPresent(expected::add));
                    }
                });
        Set<Identifier> missing = new TreeSet<>(expected);
        missing.removeAll(features);
        if (missing.isEmpty()) {
            com.hbm.Constants.LOG.info("[HBM] Forge biome modifiers injected all {} modifier features ({} hbm placed features in biomes): {}",
                    expected.size(), features.size(), features);
        } else {
            com.hbm.Constants.LOG.error("[HBM] Forge biome modifiers injected {}/{} modifier features; missing {}",
                    expected.size() - missing.size(), expected.size(), missing);
        }
    }
}
*///?}
