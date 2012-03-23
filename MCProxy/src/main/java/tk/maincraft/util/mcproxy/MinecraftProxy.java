package tk.maincraft.util.mcproxy;

import java.util.Collection;

import tk.maincraft.util.mcpackets.Packet;

public interface MinecraftProxy {
    void sendPacket(Packet packet, NetworkPartner target);
    // almost never use this because it will DIRECTLY add the packets to the send-queue
    void fastSend(Collection<Packet> packets, NetworkPartner target);
    void shutdown();
}
