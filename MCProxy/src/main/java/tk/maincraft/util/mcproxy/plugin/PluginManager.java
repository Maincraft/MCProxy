package tk.maincraft.util.mcproxy.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import tk.maincraft.util.mcpackets.Packet;
import tk.maincraft.util.mcproxy.MinecraftProxy;
import tk.maincraft.util.mcproxy.NetworkPartner;

public final class PluginManager {
    private final MinecraftProxy proxy;
    private final File folder;
    private final Set<Plugin> plugins;
    private final List<PacketHandlerInfo> handlers;

    public PluginManager(MinecraftProxy proxy, String folderName) {
        this.proxy = proxy;
        this.folder = new File(folderName);
        this.plugins = new HashSet<Plugin>();
        this.handlers = new ArrayList<PacketHandlerInfo>();
    }

    public File getFolder() {
        return folder;
    }

    public void loadPlugin(File pluginFile) {
        JarFile jar = null;
        InputStream stream = null;
        try {
            jar = new JarFile(pluginFile);
            JarEntry entry = jar.getJarEntry("pluginClass");

            if (entry == null) {
                throw new FileNotFoundException("pluginClass");
            }
            stream = jar.getInputStream(entry);

            String pluginClassName = new BufferedReader(new InputStreamReader(stream)).readLine();
            jar.close();
            jar = null;
            stream.close();
            stream = null;


            // read plugin
            URLClassLoader loader = URLClassLoader.newInstance(
                    new URL[] { pluginFile.toURI().toURL() }, this.getClass().getClassLoader());
            Class<?> jarClass = Class.forName(pluginClassName, true, loader);
            Class<? extends Plugin> pluginClass = jarClass.asSubclass(Plugin.class);

            this.addPlugin(pluginClass.newInstance());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void loadPlugins() {
        if (!folder.exists())
            folder.mkdirs();
        for (File file : folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File paramFile, String paramString) {
                return paramString.endsWith(".jar");
            }
        })) {
            loadPlugin(file);
        }

    }

    public void addPlugin(Plugin plugin) {
        plugin.setProxy(proxy);
        plugins.add(plugin);
    }

    public void bakeHandlers() {
        for (Plugin plugin : plugins) {
            for (Method method : plugin.getClass().getMethods()) {
                if (method.isAnnotationPresent(PacketHandler.class)) {
                    PacketHandler info = method.getAnnotation(PacketHandler.class);
                    // some validations:
                    // param-rules: either only packet-param or additional boolean-param (acceptsCancelled)
                    // returntype-rules: only void or boolean
                    boolean acceptsCancelled = (method.getParameterTypes().length == 2)
                            && (method.getParameterTypes()[1] == boolean.class);
                    if ((!((method.getParameterTypes().length == 1) || acceptsCancelled))
                            || (!Packet.class.isAssignableFrom(method.getParameterTypes()[0]))
                            || (!((method.getReturnType() == void.class
                                || method.getReturnType() == boolean.class)))) {
                        System.out.println("Invalid packet-handler: " + method);
                        continue;
                    }
                    handlers.add(new PacketHandlerInfo(info.priority(), method, plugin,
                            method.getParameterTypes()[0].asSubclass(Packet.class),
                            acceptsCancelled, info.target()));
                }
            }
        }
        // and sort them
        Collections.sort(handlers);
    }

    public boolean sendingPacket(Packet packet, NetworkPartner target) {
        boolean cancelled = false;
        ListIterator<PacketHandlerInfo> iterator = handlers.listIterator();
        while (iterator.hasNext()) {
            PacketHandlerInfo phi = iterator.next();
            if (phi.getPacketClass().isAssignableFrom(packet.getPacketType()) && phi.acceptsFrom(target)) {
                cancelled = !phi.call(packet, cancelled);
            }
        }
        return !cancelled;
    }

    private final static class PacketHandlerInfo implements Comparable<PacketHandlerInfo> {
        private final short priority;
        private final Method method;
        private final Plugin plugin;
        private final Class<? extends Packet> packetClass;
        private final boolean acceptsCancelled;
        private final NetworkPartner[] targets;

        public PacketHandlerInfo(short priority, Method method, Plugin plugin,
                Class<? extends Packet> packetClass, boolean acceptsCancelled, NetworkPartner[] targets) {
            this.priority = priority;
            this.method = method;
            this.plugin = plugin;
            this.packetClass = packetClass;
            this.acceptsCancelled = acceptsCancelled;
            this.targets = targets;
        }

        // returns false if it was cancelled
        public boolean call(Packet packet, boolean cancelled) {
            if (cancelled && !acceptsCancelled)
                return !cancelled; // we don't even call it then
            try {
                Object retval;
                if (!acceptsCancelled)
                    retval = method.invoke(plugin, packet);
                else retval = method.invoke(plugin, packet, cancelled);

                if (method.getReturnType() == boolean.class)
                    return (Boolean) retval;
                else return true;
            } catch (Exception e) {
                if (e.getCause() instanceof CancelPacketException)
                    return false;
                else throw new RuntimeException("Unexpected exception!", e);
            }
        }

        public Class<? extends Packet> getPacketClass() {
            return packetClass;
        }

        public boolean acceptsFrom(NetworkPartner partner) {
            for (NetworkPartner np : targets) {
                if (np == partner)
                    return true;
            }
            return false;
        }

        @Override
        public int compareTo(PacketHandlerInfo o) {
            return Short.valueOf(priority).compareTo(Short.valueOf(o.priority));
        }
    }
}
