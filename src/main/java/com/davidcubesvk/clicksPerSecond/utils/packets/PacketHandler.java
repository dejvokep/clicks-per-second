package com.davidcubesvk.clicksPerSecond.utils.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import org.bukkit.event.block.Action;

/**
 * Class handling incoming packets to the server.
 */
public class PacketHandler {

    /**
     * Registers all packet listeners.
     */
    public PacketHandler() {
        //ProtocolManager instance from ProtocolLib
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        //Right click listeners
        //ITEM - UseItem (> block)
        //     - BlockPlace (> air)
        //     - UseEntity (> entity)
        //AIR - UseItem (> block)
        //    - X (> air)
        //    - UseEntity (> entity)

        //UseItem
        protocolManager.addPacketListener(
                new PacketAdapter(ClicksPerSecond.getPlugin(), ListenerPriority.MONITOR, PacketType.Play.Client.USE_ITEM) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        TestManager.getInstance().addClick(event.getPlayer().getUniqueId(), Action.RIGHT_CLICK_BLOCK);
                    }
                });
        //BlockPlace
        protocolManager.addPacketListener(
                new PacketAdapter(ClicksPerSecond.getPlugin(), ListenerPriority.MONITOR, PacketType.Play.Client.BLOCK_PLACE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        TestManager.getInstance().addClick(event.getPlayer().getUniqueId(), Action.RIGHT_CLICK_BLOCK);
                    }
                });
        //UseEntity
        protocolManager.addPacketListener(
                new PacketAdapter(ClicksPerSecond.getPlugin(), ListenerPriority.MONITOR, PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        TestManager.getInstance().addClick(event.getPlayer().getUniqueId(), Action.RIGHT_CLICK_BLOCK);
                    }
                });

        //Left click listeners
        //ITEM - ArmAnimation (> block)
        //     - ArmAnimation (> air)
        //     - ArmAnimation (> entity)
        //AIR - ArmAnimation (> block)
        //    - ArmAnimation (> air)
        //    - ArmAnimation (> entity)

        //ArmAnimation
        protocolManager.addPacketListener(
                new PacketAdapter(ClicksPerSecond.getPlugin(), ListenerPriority.MONITOR, PacketType.Play.Client.ARM_ANIMATION) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        TestManager.getInstance().addClick(event.getPlayer().getUniqueId(), Action.LEFT_CLICK_BLOCK);
                    }
                });
    }

}
