package de.notaviable.npcs;

import org.bukkit.entity.Player;

/**
 * Project: NPCLib
 * Created by notaviable on 29.04.2016.
 */
public interface HitListener {
    void onHit(Player hitter, int entityId);
}
