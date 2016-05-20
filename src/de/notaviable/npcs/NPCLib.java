package de.notaviable.npcs;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.notaviable.npcs.data.MCVersion;
import de.notaviable.npcs.utils.VersionUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Random;

/**
 * Project: NPCLib
 * Created by notaviable on 29.04.2016.
 */
public class NPCLib extends JavaPlugin {
    private static NPCLib instance;
    private MCVersion mcVersion = VersionUtils.getVersion();

    public Random rdm;

    public ArrayList<HitListener> listeners = new ArrayList<>();

    public static ProtocolManager getProtocolManager() {
        return ProtocolLibrary.getProtocolManager();
    }

    public static MCVersion getMCVersion() {
        return getInstance().mcVersion;
    }

    public static NPCLib getInstance() {
        return instance;
    }

    public static ArrayList<HitListener> getListeners() {
        return getInstance().listeners;
    }

    public static Random getRdm() {
        return getInstance().rdm;
    }

    @Override
    public void onEnable() {
        instance = this;
        rdm = new Random();
        new HitPacketListener();
    }

    @Override
    public void onDisable() {
        listeners.clear();
    }

    public void registerListener(HitListener listener) {
        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }
   public void unregisterListener(HitListener listener) {
       listeners.remove(listener);
   }


}
