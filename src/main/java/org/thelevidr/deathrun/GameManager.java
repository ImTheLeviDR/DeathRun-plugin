package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private long gameStartTime = 0;
    private boolean isGameRunning = false;
    private Location glassOrigin;
    private int glassConstant;
    private int glassParam1;
    private int glassParam2;

    public GameManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void startGame(String mapName) {
        String path = "map." + mapName;

        configManager.loadMapData(mapName);
        
        glassOrigin = configManager.getGlassOrigin();
        glassConstant = configManager.getGlassConstant();
        glassParam1 = configManager.getGlassParam1();
        glassParam2 = configManager.getGlassParam2();

        if (glassOrigin == null) return;

        setBlocksByConstant(glassOrigin, glassConstant, glassParam1, glassParam2, Material.getMaterial("STAINED_GLASS"), (byte) 5);

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
                    if (glassOrigin != null) {
                        setBlocksByConstant(glassOrigin, glassConstant, glassParam1, glassParam2, Material.AIR, (byte) 0);
                    }
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

    private void setBlocksByConstant(Location origin, int constant, int param1, int param2, Material material, byte data) {
        if (origin == null) return;
        
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();
        World world = origin.getWorld();

        boolean addingGlass = (material != Material.AIR);

        for (int i = 0; i < param1; i++) {
            for (int j = 0; j < param2; j++) {
                int bx = x, by = y, bz = z;
                switch (constant) {
                    case 1:
                        by = y + i;
                        bz = z + j;
                        break;
                    case 2:
                        bx = x + i;
                        bz = z + j;
                        break;
                    case 3:
                        bx = x + i;
                        by = y + j;
                        break;
                }
                Block block = world.getBlockAt(bx, by, bz);
                
                if (addingGlass) {
                    if (block.getType() == Material.AIR || block.getType() == Material.STAINED_GLASS) {
                        block.setType(material);
                        block.setData(data);
                    }
                } else {
                    if (block.getType() == Material.STAINED_GLASS) {
                        block.setType(Material.AIR);
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

        int x = playerLoc.getBlockX();
        int y = playerLoc.getBlockY();
        int z = playerLoc.getBlockZ();
        
        Block blockAt = playerLoc.getWorld().getBlockAt(x, y, z);
        Block blockBelow = playerLoc.getWorld().getBlockAt(x, y - 1, z);

        if (blockAt.getType() == Material.PORTAL || blockBelow.getType() == Material.PORTAL) {
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
