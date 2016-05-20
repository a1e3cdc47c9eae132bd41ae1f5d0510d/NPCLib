package de.notaviable.npcs.utils;

import de.notaviable.npcs.data.MCVersion;
import org.bukkit.Bukkit;

/**
 * Project: KillSwitch
 * Created by notaviable on 14.04.2016.
 */
public class VersionUtils {
    public static MCVersion getVersion() {
        String version = Bukkit.getVersion();
        if (version.contains("(MC: 1.7")) return MCVersion.V1_7_X;
        if (version.contains("(MC: 1.8")) return MCVersion.V1_8_X;
        if (version.contains("(MC: 1.9")) return MCVersion.V1_9_X;
        return MCVersion.Unknown;
    }

    public static String getVersionString() {
        return getVersion().name().replace('_', '.').replaceFirst("V", "");
    }
}
