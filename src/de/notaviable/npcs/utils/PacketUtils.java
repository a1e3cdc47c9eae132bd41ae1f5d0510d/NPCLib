package de.notaviable.npcs.utils;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Project: KillSwitch
 * Created by notaviable on 14.04.2016.
 */
public class PacketUtils {
    public static int getCompressedAngle(float angle) {
        return (int) (angle * 256.0F / 360.0F);
    }

    public static WrappedDataWatcher getDefaultWatcher(World world, EntityType type) {
        if (type == EntityType.PLAYER) getPlayerWatcher(PlayerUtils.getRandomOnlinePlayer());
        Entity entity = world.spawnEntity(new Location(world, 0, 256, 0), type);
        WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
        entity.remove();
        return watcher;
    }

    private static WrappedDataWatcher getPlayerWatcher(Player player) {
        return WrappedDataWatcher.getEntityWatcher(player).deepClone();
    }
}
