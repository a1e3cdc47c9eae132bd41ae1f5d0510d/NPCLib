package de.notaviable.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;

/**
 * Project: NPCLib
 * Created by notaviable on 29.04.2016.
 */
public class HitPacketListener {
    public HitPacketListener() {
        NPCLib.getProtocolManager().addPacketListener(new PacketAdapter(NPCLib.getInstance(), PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                EnumWrappers.EntityUseAction action = EnumWrappers.EntityUseAction.ATTACK;
                try {
                    action = event.getPacket().getEntityUseActions().read(0);
                } catch (Exception e) {
                }
                if (action != EnumWrappers.EntityUseAction.ATTACK) return;
                for (HitListener hitListener : NPCLib.getListeners()) {
                    try {
                        hitListener.onHit(event.getPlayer(), event.getPacket().getIntegers().read(0));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
