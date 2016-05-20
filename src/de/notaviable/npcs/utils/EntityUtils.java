package de.notaviable.npcs.utils;

import de.notaviable.npcs.NPC;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Project: KillSwitch
 * Created by notaviable on 25.03.2016.
 */
public class EntityUtils {
    public static Entity getEntityById(World world, int eid) {
        for (Entity entity : world.getEntities()) {
            if (entity.getEntityId() == eid) return entity;
        }
        return null;
    }

    public static int findFreeEntityId(World world) {
        int id = new Random().nextInt(Integer.MAX_VALUE);
        while (getEntityById(world, id) != null)
            id = new Random().nextInt(Integer.MAX_VALUE);
        return id;
    }

    public static NPC findNPCbyID(Player p, int eid) {
        return null;
    }
}
