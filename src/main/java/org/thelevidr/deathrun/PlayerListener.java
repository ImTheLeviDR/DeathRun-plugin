package org.thelevidr.deathrun;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerListener implements Listener {
    private final ConfigManager configManager;
    private final GameManager gameManager;
    private final MapRecognizer mapRecognizer;
    private final Map<String, Integer> playerPadCache = new HashMap<>();

    public PlayerListener(ConfigManager configManager, GameManager gameManager, MapRecognizer mapRecognizer) {
        this.configManager = configManager;
        this.gameManager = gameManager;
        this.mapRecognizer = mapRecognizer;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        
        ItemStack resetItem = new ItemStack(Material.INK_SACK, 1, (short) 1);
        ItemMeta meta = resetItem.getItemMeta();
        meta.setDisplayName("§cReset");
        resetItem.setItemMeta(meta);
        
        player.getInventory().setItem(0, resetItem);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT")) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.INK_SACK && item.getDurability() == 1) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§cReset")) {
                    Player player = event.getPlayer();
                    Location spawn = mapRecognizer.getSpawnLocation(player.getWorld());
                    if (spawn != null) {
                        player.teleport(spawn);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        if (gameManager.isGameRunning()) {
            if (gameManager.checkFinish(player)) {
                return;
            }
        }

        List<EffectPad> effectPads = configManager.getEffectPads();
        if (effectPads.isEmpty()) return;

        Location toLoc = event.getTo();
        String world = toLoc.getWorld().getName();
        
        if (toLoc.getBlock().isLiquid()) {
            Location spawn = mapRecognizer.getSpawnLocation(toLoc.getWorld());
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.sendTitle("§cDIED", "§7You drowned", 10, 30, 10);
            return;
        }
        

        
        int x = toLoc.getBlockX();
        int y = toLoc.getBlockY() - 1;
        int z = toLoc.getBlockZ();
        
        int toPadIndex = -1;
        for (int i = 0; i < effectPads.size(); i++) {
            if (effectPads.get(i).contains(world, x, y, z)) {
                toPadIndex = i;
                break;
            }
        }
        
        Integer cachedPad = playerPadCache.get(playerId);
        
        if (toPadIndex >= 0 && (cachedPad == null || cachedPad != toPadIndex)) {
            EffectPad pad = effectPads.get(toPadIndex);
            player.addPotionEffect(new PotionEffect(pad.getType(), pad.getDuration() * 20, pad.getAmplifier() - 1), true);
            playerPadCache.put(playerId, toPadIndex);
        } else if (toPadIndex < 0) {
            playerPadCache.remove(playerId);
        }
    }
    
    private int getPadIndex(List<EffectPad> pads, String world, int x, int y, int z) {
        for (int i = 0; i < pads.size(); i++) {
            if (pads.get(i).contains(world, x, y, z)) {
                return i;
            }
        }
        return -1;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (!gameManager.isGameRunning()) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            Location spawn = mapRecognizer.getSpawnLocation(player.getWorld());
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.setFireTicks(0);
            event.setCancelled(true);
            player.sendTitle("§cDIED", "§7You died to fire", 10, 30, 10);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            if (event.getDamage() > 20) {
                Location spawn = mapRecognizer.getSpawnLocation(player.getWorld());
                if (spawn != null) {
                    player.teleport(spawn);
                }
                player.sendTitle("§cDIED", "§7You fell to your death", 10, 30, 10);
            }
        }

        if (cause == EntityDamageEvent.DamageCause.DROWNING || cause == EntityDamageEvent.DamageCause.CONTACT) {
            Location loc = player.getLocation();
            boolean inWater = loc.getBlock().isLiquid();
            if (inWater || loc.getBlockY() < 0) {
                Location spawn = mapRecognizer.getSpawnLocation(player.getWorld());
                if (spawn != null) {
                    player.teleport(spawn);
                }
                event.setCancelled(true);
                player.sendTitle("§cDIED", "§7You drowned", 10, 30, 10);
            }
        }
    }
}
