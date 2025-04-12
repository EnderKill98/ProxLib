package me.enderkill98.proxlib;

public interface ProxPacketReceiveHandler {
    void onReceived(short id, byte[] data);
}
