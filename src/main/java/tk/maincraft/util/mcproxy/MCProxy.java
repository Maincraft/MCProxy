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
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import tk.maincraft.util.mcpackets.Packet;
import tk.maincraft.util.mcpackets.PacketReader;
import tk.maincraft.util.mcpackets.PacketWriter;
import tk.maincraft.util.mcpackets.Packets;
import tk.maincraft.util.mcpackets.packet.KickPacket;
import tk.maincraft.util.mcpackets.packet.LoginPacket;
import tk.maincraft.util.mcpackets.packet.impl.KickPacketImpl;

public class MCProxy {
    public static void main(String[] args) throws Exception {
        MCProxy mcproxy = new MCProxy(25566);
        mcproxy.run();
    }

    private final int port;
    private final BlockingQueue<Packet> serverToClientQueue = new LinkedBlockingQueue<Packet>(); //new ConcurrentLinkedQueue<Packet>();
    private final BlockingQueue<Packet> clientToServerQueue = new LinkedBlockingQueue<Packet>(); //new ConcurrentLinkedQueue<Packet>();

    private Object shutdownLock = new Object();

    public MCProxy(int port) {
        this.port = port;
    }

    private final class ReaderThread extends Thread {
        private final Queue<Packet> target;
        private final DataInput input;
        private final Object lock;
        private final boolean isClient;

        public ReaderThread(Queue<Packet> target, DataInput input, Object lock, boolean isClient) {
            this.target = target;
            this.input = input;
            this.lock = lock;
            this.isClient = isClient;
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Packet read = PacketReader.read(input);
                    if (isClient) {
                        // client checks
                        if (read instanceof LoginPacket) {
                            LoginPacket loginPacket = (LoginPacket) read;
                            int protocolVersion = loginPacket.getProtocolVersionOrEntityId();
                            if (protocolVersion != Packets.PROTOCOL_VERSION) {
                                System.out.println("Their protocol: " + protocolVersion);
                                System.out.println("Our protocol: " + Packets.PROTOCOL_VERSION);
                                KickPacket kickPacket = new KickPacketImpl(
                                        (protocolVersion > Packets.PROTOCOL_VERSION) ? "Outdated proxy!"
                                                : "Outdated client!");
                                System.out.println("MCProxy: " + kickPacket);
                                serverToClientQueue.add(kickPacket);
                                clientToServerQueue.add(kickPacket);
                                System.out.println(this.getName()
                                        + ": Normal termination due to us sending a KickPacket!");
                                break;
                            }
                        }
                    } else {
                        // server checks
                    }
                    target.add(read);
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

    private static final class WriterThread extends Thread {
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
                    Packet packet = source.take();
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
        Thread clientReader = new ReaderThread(clientToServerQueue, dIn1, shutdownLock, true);
        clientReader.setName("clientReader");
        Thread serverReader = new ReaderThread(serverToClientQueue, dIn2, shutdownLock, false);
        serverReader.setName("serverReader");
        Thread clientWriter = new WriterThread(serverToClientQueue, dOut1, buffer1);
        clientWriter.setName("clientWriter");
        Thread serverWriter = new WriterThread(clientToServerQueue, dOut2, buffer2);
        serverWriter.setName("serverWriter");
        // start threads
        clientReader.start();
        serverReader.start();
        clientWriter.start();
        serverWriter.start();
        // join readers, they'll crash
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
}
