package tk.maincraft.util.mcproxy.plugins;

import tk.maincraft.util.mcpackets.Packets;
import tk.maincraft.util.mcpackets.packet.KickPacket;
import tk.maincraft.util.mcpackets.packet.LoginPacket;
import tk.maincraft.util.mcpackets.packet.impl.KickPacketImpl;
import tk.maincraft.util.mcproxy.NetworkPartner;
import tk.maincraft.util.mcproxy.plugin.PacketHandler;
import tk.maincraft.util.mcproxy.plugin.Plugin;

public class ProtocolVersionVerificationPlugin extends Plugin {
    public ProtocolVersionVerificationPlugin() {
        super("ProtocolVersionVerificationPlugin");
    }

    @PacketHandler(target = NetworkPartner.SERVER)
    public void checkProtocolVersion(LoginPacket packet) {
        int protocolVersion = packet.getProtocolVersionOrEntityId();
        if (protocolVersion != Packets.PROTOCOL_VERSION) {
            System.out.println("Their protocol: " + protocolVersion);
            System.out.println("Our protocol: " + Packets.PROTOCOL_VERSION);
            KickPacket kickPacket = new KickPacketImpl((protocolVersion > Packets.PROTOCOL_VERSION)
                    ? "Outdated proxy!" : "Outdated client!");
            System.out.println("So we say: " + kickPacket);
            this.getProxy().sendPacket(kickPacket, NetworkPartner.CLIENT);
            this.getProxy().sendPacket(kickPacket, NetworkPartner.SERVER);
        }
    }
}
