#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import tk.maincraft.util.mcpackets.Packet;

import tk.maincraft.util.mcproxy.plugin.PacketHandler;
import tk.maincraft.util.mcproxy.plugin.Plugin;

public class ${artifactId} extends Plugin {
    public ${artifactId}() {
        super("${artifactId}");
    }

    @PacketHandler
    public void onPacket(Packet packet) {
        // TODO
    }
}
