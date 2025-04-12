package me.enderkill98.proxlib;

public interface ProxPacketReceiveHandler {
    void onReceived(ProxPacketIdentifier identifier, byte[] data);
}
