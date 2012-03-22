package tk.maincraft.util.mcproxy.plugin;

import tk.maincraft.util.mcproxy.MinecraftProxy;

public abstract class Plugin {
    private final String name;
    private MinecraftProxy proxy;

    public Plugin(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final MinecraftProxy getProxy() {
        return proxy;
    }

    final void setProxy(MinecraftProxy proxy) {
        this.proxy = proxy;
    }
}
