package org.mineacademy.boss.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.impl.SimpleBoss;
import org.mineacademy.boss.model.BossManager;

import lombok.experimental.UtilityClass;

/**
 * Handles updating settings for already spawned bosses
 */

@UtilityClass
public class BossUpdateUtil {

	public void updateAll() {
		for (final World world : Bukkit.getWorlds())
			updateIn(world);
	}

	public void updateIn(final World world) {
		final BossManager manager = BossPlugin.getBossManager();

		for (final LivingEntity en : world.getLivingEntities()) {
			final Boss boss = manager.findBoss(en);

			if (boss != null)
				((SimpleBoss) boss).reapplyProperties(en);
		}
	}
}
