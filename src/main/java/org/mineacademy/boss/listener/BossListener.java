package org.mineacademy.boss.listener;

import java.util.function.Consumer;

import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.Common;

import lombok.NonNull;

/**
 * A listener class which provides helper methods to our main listener classes.
 */
abstract class BossListener implements Listener {

	/**
	 * Runs the callback when the entity is a valid Boss.
	 *
	 * @param entity
	 * @param callback
	 */
	protected final void runIfBoss(@NonNull Entity entity, Consumer<SpawnedBoss> callback) {
		try {
			final SpawnedBoss spawnedBoss = Boss.findBoss(entity);

			if (spawnedBoss != null)
				callback.accept(spawnedBoss);

		} catch (final Exception ex) {
			Common.error(ex,
					"Failed to execute event for entity " + entity,
					"Error: {error}");
		}
	}

	/**
	 * Runs the callback when the item is a valid Boss.
	 *
	 * @param item
	 * @param callback
	 */
	protected final void runIfBoss(ItemStack item, Consumer<Boss> callback) {
		final Boss boss = Boss.findBoss(item);

		if (boss != null)
			callback.accept(boss);
	}
}
