package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

public class EffectPad {
    private final int minX, maxX, minY, maxY, minZ, maxZ;
    private final String worldName;
    private final PotionEffectType type;
    private final int amplifier;
    private final int duration;

    public EffectPad(Location loc1, Location loc2, PotionEffectType type, int amplifier, int duration) {
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        this.worldName = loc1.getWorld().getName();
        this.type = type;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public boolean contains(String world, int x, int y, int z) {
        return world.equals(worldName) &&
                x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDuration() {
        return duration;
    }
}
