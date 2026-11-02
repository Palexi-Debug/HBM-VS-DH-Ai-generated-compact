package com.hbm.platform.forge.drop;

import com.hbm.Constants;
import com.hbm.blocks.bomb.BombDropHook;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The drop system: registers the falling-bomb entity and implements {@link BombDropHook}. The bomb hanging under a
 * rack is turned into a {@link DroppedBomb}. On a Valkyrien Skies ship the bomb starts at the world position of the
 * rack and keeps the ship's velocity (read by reflection, so Valkyrien Skies stays optional).
 */
public final class DropBombs {

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.MOD_ID);

    public static final RegistryObject<EntityType<DroppedBomb>> TYPE = ENTITIES.register("dropped_bomb",
            () -> EntityType.Builder.<DroppedBomb>of(DroppedBomb::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).fireImmune().clientTrackingRange(10).updateInterval(1)
                    .build("dropped_bomb"));

    private DropBombs() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BombDropHook.handler = DropBombs::release;
    }

    private static boolean release(Level level, BlockPos rackPos) {
        if (!(level instanceof ServerLevel server)) return false;
        BlockPos bombPos = rackPos.below();
        BlockState state = level.getBlockState(bombPos);
        if (!(state.getBlock() instanceof IBomb) || !(level.getBlockEntity(bombPos) instanceof TileEntityNukeBomb be)) {
            return false;
        }
        var data = be.saveWithoutMetadata();
        be.clearSlots(); // the parts travel with the entity, they must not drop when the block is removed
        level.removeBlock(bombPos, false);

        Vec3 centre = Vec3.atBottomCenterOf(bombPos);
        Vec3 velocity = Vec3.ZERO;
        Vec3[] ship = shipTransform(server, bombPos, centre);
        if (ship != null) {
            centre = ship[0];
            velocity = ship[1];
        }
        DroppedBomb bomb = TYPE.get().create(server);
        if (bomb == null) return false;
        bomb.setBomb(state, data);
        bomb.setPos(centre.x, centre.y, centre.z);
        bomb.launch(velocity);
        server.addFreshEntity(bomb);
        Constants.LOG.info("Bomb released from rack {} at {} with velocity {}", rackPos, centre, velocity);
        return true;
    }

    private static boolean vsFailed;

    /**
     * Valkyrien Skies: {the world position of the block, its world velocity in blocks per tick}, or null when the
     * block is not on a ship (or Valkyrien Skies is not installed).
     */
    private static Vec3[] shipTransform(ServerLevel level, BlockPos pos, Vec3 centre) {
        if (vsFailed) return null;
        Class<?> utils;
        try {
            utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
        } catch (ClassNotFoundException e) {
            vsFailed = true; // not installed
            return null;
        }
        try {
            Object ship = utils.getMethod("getShipManagingPos", Level.class, BlockPos.class).invoke(null, level, pos);
            if (ship == null) return null;
            Vec3 world = (Vec3) utils.getMethod("toWorldCoordinates", Level.class, Vec3.class).invoke(null, level, centre);
            double[] v = vector(call(ship, "getVelocity"));
            Vec3 velocity = new Vec3(v[0], v[1], v[2]);
            try {
                // the point also moves with the ship's rotation: v + omega x r
                double[] omega = vector(call(ship, "getOmega"));
                Object transform = call(ship, "getTransform");
                double[] c = vector(call(transform, "getPositionInWorld"));
                Vec3 r = world.subtract(c[0], c[1], c[2]);
                velocity = velocity.add(omega[1] * r.z - omega[2] * r.y, omega[2] * r.x - omega[0] * r.z,
                        omega[0] * r.y - omega[1] * r.x);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Constants.LOG.info("Valkyrien Skies: no angular velocity ({}), using the ship's linear velocity", e.toString());
            }
            return new Vec3[] {world, velocity.scale(1.0D / 20.0D)}; // VS speaks blocks per second
        } catch (Throwable t) {
            vsFailed = true;
            Constants.LOG.warn("Valkyrien Skies bridge failed, bombs drop straight down: {}", t.toString());
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
