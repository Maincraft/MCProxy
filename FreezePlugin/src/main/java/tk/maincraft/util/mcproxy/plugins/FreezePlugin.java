package tk.maincraft.util.mcproxy.plugins;

import java.util.LinkedList;
import java.util.Queue;
import tk.maincraft.util.mcpackets.Packet;
import tk.maincraft.util.mcpackets.packet.ChatPacket;
import tk.maincraft.util.mcpackets.packet.impl.ChatPacketImpl;
import tk.maincraft.util.mcproxy.NetworkPartner;
import tk.maincraft.util.mcproxy.plugin.PacketHandler;
import tk.maincraft.util.mcproxy.plugin.Plugin;

public class FreezePlugin extends Plugin {
    private static enum Action {
        DISCARD, CAPTURE, FORWARD;

        public static Action fromString(String string) {
            try {
                return Action.valueOf(string.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    // dangerous. never forget the synchronized-block!
    private final Queue<Packet> forServerQueue = new LinkedList<Packet>();
    private final Queue<Packet> forClientQueue = new LinkedList<Packet>();
    private boolean active = false;
    private Action clientAction;
    private Action serverAction;

    public FreezePlugin() {
        super("FreezePlugin");
    }

    @PacketHandler(target = NetworkPartner.SERVER, priority = -1)
    public boolean handleCommands(ChatPacket packet) {
        String msg = packet.getMessage().toLowerCase();
        if (msg.startsWith("/proxy:freeze ")) {
            // this is a command!
            String[] args = msg.split(" ");
            if (args[1].equals("start") && (args.length == 4)) {
                // freeze start-command
                String[] action1 = args[2].split(":");
                String[] action2 = args[3].split(":");
                if ((action1.length == 2) && (action2.length == 2)) {
                    Action clientAction = null;
                    Action serverAction = null;
                    if (action1[0].equals("client") && action2[0].equals("server")) {
                        clientAction = Action.fromString(action1[1]);
                        serverAction = Action.fromString(action2[1]);
                    } else if (action1[0].equals("server") && action2[0].equals("client")) {
                        clientAction = Action.fromString(action2[1]);
                        serverAction = Action.fromString(action1[1]);
                    }
                    if ((clientAction != null) && (serverAction != null)) {
                        this.clientAction = clientAction;
                        this.serverAction = serverAction;
                        // clear queues, just in case
                        synchronized (forServerQueue) {
                            forServerQueue.clear();
                        }
                        synchronized (forClientQueue) {
                            forClientQueue.clear();
                        }
                        this.active = true;
                        return false;
                    }
                }
            }
            if (args[1].equals("flush") && ((args.length == 2) || (args.length == 3))) {
                // flush-command
                boolean flushed = false;
                if ((args.length == 2) || ((args.length == 3) && args[2].equals("client"))) {
                    // flush client-queue
                    synchronized (forClientQueue) {
                        this.getProxy().fastSend(forClientQueue, NetworkPartner.CLIENT);
                        forClientQueue.clear();
                    }
                    flushed = true;
                }
                if ((args.length == 2) || ((args.length == 3) && args[2].equals("server"))) {
                    // flush server-queue
                    synchronized (forServerQueue) {
                        this.getProxy().fastSend(forServerQueue, NetworkPartner.SERVER);
                        forServerQueue.clear();
                    }
                    flushed = true;
                }
                if (flushed)
                    return false;
            }
            if (args[1].equals("stop") && ((args.length == 2) || ((args.length == 3) && args[2].equals("noflush")))) {
                // stop-command
                this.active = false;
                boolean flush = (args.length != 3);
                synchronized (forServerQueue) {
                    if (flush)
                        this.getProxy().fastSend(forServerQueue, NetworkPartner.SERVER);
                    forServerQueue.clear();
                }
                synchronized (forClientQueue) {
                    if (flush)
                        this.getProxy().fastSend(forClientQueue, NetworkPartner.CLIENT);
                    forClientQueue.clear();
                }
                return false;
            }
            this.getProxy().sendPacket(new ChatPacketImpl("Usage: /proxy:freeze [start client:ACTION server:ACTION|flush [client|server]|stop [noflush]]"), NetworkPartner.CLIENT);
            return false;
        }
        return true;
    }

    @PacketHandler(priority = 100, target = NetworkPartner.CLIENT)
    public boolean interceptPacketsForClient(Packet packet) {
        return interceptPackets(packet, NetworkPartner.CLIENT);
    }

    @PacketHandler(priority = 100, target = NetworkPartner.SERVER)
    public boolean interceptPacketsForServer(Packet packet) {
        return interceptPackets(packet, NetworkPartner.SERVER);
    }

    public boolean interceptPackets(Packet packet, NetworkPartner target) {
        if (!active)
            return true;
        Queue<Packet> queue;
        Action action;
        if (target == NetworkPartner.CLIENT) {
            queue = forClientQueue;
            action = serverAction; // it is FOR the client and coming FROM the server
        } else if (target == NetworkPartner.SERVER) {
            queue = forServerQueue;
            action = clientAction; // it is FOR the server and coming FROM the client
        } else {
            System.out.println("FreezePlugin: Unknown NetworkPartner!");
            return true;
        }
        switch (action) {
        case DISCARD: // do nothing
            break;
        case FORWARD:
            return true;
        case CAPTURE:
            synchronized (queue) {
                if (active) // check the active-flag again. we're possibly stopping while
                    queue.add(packet); // doing the things above
            }
            break;
        }
        return false;
    }
}
