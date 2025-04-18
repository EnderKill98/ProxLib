package me.enderkill98.proxlib;

public record ProxPacketIdentifier(short vendorId, byte packetId) {

    public static int VENDOR_ID_BITS = 10;
    public static int PACKET_ID_BITS = 6;

    /**
     * Max, exclusive value for vendor id
     * Currently, this should be 1024 (so any value between 0 and 1023 is valid)
     */
    public static int getMaxVendorIdValue() {
        return 1 << VENDOR_ID_BITS;
    }

    /**
     * Max, exclusive value for packet id
     * Currently, this should be 64 (so any value between 0 and 63 is valid)
     */
    public static int getMaxPacketIdValue() {
        return 1 << PACKET_ID_BITS;
    }

    public static ProxPacketIdentifier ofPacked(short vendorAndPacketId) {
        short vendorId = (short) (Short.toUnsignedInt(vendorAndPacketId) >>> PACKET_ID_BITS);
        byte packetId = (byte) (vendorAndPacketId & ((1 << PACKET_ID_BITS) - 1));
        return new ProxPacketIdentifier(vendorId, packetId);
    }

    public static ProxPacketIdentifier of(int vendorId, int packetId) {
        ProxPacketIdentifier instance = new ProxPacketIdentifier((short) vendorId, (byte) packetId);
        instance.validate();
        return instance;
    }

    public void validate() {
        if(vendorId < 0 || vendorId >= 1 << VENDOR_ID_BITS)
            throw new RuntimeException("VendorId cannot be represented as a " + VENDOR_ID_BITS + "-bit value (unsigned)");
        if(packetId < 0 || packetId >= 1 << PACKET_ID_BITS)
            throw new RuntimeException("PacketId cannot be represented as a " + PACKET_ID_BITS + "-bit value (unsigned)");
    }

    public short pack() {
        validate();
        return (short) ((vendorId << PACKET_ID_BITS) | packetId);
    }

    @Override
    public int hashCode() {
        return pack();
    }
}