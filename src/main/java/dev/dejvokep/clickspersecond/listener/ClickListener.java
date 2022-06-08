package dev.dejvokep.clickspersecond.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.dejvokep.clickspersecond.ClicksPerSecond;

/**
 * Class handling incoming packets to the server.
 */
public class ClickListener {

    private final ClicksPerSecond plugin;

    /**
     * Registers the {@link PacketType.Play.Client#ARM_ANIMATION} packet listener.
     */
    public ClickListener(ClicksPerSecond plugin) {
        this.plugin = plugin;
        // Create
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Client.getInstance().values()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                System.out.println(event.getPacketType().name());
            }
        });
    }

}
