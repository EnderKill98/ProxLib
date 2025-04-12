package me.enderkill98.proxlib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProxPackets {

    public static int[] PACKET_PDU_MAGIC = new int[]{ProxDataUnits.getMaxProxDataUnit() - 1, ProxDataUnits.getMaxProxDataUnit() - 19};

    // This is still missing the MAGIC, which can't be encoded here
    public static byte[] encodeProxPacket(short id, byte[] data) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int length = 2 /*Id*/ + data.length;

        // Write length
        bout.write((byte) ((length >> 16) & 0xFF));
        bout.write((byte) ((length >> 8) & 0xFF));
        bout.write((byte) (length & 0xFF));

        // Write id
        bout.write((byte) ((id >> 8) & 0xFF));
        bout.write((byte) (id & 0xFF));

        // Write data
        try {
            bout.write(data);
        } catch (IOException ex) {
            return null; // Should never happen tbh
        }

        return bout.toByteArray();
    }

    public static List<Integer> fullyEncodeProxPacketToProxDataUnits(short id, byte[] data) {
        ArrayList<Integer> pdus = new ArrayList<>();
        for (int pdu : PACKET_PDU_MAGIC)
            pdus.add(pdu);

        byte[] encoded = encodeProxPacket(id, data);
        pdus.addAll(ProxDataUnits.bytesToProxDataUnits(encoded));
        return pdus;
    }

}
