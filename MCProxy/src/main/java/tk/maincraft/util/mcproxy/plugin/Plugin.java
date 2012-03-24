package tk.maincraft.util.mcproxy.plugin;

import tk.maincraft.util.mcproxy.MinecraftProxy;

/**
 * All plugins have to extend this class.
 * @see <a href="https://github.com/Maincraft/MCProxy/wiki">The wiki</a>
 */
public abstract class Plugin {
    private final String name;
    private MinecraftProxy proxy;

    /**
     * Creates a new Plugin-instance.
     * @param name This plugin's name.
     */
    public Plugin(String name) {
        this.name = name;
    }

    /**
     * @return This plugin's name.
     */
    public final String getName() {
        return name;
    }

    /**
     * @return A reference to the MCProxy-instance.
     */
    public final MinecraftProxy getProxy() {
        return proxy;
    }

    /*
     * The PluginManager uses this to inject the MCProxy-instance.
     */
    final void setProxy(MinecraftProxy proxy) {
        this.proxy = proxy;
    }
}
