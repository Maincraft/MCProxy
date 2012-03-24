package tk.maincraft.util.mcproxy;

import java.util.Collection;

import tk.maincraft.util.mcpackets.Packet;
import tk.maincraft.util.mcproxy.plugin.PacketHandler;

public interface MinecraftProxy {
    /**
     * Sends a {@link Packet} to a {@link NetworkPartner}.
     *
     * @param packet The {@link Packet}.
     * @param target The {@link NetworkPartner} that should receive it.
     */
    void sendPacket(Packet packet, NetworkPartner target);

    /**
     * <b><u>Directly</u></b> sends a {@link Collection} of {@link Packet}s to
     * a {@link NetworkPartner}, bypassing all {@link PacketHandler}s. Using this
     * is hardly ever the right way.
     *
     * @param packets The packets.
     * @param target The {@link NetworkPartner} that should receive the packets.
     */
    void fastSend(Collection<Packet> packets, NetworkPartner target);

    /**
     * Stops MCProxy.
     */
    void shutdown();
}
