package com.samleighton.sethomestwo.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.jetbrains.annotations.Nullable;

public class TeleportSafetyUtil {

    /**
     * A destination is safe when the feet and head blocks are passable and not
     * liquid, the block below is solid, and none of feet/head/below are burn hazards.
     */
    public static boolean isSafe(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);

        if (!feet.isPassable() || !head.isPassable()) return false;
        if (feet.isLiquid() || head.isLiquid()) return false;
        if (!below.getType().isSolid()) return false;
        return !isBurnHazard(feet) && !isBurnHazard(head) && !isBurnHazard(below);
    }

    private static boolean isBurnHazard(Block block) {
        Material material = block.getType();
        if (material == Material.LAVA
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.MAGMA_BLOCK) {
            return true;
        }
        if (material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE) {
            // Only a lit campfire burns; an extinguished one is safe to stand on.
            return block.getBlockData() instanceof Lightable && ((Lightable) block.getBlockData()).isLit();
        }
        return false;
    }

    /**
     * Returns the destination itself if safe, otherwise the nearest safe spot:
     * same X/Z column up to 5 blocks up/down first, then a 3-block horizontal
     * radius at each height. Returns null when nothing safe is found.
     */
    @Nullable
    public static Location findSafeLocation(Location destination) {
        // getBlock() below forces the chunk to load; never evaluate unloaded air.
        if (isSafe(destination)) return destination;

        // Phase 1: same column only, nearest height first (+1,-1,+2,-2,...,+5,-5).
        for (int dy = 1; dy <= 5; dy++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                int yOffset = dy * sign;
                Location candidate = destination.clone().add(0, yOffset, 0);
                if (isSafe(candidate)) {
                    return centered(candidate);
                }
            }
        }

        // Phase 2: horizontal radius (skipping the column already checked) at
        // each height, nearest height first (0,+1,-1,...,+5,-5).
        for (int dy = 0; dy <= 5; dy++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (dy == 0 && sign < 0) continue;
                int yOffset = dy * sign;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        Location candidate = destination.clone().add(dx, yOffset, dz);
                        if (isSafe(candidate)) {
                            return centered(candidate);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Centers a candidate on its block so the player does not clip a corner.
     */
    private static Location centered(Location candidate) {
        candidate.setX(candidate.getBlockX() + 0.5);
        candidate.setZ(candidate.getBlockZ() + 0.5);
        candidate.setY(candidate.getBlockY());
        return candidate;
    }
}
