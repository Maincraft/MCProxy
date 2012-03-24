package tk.maincraft.util.mcproxy.plugin;

/**
 * A {@link PacketHandler} can throw this to cancel the packet.
 */
public final class CancelPacketException extends Exception {
    private static final long serialVersionUID = 1L;

    public CancelPacketException() {
        super();
    }
}
