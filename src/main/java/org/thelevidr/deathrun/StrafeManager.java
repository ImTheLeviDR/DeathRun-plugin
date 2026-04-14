package org.thelevidr.deathrun;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StrafeManager implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> justClicked = new HashSet<>();

    private final String LEFT_ACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDhlMzcyZmE2YTQ1NjRlYzI5MDU5NjU5YTM1YTM2ZmVkYWQ2OWZiZWQ1ZTA1OTZlN2EyMTdiOGY2ZTExYyJ9fX0";
    private final String LEFT_COOLDOWN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODZkODVmYzRiYzZjNGNlOTdlNGJiZjljZTI0NGVhNmQxY2M1MDM3YTZkYjlmODlhYWI5YTI5YzI3YmYwNjAifX19";
    private final String RIGHT_ACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWY5NjRkY2ZmOTMwZmFhNDdkNmY5ZDc4YTQxM2QwZTBhOTNlY2Q0ZTlmMzZlYjU4NDI2ZmM4YmRiYjJhZWQ3In19fQ";
    private final String RIGHT_COOLDOWN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjY1ZmE1MTQ4ZDE0ZTNmYzc4Y2Q0MGY3NmFhMDg3ZjQ4MzZkNzNlMzcxM2Y4MWIzYjZlNzNkMmJjODQyOTJkIn19fQ";
    private final String BACK_ACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkZmM2NjMxNDY3NWJkNjNiYzhkMmFmMTdjODVmMmRmY2NlMTk2ZTk3ZWNmNDJiNzhmOTNjZGVmMjUzYjIzIn19fQ==";
    private final String BACK_COOLDOWN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWUzNjEwMTYyMTdmODhmZDQyYmNmOWNiZWQxM2I0N2UxZGNlYjFlNjQ3NWJjZmY4ODg5M2ZiZDZlZTQ0OGNiMyJ9fX0g";

    public StrafeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveStrafes(Player p) {
        p.getInventory().setItem(3, getLeftStrafe());
        p.getInventory().setItem(4, getBackStrafe());
        p.getInventory().setItem(5, getRightStrafe());
    }

    public void removeStrafes(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.SKULL_ITEM && item.getItemMeta() != null) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (name != null && name.contains("Strafe")) {
                    inv.setItem(i, null);
                }
            }
        }
    }

    private ItemStack getLeftStrafe() {
        return createSkull(LEFT_ACTIVE, "§a§lLeft Strafe §7[Right Click]", 1, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack getRightStrafe() {
        return createSkull(RIGHT_ACTIVE, "§a§lRight Strafe §7[Right Click]", 1, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack getBackStrafe() {
        return createSkull(BACK_ACTIVE, "§a§lBack Strafe §7[Right Click]", 1, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack getLeftCooldown(int cooldown) {
        return createSkull(LEFT_COOLDOWN, "§a§lLeft Strafe §7[On Cooldown]", cooldown, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack getRightCooldown(int cooldown) {
        return createSkull(RIGHT_COOLDOWN, "§a§lRight Strafe §7[On Cooldown]", cooldown, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack getBackCooldown(int cooldown) {
        return createSkull(BACK_COOLDOWN, "§a§lBack Strafe §7[On Cooldown]", cooldown, Arrays.asList("", "§7Stop reading this", "§7and go play!", "", "§b► Right click to use!"));
    }

    private ItemStack createSkull(String base64, String name, int amount, java.util.List<String> lore) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, amount, (short) 3);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        try {
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(base64.getBytes()), null);
            profile.getProperties().put("textures", new Property("textures", base64));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        if (held == null || held.getType() != Material.SKULL_ITEM) return;

        ItemMeta meta = held.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = ChatColor.stripColor(meta.getDisplayName());
        if (!name.contains("Strafe")) return;

        Player p = event.getPlayer();
        if (justClicked.contains(p.getUniqueId())) return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            return;
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (name.contains("On Cooldown")) return;

            if (name.contains("Left Strafe")) {
                applyLeftStrafe(p);
                playStrafeSound(p);
                startCooldown(p, 3, "LEFT");
            } else if (name.contains("Right Strafe")) {
                applyRightStrafe(p);
                playStrafeSound(p);
                startCooldown(p, 5, "RIGHT");
            } else if (name.contains("Back Strafe")) {
                applyBackStrafe(p);
                playStrafeSound(p);
                startCooldown(p, 4, "BACK");
            }

            justClicked.add(p.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> justClicked.remove(p.getUniqueId()), 2);
        }
    }

    private void startCooldown(Player p, int slot, String type) {
        int cdSeconds = 20;
        
        ItemStack cdItem;
        if (type.equals("LEFT")) cdItem = getLeftCooldown(cdSeconds);
        else if (type.equals("RIGHT")) cdItem = getRightCooldown(cdSeconds);
        else cdItem = getBackCooldown(cdSeconds);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            p.getInventory().setItem(slot, cdItem);
            p.updateInventory();
        }, 1);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateCooldown(p, slot, type), 20);
    }

    private void updateCooldown(Player p, int slot, String type) {
        ItemStack item = p.getInventory().getItem(slot);
        if (item == null || item.getType() != Material.SKULL_ITEM) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !ChatColor.stripColor(meta.getDisplayName()).contains("On Cooldown")) return;

        int amount = item.getAmount();
        if (amount <= 1) {
            if (type.equals("LEFT")) p.getInventory().setItem(slot, getLeftStrafe());
            else if (type.equals("RIGHT")) p.getInventory().setItem(slot, getRightStrafe());
            else p.getInventory().setItem(slot, getBackStrafe());
            p.updateInventory();
            return;
        }

        item.setAmount(amount - 1);
        p.updateInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateCooldown(p, slot, type), 20);
    }

    private void applyBackStrafe(Player player) {
        Location locVec = player.getLocation().clone();
        locVec = snapYaw(locVec);
        locVec.setPitch(0);
        Vector velocityVector = locVec.getDirection().multiply(-1.0).multiply(1.78);
        velocityVector = velocityVector.setY(0.3);
        player.setVelocity(velocityVector);
    }

    private void applyLeftStrafe(Player player) {
        Location locVec = player.getLocation().clone();
        locVec = snapYaw(locVec);
        locVec.setPitch(0);
        Vector velocityVector = locVec.getDirection();
        double x = velocityVector.getX(), z = velocityVector.getZ();
        double aux = -x;
        x = z;
        z = aux;
        velocityVector.setX(x);
        velocityVector.setZ(z);
        velocityVector = velocityVector.multiply(1.78);
        velocityVector = velocityVector.setY(0.3);
        player.setVelocity(velocityVector);
    }

    private void applyRightStrafe(Player player) {
        Location locVec = player.getLocation().clone();
        locVec = snapYaw(locVec);
        locVec.setPitch(0);
        Vector velocityVector = locVec.getDirection();
        double x = velocityVector.getX(), z = velocityVector.getZ();
        double aux = x;
        x = -z;
        z = aux;
        velocityVector.setX(x);
        velocityVector.setZ(z);
        velocityVector = velocityVector.multiply(1.78);
        velocityVector = velocityVector.setY(0.3);
        player.setVelocity(velocityVector);
    }

    private Location snapYaw(Location location) {
        double rot = (location.getYaw() - 90) % 360;
        if (rot < 0)
            rot += 360.0;

        float settableYaw = -1;
        if (45 <= rot && rot < 135) settableYaw = 90;
        else if (135 <= rot && rot < 225) settableYaw = 180;
        else if (225 <= rot && rot < 315) settableYaw = 270;
        else settableYaw = 0;

        location.setYaw(settableYaw + 90);
        return location;
    }

    private void playStrafeSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6f);
        } catch (Exception e) {
            // Fallback in case ENTITY_CHICKEN_EGG varies
            try {
                player.playSound(player.getLocation(), Sound.valueOf("ITEM_EGG_THROW"), 1, 0.6f);
            } catch (Exception ignored) {}
        }
    }
}
