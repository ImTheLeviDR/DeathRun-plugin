package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MapRecognizer {
    private final JavaPlugin plugin;
    private final Map<String, String> worldToMapName = new HashMap<>();
    private final Map<String, Location> mapSpawns = new HashMap<>();

    public MapRecognizer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerMap(String mapName, String worldName, Location spawnLocation) {
        worldToMapName.put(worldName, mapName);
        if (spawnLocation != null) {
            mapSpawns.put(mapName, spawnLocation);
        }
    }

    public String getMapName(String worldName) {
        return worldToMapName.get(worldName);
    }

    public String getMapName(World world) {
        return world != null ? worldToMapName.get(world.getName()) : null;
    }

    public Location getSpawnLocation(String mapName) {
        return mapSpawns.get(mapName);
    }

    public Location getSpawnLocation(World world) {
        String mapName = getMapName(world);
        return mapName != null ? mapSpawns.get(mapName) : null;
    }

    public void clear() {
        worldToMapName.clear();
        mapSpawns.clear();
    }
}