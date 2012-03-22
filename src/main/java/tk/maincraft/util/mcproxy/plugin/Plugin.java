package tk.maincraft.util.mcproxy.plugin;

public abstract class Plugin {
    private final String name;

    public Plugin(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
