package com.chunky.hbmnukemeka.drop;

import com.chunky.hbmnukemeka.HbmNukeMeka;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Drop system for the Continuum bombs: the rack block, the falling-bomb entity and the release logic. Continuum is
 * only touched by reflection ({@code IBomb}, {@code TileEntityNukeBomb}), and Valkyrien Skies is optional: on a ship
 * the bomb starts at the world position of the rack with the ship's velocity at that point.
 */
public final class DropSystem {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HbmNukeMeka.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HbmNukeMeka.MOD_ID);

    public static final RegistryObject<Block> RACK = BLOCKS.register("bomb_drop_rack",
            () -> new BombRackBlock(BlockBehaviour.Properties.of().strength(5.0F, 60.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Item> RACK_ITEM = HbmNukeMeka.ITEMS.register("bomb_drop_rack",
            () -> new BlockItem(RACK.get(), new Item.Properties()));
    public static final RegistryObject<EntityType<DroppedBomb>> TYPE = ENTITIES.register("dropped_bomb",
            () -> EntityType.Builder.<DroppedBomb>of(DroppedBomb::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).fireImmune().clientTrackingRange(10).updateInterval(1)
                    .build("dropped_bomb"));

    private static Class<?> iBomb;
    private static Method explodeMethod;
    private static boolean resolved;

    private DropSystem() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
    }

    private static synchronized boolean resolve() {
        if (resolved) return iBomb != null;
        resolved = true;
        try {
            iBomb = Class.forName("com.hbm.interfaces.IBomb");
            explodeMethod = iBomb.getMethod("explode", Level.class, BlockPos.class, Entity.class);
        } catch (ReflectiveOperationException e) {
            iBomb = null;
            HbmNukeMeka.LOGGER.warn("Drop system: Continuum's IBomb not found, bombs cannot be dropped ({})", e.toString());
        }
        return iBomb != null;
    }

    public static boolean isBomb(BlockState state) {
        return resolve() && iBomb.isInstance(state.getBlock());
    }

    public static void explode(Level level, BlockPos pos) {
        if (!resolve()) return;
        try {
            explodeMethod.invoke(level.getBlockState(pos).getBlock(), level, pos, null);
        } catch (ReflectiveOperationException e) {
            HbmNukeMeka.LOGGER.warn("Drop system: could not detonate the landed bomb: {}", e.toString());
        }
    }

    static void release(Level level, BlockPos rackPos) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos bombPos = rackPos.below();
        BlockState state = level.getBlockState(bombPos);
        BlockEntity be = level.getBlockEntity(bombPos);
        if (be == null || !isBomb(state)) return;
        CompoundTag data = be.saveWithoutMetadata();
        try {
            be.getClass().getMethod("clearSlots").invoke(be); // the parts travel with the entity, they must not drop
        } catch (ReflectiveOperationException ignored) {
            // older Continuum: no such method
        }
        level.removeBlock(bombPos, false);

        Vec3 centre = Vec3.atBottomCenterOf(bombPos);
        Vec3 velocity = Vec3.ZERO;
        Vec3[] ship = shipTransform(server, bombPos, centre);
        if (ship != null) {
            centre = ship[0];
            velocity = ship[1];
        }
        DroppedBomb bomb = TYPE.get().create(server);
        if (bomb == null) return;
        bomb.setBomb(state, data);
        bomb.setPos(centre.x, centre.y, centre.z);
        bomb.launch(velocity);
        server.addFreshEntity(bomb);
        HbmNukeMeka.LOGGER.info("Bomb released from rack {} at {} with velocity {}", rackPos, centre, velocity);
    }

    private static boolean vsFailed;

    /** Valkyrien Skies: {world position, world velocity in blocks per tick} of a block on a ship, else null. */
    private static Vec3[] shipTransform(ServerLevel level, BlockPos pos, Vec3 centre) {
        if (vsFailed) return null;
        Class<?> utils;
        try {
            utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
        } catch (ClassNotFoundException e) {
            vsFailed = true;
            return null;
        }
        try {
            Object ship = utils.getMethod("getShipManagingPos", Level.class, BlockPos.class).invoke(null, level, pos);
            if (ship == null) return null;
            Vec3 world = (Vec3) utils.getMethod("toWorldCoordinates", Level.class, Vec3.class).invoke(null, level, centre);
            double[] v = vector(call(ship, "getVelocity"));
            Vec3 velocity = new Vec3(v[0], v[1], v[2]);
            try {
                double[] omega = vector(call(ship, "getOmega"));
                double[] c = vector(call(call(ship, "getTransform"), "getPositionInWorld"));
                Vec3 r = world.subtract(c[0], c[1], c[2]);
                velocity = velocity.add(omega[1] * r.z - omega[2] * r.y, omega[2] * r.x - omega[0] * r.z,
                        omega[0] * r.y - omega[1] * r.x);
            } catch (ReflectiveOperationException | RuntimeException e) {
                HbmNukeMeka.LOGGER.info("Valkyrien Skies: no angular velocity ({})", e.toString());
            }
            return new Vec3[] {world, velocity.scale(1.0D / 20.0D)};
        } catch (Throwable t) {
            vsFailed = true;
            HbmNukeMeka.LOGGER.warn("Valkyrien Skies bridge failed, bombs drop straight down: {}", t.toString());
            return null;
        }
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        Method m = target.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static double[] vector(Object v) throws ReflectiveOperationException {
        return new double[] {(double) call(v, "x"), (double) call(v, "y"), (double) call(v, "z")};
    }
}
