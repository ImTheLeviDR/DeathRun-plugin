package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerListener implements Listener {
    private final ConfigManager configManager;
    private final GameManager gameManager;
    private final Map<String, Integer> playerPadCache = new HashMap<>();

    public PlayerListener(ConfigManager configManager, GameManager gameManager) {
        this.configManager = configManager;
        this.gameManager = gameManager;
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
}
