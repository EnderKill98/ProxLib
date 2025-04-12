package me.enderkill98.proxlib;

import java.io.ByteArrayOutputStream;

public class ProxDataUnitReader {
    private static final int HIGHEST_IN_BIT = ProxDataUnits.getStorableBitCount();
    private static final int HIGHEST_OUT_BIT = 8;

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private byte currentByte = 0;
    private int currentBytePos = 0;

    public void read(int proxDataUnit) {
        for (int i = 0; i < HIGHEST_IN_BIT; i++) {
            int inputBit = (proxDataUnit >>> i) & 0x01; // 0 or 1

            currentByte |= (byte) (inputBit << currentBytePos);
            currentBytePos++;
            if (currentBytePos == HIGHEST_OUT_BIT) {
                outputStream.write(currentByte);
                currentByte = 0;
                currentBytePos = 0;
            }
        }
    }

    public int getTotalBytes() {
        return outputStream.size();
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}
