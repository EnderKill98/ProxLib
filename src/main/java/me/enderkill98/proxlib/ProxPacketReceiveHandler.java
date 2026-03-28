package me.enderkill98.proxlib;

import net.minecraft.world.entity.player.Player;

public interface ProxPacketReceiveHandler {
    void onReceived(Player sender, ProxPacketIdentifier identifier, byte[] data);
}
