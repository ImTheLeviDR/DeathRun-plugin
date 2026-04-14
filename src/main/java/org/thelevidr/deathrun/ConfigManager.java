package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration defaultsConfig;
    private FileConfiguration mergedConfig;
    private List<EffectPad> effectPads = new ArrayList<>();
    private int finishMinX, finishMaxX, finishMinY, finishMaxY, finishMinZ, finishMaxZ;
    private boolean hasFinishRegion = false;
    private Location spawnLocation;
    private int glassConstant = 1;
    private int glassParam1 = 5;
    private int glassParam2 = 5;
    private Location glassOrigin;

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
        defaultsConfig = null;
        mergedConfig = null;
        loadDefaultsConfig();
        checkForConflicts();
        loadAllEffectPads();
    }

    private void loadDefaultsConfig() {
        defaultsConfig = null;
        try {
            URL url = new URL("https://raw.githubusercontent.com/ImTheLeviDR/DeathRun-plugin/refs/heads/master/defaults.yml");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "DeathRun-Plugin");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                defaultsConfig = YamlConfiguration.loadConfiguration(new java.io.StringReader(content.toString()));
                plugin.getLogger().info("Loaded default maps from GitHub.");
            } else {
                plugin.getLogger().warning("Failed to fetch defaults.yml: HTTP " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load defaults.yml: " + e.getMessage());
        }
    }

    private void checkForConflicts() {
        if (defaultsConfig == null || !defaultsConfig.contains("map")) return;
        if (!config.contains("map")) return;

        Set<String> defaultsMaps = new HashSet<>(defaultsConfig.getConfigurationSection("map").getKeys(false));
        Set<String> customMaps = new HashSet<>(config.getConfigurationSection("map").getKeys(false));

        defaultsMaps.retainAll(customMaps);
        if (!defaultsMaps.isEmpty()) {
            plugin.getLogger().severe("ERROR: Map name conflict(s) found: " + defaultsMaps +
                ". Remove these maps from your config.yml or use different names for custom maps.");
        }
    }

    private FileConfiguration getMergedConfig() {
        if (mergedConfig == null) {
            mergedConfig = new YamlConfiguration();
            if (defaultsConfig != null && defaultsConfig.contains("map")) {
                for (String key : defaultsConfig.getConfigurationSection("map").getKeys(false)) {
                    mergedConfig.set("map." + key, defaultsConfig.getConfigurationSection("map." + key));
                }
            }
            if (config.contains("map")) {
                for (String key : config.getConfigurationSection("map").getKeys(false)) {
                    mergedConfig.set("map." + key, config.getConfigurationSection("map." + key));
                }
            }
        }
        return mergedConfig;
    }

    public void loadAllEffectPads() {
        effectPads.clear();
        finishMinX = 0;
        finishMinY = 0;
        finishMinZ = 0;
        finishMaxX = 0;
        finishMaxY = 0;
        finishMaxZ = 0;
        FileConfiguration merged = getMergedConfig();
        if (!merged.contains("map")) return;

        for (String mapName : merged.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            loadEffectPads(path);
            loadFinishRegion(path);
        }
    }

    private void loadFinishRegion(String path) {
        FileConfiguration merged = getMergedConfig();
        ConfigurationSection finish1 = merged.getConfigurationSection(path + ".finish.1");
        ConfigurationSection finish2 = merged.getConfigurationSection(path + ".finish.2");
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

    public void loadSpawnLocation(String mapName) {
        spawnLocation = null;
        FileConfiguration merged = getMergedConfig();
        String path = "map." + mapName + ".spawn";
        if (!merged.contains(path + ".x") || !merged.contains(path + ".y") ||
            !merged.contains(path + ".z") || !merged.contains(path + ".world") ||
            !merged.contains(path + ".facing")) {
            plugin.getLogger().severe("Missing spawn configuration for map: " + mapName);
            return;
        }

        World world = plugin.getServer().getWorld(merged.getString(path + ".world"));
        if (world == null) {
            plugin.getLogger().severe("Invalid world in spawn config: " + merged.getString(path + ".world"));
            return;
        }

        double x = merged.getDouble(path + ".x");
        double y = merged.getDouble(path + ".y");
        double z = merged.getDouble(path + ".z");
        String facingStr = merged.getString(path + ".facing").toLowerCase();

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
    }

    public void loadMapData(String mapName) {
        FileConfiguration merged = getMergedConfig();
        String path = "map." + mapName;
        effectPads.clear();
        loadEffectPads(path);
        loadFinishRegion(path);
        loadSpawnLocation(mapName);
        loadGlassData(path);
    }

    private void loadGlassData(String path) {
        FileConfiguration merged = getMergedConfig();
        glassOrigin = null;
        glassConstant = 1;
        glassParam1 = 5;
        glassParam2 = 5;
        
        ConfigurationSection originSection = merged.getConfigurationSection(path + ".glass.origin");
        if (originSection != null) {
            glassOrigin = getLocationFromSection(originSection);
            glassConstant = merged.getInt(path + ".glass.constant", 1);
            glassParam1 = merged.getInt(path + ".glass.param1", 5);
            glassParam2 = merged.getInt(path + ".glass.param2", 5);
        }
    }

    public int getGlassConstant() { return glassConstant; }
    public int getGlassParam1() { return glassParam1; }
    public int getGlassParam2() { return glassParam2; }
    public Location getGlassOrigin() { return glassOrigin; }

    private void loadEffectPads(String path) {
        FileConfiguration merged = getMergedConfig();
        ConfigurationSection effects = merged.getConfigurationSection(path + ".effects");
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
        FileConfiguration merged = getMergedConfig();
        ConfigurationSection section = merged.getConfigurationSection(path);
        return section != null ? getLocationFromSection(section) : null;
    }

    public FileConfiguration getConfig() {
        return getMergedConfig();
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
        FileConfiguration merged = getMergedConfig();
        if (!merged.contains("map")) return false;
        for (String mapName : merged.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            if (merged.contains(path + ".glass.origin")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMap(String mapName) {
        FileConfiguration merged = getMergedConfig();
        return merged.contains("map." + mapName + ".name");
    }

    public boolean hasGlassLocations(String mapName) {
        FileConfiguration merged = getMergedConfig();
        String path = "map." + mapName;
        return merged.contains(path + ".glass.origin");
    }

    public List<String> getAllMapNames() {
        FileConfiguration merged = getMergedConfig();
        if (!merged.contains("map")) return new ArrayList<>();
        return new ArrayList<>(merged.getConfigurationSection("map").getKeys(false));
    }

    public String getMapDisplayName(String mapName) {
        FileConfiguration merged = getMergedConfig();
        String path = "map." + mapName + ".name";
        if (merged.contains(path)) {
            return merged.getString(path);
        }
        return mapName;
    }

    public Map<String, String> getAllMapWorlds() {
        Map<String, String> result = new HashMap<>();
        FileConfiguration merged = getMergedConfig();
        if (!merged.contains("map")) return result;
        
        for (String mapName : merged.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            if (merged.contains(path + ".spawn.world")) {
                String worldName = merged.getString(path + ".spawn.world");
                result.put(mapName, worldName);
            }
        }
        return result;
    }
}
