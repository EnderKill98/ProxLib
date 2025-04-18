package me.enderkill98.proxlib;

import net.minecraft.entity.player.PlayerEntity;

public interface ProxPacketReceiveHandler {
    void onReceived(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data);
}
