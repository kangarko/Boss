package org.mineacademy.boss.goal;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.remain.Remain;

public class GoalManagerCheck {

    /**
     * If the Goal API is available.
     */
    @Getter
    private static final boolean available;

    static {
        boolean isAvailable = false;

        try {
            Class.forName("com.destroystokyo.paper.entity.ai.Goal");

            Bukkit.class.getMethod("getMobGoals");

            isAvailable = true;
        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            // Goal API is not available
        }

        available = isAvailable;

        if(!available && Remain.isPaper() && MinecraftVersion.atLeast(MinecraftVersion.V.v1_15))
            Common.warning("Your server is running Paper 1.15.2+, but the Native Goal API is not available. This feature won't be enabled.");
    }
}