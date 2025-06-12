package org.mineacademy.boss.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.platform.Platform;

import dev.rosewood.rosestacker.event.EntityStackEvent;
import dev.rosewood.rosestacker.stack.StackedEntity;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.events.StackMergeEvent;

/**
 * A common listener for all third party plugin integration
 */
public final class ThirdPartiesListener {

	/**
	 * Register all compatible hooks
	 */
	public static void registerEvents() {
		if (Platform.isPluginInstalled("StackMob")) {
			Platform.registerEvents(new StackMobListener());

			Common.log("&fHooked into StackMob to prevent stacking of Bosses.");
		}

		if (Platform.isPluginInstalled("RoseStacker")) {
			Platform.registerEvents(new RoseStackerListener());

			Common.log("&fHooked into RoseStacker to prevent stacking of Bosses.");
		}

		if (Platform.isPluginInstalled("WildStacker")) {
			Platform.registerEvents(new WildStackerListener());

			Common.log("&fHooked into WildStacker to prevent stacking of Bosses.");
		}
	}
}

/**
 * The StackMob listener.
 */
final class StackMobListener implements Listener {

	/**
	 * Prevent stacking of Bosses (breaks our plugin).
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityStack(StackMergeEvent event) {
		final StackEntity stacked = event.getStackEntity();
		final StackEntity nearby = event.getNearbyStackEntity();

		if (stacked != null && Boss.findBoss(stacked.getEntity()) != null) {
			event.setCancelled(true);

			return;
		}

		if (nearby != null && Boss.findBoss(nearby.getEntity()) != null)
			event.setCancelled(true);
	}
}

/**
 * The RoseStacker listener.
 */
final class RoseStackerListener implements Listener {

	/**
	 * Prevent stacking of Bosses (breaks our plugin).
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityStack(EntityStackEvent event) {
		final StackedEntity stacked = event.getStack();

		if (Boss.findBoss(stacked.getEntity()) != null) {
			event.setCancelled(true);

			return;
		}

		for (final StackedEntity target : event.getTargets()) {
			if (Boss.findBoss(target.getEntity()) != null) {
				event.setCancelled(true);

				return;
			}
		}
	}
}

/**
 * The WildStacker listener.
 */
final class WildStackerListener implements Listener {

	/**
	 * Prevent stacking of Bosses (breaks our plugin).
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityStack(com.bgsoftware.wildstacker.api.events.EntityStackEvent event) {
		final com.bgsoftware.wildstacker.api.objects.StackedEntity stacked = event.getEntity();
		final com.bgsoftware.wildstacker.api.objects.StackedEntity target = event.getTarget();

		if (stacked != null && Boss.findBoss(stacked.getLivingEntity()) != null) {
			event.setCancelled(true);

			return;
		}

		if (target != null && Boss.findBoss(target.getLivingEntity()) != null)
			event.setCancelled(true);
	}
}
