package org.mineacademy.boss.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.model.BossManager;

import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.events.StackMergeEvent;

public class StackMobListener implements Listener {

	@EventHandler
	public final void onEntityStack(StackMergeEvent event) {
		final StackEntity stacked = event.getStackEntity();
		final StackEntity nearby = event.getNearbyStackEntity();

		if (stacked != null) {
			final BossManager manager = BossPlugin.getBossManager();

			final SpawnedBoss boss = manager.findBoss(stacked.getEntity().getLocation());
			final SpawnedBoss bossSecond = manager.findBoss(nearby.getEntity().getLocation());

			if (boss != null || bossSecond != null)
				event.setCancelled(true);
		}
	}
}
