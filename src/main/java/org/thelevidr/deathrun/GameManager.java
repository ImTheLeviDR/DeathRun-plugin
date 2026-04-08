package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private long gameStartTime = 0;
    private boolean isGameRunning = false;
    private Location glassLoc1;
    private Location glassLoc2;

    public GameManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void startGame(String mapName) {
        String path = "map." + mapName;

        glassLoc1 = configManager.getLocationFromConfig(path + ".glass.1");
        glassLoc2 = configManager.getLocationFromConfig(path + ".glass.2");

        if (glassLoc1 == null || glassLoc2 == null) return;

        setBlocksInRegion(glassLoc1, glassLoc2, Material.getMaterial("STAINED_GLASS"), (byte) 5);

        configManager.loadMapData(mapName);

        startCountdown();
    }

    public void reload() {
        gameStartTime = 0;
        isGameRunning = false;
    }

    private void startCountdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§a3", "", 0, 20, 0);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendTitle("§62", "", 0, 20, 0);
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendTitle("§c1", "", 0, 20, 0);
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.sendTitle("§aRUN!", "", 0, 20, 0);
                    }
                    setBlocksInRegion(glassLoc1, glassLoc2, Material.AIR, (byte) 0);
                    gameStartTime = System.currentTimeMillis();
                    isGameRunning = true;
                }, 20L);
            }, 20L);
        }, 20L);
    }

    private void setBlocksInRegion(Location loc1, Location loc2, Material material, byte data) {
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = loc1.getWorld().getBlockAt(x, y, z);
                    block.setType(material);
                    if (material != Material.AIR) {
                        block.setData(data);
                    }
                }
            }
        }
    }

    public boolean checkFinish(Player player) {
        if (!isGameRunning) return false;

        if (!configManager.hasFinishRegion()) return false;

        Location playerLoc = player.getLocation();
        
        if (!configManager.isInFinishRegion(playerLoc)) return false;

        int px = playerLoc.getBlockX();
        int py = playerLoc.getBlockY() - 1;
        int pz = playerLoc.getBlockZ();
        
        Block blockBelow = playerLoc.getWorld().getBlockAt(px, py, pz);

        if (blockBelow.getType() == Material.PORTAL) {
            finishPlayer(player);
            return true;
        }
        return false;
    }

    private void finishPlayer(Player player) {
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long minutes = (elapsed / 60000) % 60;
        long seconds = (elapsed / 1000) % 60;
        long millis = elapsed % 1000;

        Location spawnLocation = configManager.getSpawnLocation();
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        }
        player.sendMessage("§aFinish! Time: §e" +
                String.format("%02d:%02d:%03d", minutes, seconds, millis));

        gameStartTime = 0;
        isGameRunning = false;
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }
}
