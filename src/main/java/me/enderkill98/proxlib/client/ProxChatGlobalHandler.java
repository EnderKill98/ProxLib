package me.enderkill98.proxlib.client;

import net.minecraft.entity.player.PlayerEntity;

public interface ProxChatGlobalHandler {
    void onPacketReceived(PlayerEntity sender, short id, byte[] data);
}
