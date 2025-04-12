package me.enderkill98.proxlib.client;

import me.enderkill98.proxlib.ProxPacketIdentifier;
import net.minecraft.entity.player.PlayerEntity;

public interface ProxChatGlobalHandler {
    void onPacketReceived(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data);
}
