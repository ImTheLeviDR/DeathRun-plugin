package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private List<EffectPad> effectPads = new ArrayList<>();
    private int finishMinX, finishMaxX, finishMinY, finishMaxY, finishMinZ, finishMaxZ;
    private boolean hasFinishRegion = false;
    private Location spawnLocation;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File optionsFile = new File(plugin.getDataFolder(), "options.yml");
        if (!optionsFile.exists()) {
            plugin.saveResource("options.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(optionsFile);
    }

    public void reloadAll() {
        loadConfig();
        loadAllEffectPads();
        loadSpawnLocation();
    }

    public void loadAllEffectPads() {
        effectPads.clear();
        finishMinX = 0;
        finishMinY = 0;
        finishMinZ = 0;
        finishMaxX = 0;
        finishMaxY = 0;
        finishMaxZ = 0;
        if (!config.contains("map")) return;

        for (String mapName : config.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            loadEffectPads(path);
            loadFinishRegion(path);
        }
    }

    private void loadFinishRegion(String path) {
        ConfigurationSection finish1 = config.getConfigurationSection(path + ".finish.1");
        ConfigurationSection finish2 = config.getConfigurationSection(path + ".finish.2");
        if (finish1 != null && finish2 != null) {
            Location loc1 = getLocationFromSection(finish1);
            Location loc2 = getLocationFromSection(finish2);
            if (loc1 != null && loc2 != null) {
                finishMinX = Math.min(loc1.getBlockX(), loc2.getBlockX());
                finishMaxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
                finishMinY = Math.min(loc1.getBlockY(), loc2.getBlockY());
                finishMaxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
                finishMinZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
                finishMaxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
                hasFinishRegion = true;
            }
        }
    }

    public void loadSpawnLocation() {
        spawnLocation = null;
        if (!config.contains("map")) return;

        for (String mapName : config.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName + ".spawn";
            if (!config.contains(path + ".x") || !config.contains(path + ".y") ||
                !config.contains(path + ".z") || !config.contains(path + ".world") ||
                !config.contains(path + ".facing")) {
                plugin.getLogger().severe("Missing spawn configuration for map: " + mapName);
                return;
            }

            World world = plugin.getServer().getWorld(config.getString(path + ".world"));
            if (world == null) {
                plugin.getLogger().severe("Invalid world in spawn config: " + config.getString(path + ".world"));
                return;
            }

            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            String facingStr = config.getString(path + ".facing").toLowerCase();

            spawnLocation = new Location(world, x, y, z);

            float yaw = 0;
            switch (facingStr) {
                case "north": yaw = 180; break;
                case "south": yaw = 0; break;
                case "east": yaw = -90; break;
                case "west": yaw = 90; break;
                default: plugin.getLogger().warning("Invalid facing value: " + facingStr + ", defaulting to north"); yaw = 180;
            }
            spawnLocation.setYaw(yaw);
            return;
        }
    }

    public void loadMapData(String mapName) {
        String path = "map." + mapName;
        effectPads.clear();
        loadEffectPads(path);
        loadFinishRegion(path);
        loadSpawnLocation();
    }

    private void loadEffectPads(String path) {
        ConfigurationSection effects = config.getConfigurationSection(path + ".effects");
        if (effects == null) return;

        for (String effectType : effects.getKeys(false)) {
            ConfigurationSection effectSection = effects.getConfigurationSection(effectType);
            if (effectSection == null) continue;

            PotionEffectType potionType = effectType.equalsIgnoreCase("speed") ? PotionEffectType.SPEED :
                    effectType.equalsIgnoreCase("jump") ? PotionEffectType.JUMP : null;

            if (potionType == null) continue;

            for (String key : effectSection.getKeys(false)) {
                ConfigurationSection padSection = effectSection.getConfigurationSection(key);
                if (padSection == null) continue;

                ConfigurationSection pos1 = padSection.getConfigurationSection("pos.1");
                ConfigurationSection pos2 = padSection.getConfigurationSection("pos.2");
                if (pos1 == null || pos2 == null) continue;

                Location padLoc1 = getLocationFromSection(pos1);
                Location padLoc2 = getLocationFromSection(pos2);
                int amplifier = padSection.getInt("amplifier", 1);
                int duration = padSection.getInt("duration", 2);

                if (padLoc1 != null && padLoc2 != null) {
                    effectPads.add(new EffectPad(padLoc1, padLoc2, potionType, amplifier, duration));
                }
            }
        }
    }

    private Location getLocationFromSection(ConfigurationSection section) {
        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        String worldName = section.getString("world");
        World world = plugin.getServer().getWorld(worldName);
        if (world != null) {
            return new Location(world, x, y, z);
        }
        return null;
    }

    public Location getLocationFromConfig(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null ? getLocationFromSection(section) : null;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public List<EffectPad> getEffectPads() {
        return effectPads;
    }

    public Location getFinishMin() {
        return null;
    }

    public Location getFinishMax() {
        return null;
    }
    
    public boolean hasFinishRegion() {
        return hasFinishRegion;
    }
    
    public boolean isInFinishRegion(Location loc) {
        if (!hasFinishRegion) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= finishMinX && x <= finishMaxX && 
               y >= finishMinY && y <= finishMaxY && 
               z >= finishMinZ && z <= finishMaxZ;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public boolean hasValidMap() {
        if (!config.contains("map")) return false;
        for (String mapName : config.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            if (config.contains(path + ".glass.1") && config.contains(path + ".glass.2")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMap(String mapName) {
        return config.contains("map." + mapName + ".name");
    }

    public boolean hasGlassLocations(String mapName) {
        String path = "map." + mapName;
        return config.contains(path + ".glass.1") && config.contains(path + ".glass.2");
    }
}
