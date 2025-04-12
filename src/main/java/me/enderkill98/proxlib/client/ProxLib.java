package me.enderkill98.proxlib.client;

import me.enderkill98.proxlib.ProxDataUnits;
import me.enderkill98.proxlib.ProxPacketIdentifier;
import me.enderkill98.proxlib.ProxPackets;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class ProxLib implements ClientModInitializer {

    @Override public void onInitializeClient() {}

    private static final ArrayList<ProxChatGlobalHandler> REGISTERED_GLOBAL_HANDLERS = new ArrayList<>();

    /**
     * Add your own handler, which ProxChat will automatically call for all received packets
     */
    public static boolean addGlobalHandler(ProxChatGlobalHandler handler) {
        if(REGISTERED_GLOBAL_HANDLERS.contains(handler)) return false;
        REGISTERED_GLOBAL_HANDLERS.add(handler);
        return true;
    }

    public static boolean removeGlobalHandler(ProxChatGlobalHandler handler) {
        return REGISTERED_GLOBAL_HANDLERS.remove(handler);
    }

    public static int sendPacket(MinecraftClient client, ProxPacketIdentifier identifier, byte[] data) {
        return sendPacket(client, identifier, data, false);
    }

    public static List<ProxChatGlobalHandler> getGlobalHandlers() {
        return REGISTERED_GLOBAL_HANDLERS;
    }

    /**
     * Helper-function to easier send out packets.
     * @param identifier Packet Identifier
     * @param data Packet Data
     * @param dryRun Do not send any actual data
     * @return The amount of Minecraft-Packets produced for sending this packet
     */
    public static int sendPacket(MinecraftClient client, ProxPacketIdentifier identifier, byte[] data, boolean dryRun) {
        int packets = 0;
        for(int pdu : ProxPackets.fullyEncodeProxPacketToProxDataUnits(identifier, data)) {
            packets++;
            if(!dryRun) {
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
            }
        }
        return packets;
    }

}
