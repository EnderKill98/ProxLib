package me.enderkill98.proxlib;

import net.minecraft.core.BlockPos;

/**
 * Helpers for porting from Yarn to Mojang(s mappings).
 *
 * Short for "Yarn is better". Otherwise this helper class wouldn't exist.
 */
public class Yib {

    public static BlockPos blockPosAdd(BlockPos a, BlockPos b) {
        return new BlockPos(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

}
