package me.enderkill98.proxlib;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProxPackets {

    public static int[] PACKET_PDU_MAGIC = new int[]{ProxDataUnits.getMaxProxDataUnit() - 1, ProxDataUnits.getMaxProxDataUnit() - 19};

    // This is still missing the MAGIC, which can't be encoded here
    private static byte[] encodeProxPacket(ProxPacketIdentifier identifier, byte[] data) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int length = 2 /*Id*/ + data.length;

        // Write length
        bout.write((byte) ((length >> 16) & 0xFF));
        bout.write((byte) ((length >> 8) & 0xFF));
        bout.write((byte) (length & 0xFF));

        // Write id
        int vendorAndPacketId = identifier.pack();
        bout.write((byte) ((vendorAndPacketId >> 8) & 0xFF));
        bout.write((byte) (vendorAndPacketId & 0xFF));

        // Write data
        try {
            bout.write(data);
        } catch (IOException ex) {
            return null; // Should never happen tbh
        }

        return bout.toByteArray();
    }

    public static List<Integer> fullyEncodeProxPacketToProxDataUnits(ProxPacketIdentifier identifier, byte[] data) {
        ArrayList<Integer> pdus = new ArrayList<>();
        for (int pdu : PACKET_PDU_MAGIC)
            pdus.add(pdu);

        byte[] encoded = encodeProxPacket(identifier, data);
        pdus.addAll(ProxDataUnits.bytesToProxDataUnits(encoded));
        return pdus;
    }

}
