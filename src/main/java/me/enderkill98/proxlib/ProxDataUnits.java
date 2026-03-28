package me.enderkill98.proxlib;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ProxDataUnits {

    private static BlockPos[] createAllOffsets() {
        // All tested offsets have to be in reach of all these positions
        final ArrayList<Vec3> originPositions = new ArrayList<>();
        for (int xOffset : new int[]{0, 1})
            for (int yOffset : new int[]{0, 1})
                for (int zOffset : new int[]{0, 1})
                    originPositions.add(new Vec3(xOffset, yOffset, zOffset));

        // Find all offsets that can be interacted with
        final ArrayList<BlockPos> offsets = new ArrayList<>();
        final int[] anyAxisOffsets = new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6};
        for (int x : anyAxisOffsets) {
            for (int y : anyAxisOffsets) {
                offsetLoop:
                for (int z : anyAxisOffsets) {
                    BlockPos offset = new BlockPos(x, y, z);
                    Vec3 offsetCenter = Vec3.atCenterOf(offset);
                    for (Vec3 originPos : originPositions)
                        if (originPos.distanceToSqr(offsetCenter) > 6.0 * 6.0)
                            continue offsetLoop; // Too far to interact with

                    offsets.add(offset);
                }
            }
        }

        return offsets.toArray(new BlockPos[0]);
    }

    public static final BlockPos[] ALL_OFFSETS = createAllOffsets();

    private static HashMap<BlockPos, Integer> createOffsetLookupMap() {
        HashMap<BlockPos, Integer> lookupMap = new HashMap<>();
        for (int offsetIndex = 0; offsetIndex < ALL_OFFSETS.length; offsetIndex++) {
            lookupMap.put(ALL_OFFSETS[offsetIndex], offsetIndex);
        }
        return lookupMap;
    }

    public static final HashMap<BlockPos, Integer> ALL_OFFSETS_LOOKUP_MAP = createOffsetLookupMap();

    public static int getStorableBitCount() {
        if (ALL_OFFSETS.length == 0) return 0;
        return (int) Math.floor(Math.log(ALL_OFFSETS.length) / Math.log(2));
    }

    // Value is exclusive
    public static int getMaxUsableProxDataUnit() {
        return (int) Math.pow(2, getStorableBitCount());
    }

    // Value is exclusive
    public static int getMaxProxDataUnit() {
        return ALL_OFFSETS.length;
    }

    public static List<Integer> bytesToProxDataUnits(byte[] input) {
        if (input.length == 0) return Collections.emptyList();

        ArrayList<Integer> output = new ArrayList<>();
        int highestBit = getStorableBitCount();

        int currentOffsetIndex = 0;
        int currentOffsetIndexPos = 0;
        for (byte inputByte : input) {
            for (int i = 0; i < 8; i++) {
                int inputBit = (inputByte >>> i) & 0x01; // 0 or 1

                currentOffsetIndex |= (inputBit << currentOffsetIndexPos);
                currentOffsetIndexPos++;
                if (currentOffsetIndexPos == highestBit) {
                    output.add(currentOffsetIndex);
                    currentOffsetIndex = 0;
                    currentOffsetIndexPos = 0;
                }
            }
        }
        if (currentOffsetIndexPos > 0)
            output.add(currentOffsetIndex);

        return output;
    }

    public static byte[] proxDataUnitsToBytes(int... inputProxDataUnits) {
        ProxDataUnitReader reader = new ProxDataUnitReader();
        for (int proxDataUnit : inputProxDataUnits)
            reader.read(proxDataUnit);
        return reader.getBytes();
    }

    public static BlockPos proxDataUnitToBlockPos(Player player, int offsetIndex) {
        final Vec3 eyePos = player.getEyePosition();
        final BlockPos eyeBlockPos = new BlockPos(Mth.floor(eyePos.x()), Mth.floor(eyePos.y()), Mth.floor(eyePos.z()));
        return Yib.blockPosAdd(eyeBlockPos, ALL_OFFSETS[offsetIndex]);
    }

    public static int blockPosToProxDataUnit(Player player, BlockPos blockPos) {
        final Vec3 eyePos = player.getEyePosition();
        final BlockPos eyeBlockPos = new BlockPos(Mth.floor(eyePos.x()), Mth.floor(eyePos.y()), Mth.floor(eyePos.z()));

        BlockPos offset = blockPos.subtract(eyeBlockPos);
        return ALL_OFFSETS_LOOKUP_MAP.getOrDefault(offset, -1);
    }

    public static int blockPosToProxDataUnit(BlockPos eyeBlockPos, BlockPos blockPos) {
        BlockPos offset = blockPos.subtract(eyeBlockPos);
        return ALL_OFFSETS_LOOKUP_MAP.getOrDefault(offset, -1);
    }

}
