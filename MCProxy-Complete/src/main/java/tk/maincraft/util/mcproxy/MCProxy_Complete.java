package tk.maincraft.util.mcproxy;

import tk.maincraft.util.mcproxy.plugins.FreezePlugin;
import tk.maincraft.util.mcproxy.plugins.ProtocolVersionVerificationPlugin;

public class MCProxy_Complete {
    public static void main(String[] args) throws Exception {
        MCProxy mcproxy = new MCProxy(25566);
        mcproxy.getPluginManager().addPlugin(new ProtocolVersionVerificationPlugin());
        mcproxy.getPluginManager().addPlugin(new FreezePlugin());
        mcproxy.run();
    }
}
