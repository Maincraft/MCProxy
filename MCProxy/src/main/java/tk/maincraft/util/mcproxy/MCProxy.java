package tk.maincraft.util.mcproxy;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.Flushable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import tk.maincraft.util.mcpackets.Packet;
import tk.maincraft.util.mcpackets.PacketReader;
import tk.maincraft.util.mcpackets.PacketWriter;
import tk.maincraft.util.mcpackets.packet.KickPacket;
import tk.maincraft.util.mcpackets.packet.impl.KeepAlivePacketImpl;
import tk.maincraft.util.mcproxy.plugin.PluginManager;

public class MCProxy implements MinecraftProxy {
    public static void main(String[] args) throws Exception {
        MCProxy mcproxy = new MCProxy(25566);
        mcproxy.run();
    }

    private final int port;
    private final PluginManager pluginManager;
    private final BlockingQueue<Packet> clientQueue = new LinkedBlockingQueue<Packet>();
    private final BlockingQueue<Packet> serverQueue = new LinkedBlockingQueue<Packet>();

    private Object shutdownLock = new Object();

    public MCProxy(int port) {
        this.port = port;
        this.pluginManager = new PluginManager(this, "plugins");
    }

    private final class ReaderThread extends Thread {
        private final DataInput input;
        private final Object lock;
        private final NetworkPartner target;

        public ReaderThread(NetworkPartner target, DataInput input, Object lock) {
            this.target = target;
            this.input = input;
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Packet read = PacketReader.read(input);
                    sendPacket(read, target);
                    if (read instanceof KickPacket) {
                        System.out.println(this.getName() + ": Normal termination due to a KickPacket!");
                        break;
                    }
                }
            } catch (EOFException e) {
                System.out.println(this.getName() + ": EOFException");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Exception in Thread '" + this.getName() + "':");
                e.printStackTrace(System.out);
            }
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private final class WriterThread extends Thread {
        private final BlockingQueue<Packet> source;
        private final DataOutput output;
        private final Flushable flushHook;

        public WriterThread(BlockingQueue<Packet> source, DataOutput output, Flushable flushHook) {
            super();
            this.source = source;
            this.output = output;
            this.flushHook = flushHook;
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Packet packet = source.poll(500, TimeUnit.MILLISECONDS);
                    if (packet == null) // timeout? send a keepalive  to keep them happy
                        packet = new KeepAlivePacketImpl(0);
                    PacketWriter.writePacket(output, packet);
                    flushHook.flush();
                }
            } catch (InterruptedException e) {
                System.out.println(this.getName() + ": Terminating due to interrupt.");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Exception in Thread '" + this.getName() + "':");
                e.printStackTrace(System.out);
            }
        }
    }

    /*
     * At the moment, this only redirects all traffic between a client and a server.
     * This is listening at *:25566 and redirects to 127.0.0.1:25565
     */
    public void run() throws Exception {
        // load plugins
        pluginManager.loadPlugins();
        pluginManager.bakeHandlers();

        ServerSocket socket = new ServerSocket(port);
        System.out.println("MCProxy: Now listening at *:" + port);
        Socket sock = socket.accept();
        System.out.println("MCProxy: A client connected!");
        System.out.print("MCProxy: Connecting to server ... ");
        // connect to passthrough server
        Socket myClient = new Socket("localhost", 25565);
        System.out.println("DONE!");

//        DataInputStream dIn1 = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
//        DataInputStream dIn2 = new DataInputStream(new BufferedInputStream(myClient.getInputStream()));
        DataInputStream dIn1 = new DataInputStream(sock.getInputStream());
        DataInputStream dIn2 = new DataInputStream(myClient.getInputStream());

        BufferedOutputStream buffer1 = new BufferedOutputStream(sock.getOutputStream());
        BufferedOutputStream buffer2 = new BufferedOutputStream(myClient.getOutputStream());
        DataOutputStream dOut1 = new DataOutputStream(buffer1);
        DataOutputStream dOut2 = new DataOutputStream(buffer2);
        Thread clientReader = new ReaderThread(NetworkPartner.SERVER, dIn1, shutdownLock);
        clientReader.setName("clientReader");
        Thread serverReader = new ReaderThread(NetworkPartner.CLIENT, dIn2, shutdownLock);
        serverReader.setName("serverReader");
        Thread clientWriter = new WriterThread(clientQueue, dOut1, buffer1);
        clientWriter.setName("clientWriter");
        Thread serverWriter = new WriterThread(serverQueue, dOut2, buffer2);
        serverWriter.setName("serverWriter");
        // start threads
        clientReader.start();
        serverReader.start();
        clientWriter.start();
        serverWriter.start();
        synchronized (shutdownLock) {
            shutdownLock.wait();
        }
        clientReader.interrupt();
        serverReader.interrupt();
        clientWriter.interrupt();
        serverWriter.interrupt();
        clientWriter.join();
        serverWriter.join();
        // close all these streams...
        dIn1.close();
        dIn2.close();
        dOut1.close();
        dOut2.close();
        sock.close();
        myClient.close();
        System.out.println("-- END --");
    }

    @Override
    public void sendPacket(Packet packet, NetworkPartner target) {
        if (pluginManager.sendingPacket(packet, target)) {
            switch (target) {
            case CLIENT:
                clientQueue.add(packet);
                break;
            case SERVER:
                serverQueue.add(packet);
                break;
            default:
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (shutdownLock) {
            shutdownLock.notifyAll();
        }
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
}
