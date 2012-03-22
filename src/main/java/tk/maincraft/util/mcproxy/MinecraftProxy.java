package tk.maincraft.util.mcproxy;

import tk.maincraft.util.mcpackets.Packet;

public interface MinecraftProxy {
    void sendPacket(Packet packet, NetworkPartner target);
    void shutdown();
}
