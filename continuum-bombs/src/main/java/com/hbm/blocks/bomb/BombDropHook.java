package com.hbm.blocks.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Lets the shared rack block call the platform's drop code without referencing it. */
public final class BombDropHook {

    public interface Handler {
        /** Releases the bomb hanging under the rack; true if one was released. */
        boolean release(Level level, BlockPos rackPos);
    }

    public static volatile Handler handler;

    private BombDropHook() {}

    public static boolean release(Level level, BlockPos rackPos) {
        Handler h = handler;
        return h != null && h.release(level, rackPos);
    }
}
