package org.thelevidr.deathrun;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class PersonalBestManager {
    private final JavaPlugin plugin;
    private final File pbFile;
    private YamlConfiguration pbConfig;

    public PersonalBestManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pbFile = new File(plugin.getDataFolder(), "pb.yml");
        loadPbConfig();
    }

    private void loadPbConfig() {
        if (!pbFile.exists()) {
            pbConfig = new YamlConfiguration();
            savePbConfig();
        } else {
            pbConfig = YamlConfiguration.loadConfiguration(pbFile);
        }
    }

    private void savePbConfig() {
        try {
            pbConfig.save(pbFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save pb.yml: " + e.getMessage());
        }
    }

    public void setPersonalBest(String mapName, UUID playerUuid, long timeMs) {
        String path = mapName + "." + playerUuid.toString();
        long existingPb = pbConfig.getLong(path, Long.MAX_VALUE);

        if (timeMs < existingPb) {
            pbConfig.set(path, timeMs);
            savePbConfig();
        }
    }

    public Long getPersonalBest(String mapName, UUID playerUuid) {
        String path = mapName + "." + playerUuid.toString();
        if (!pbConfig.contains(path)) {
            return null;
        }
        return pbConfig.getLong(path);
    }

    public String formatTime(long timeMs) {
        long minutes = (timeMs / 60000) % 60;
        long seconds = (timeMs / 1000) % 60;
        long millis = timeMs % 1000;
        return String.format("%02d:%02d:%03d", minutes, seconds, millis);
    }

    public void clearPersonalBest(String mapName, UUID playerUuid) {
        String path = mapName + "." + playerUuid.toString();
        if (pbConfig.contains(path)) {
            pbConfig.set(path, null);
            savePbConfig();
        }
    }

    public void clearAll() {
        pbConfig.set("map", null);
        savePbConfig();
    }
}