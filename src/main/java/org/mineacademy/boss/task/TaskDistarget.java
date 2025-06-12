package org.mineacademy.boss.task;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.hook.WorldGuardHook;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleRunnable;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

/**
 * The task to stop targeting when target player enters a WG region.
 */
public final class TaskDistarget extends SimpleRunnable {

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		for (final World world : Bukkit.getWorlds())
			for (final LivingEntity entity : world.getLivingEntities())
				if (HookManager.isCitizensLoaded()) {
					final NPCRegistry registry = CitizensAPI.getNPCRegistry();

					if (registry != null) {
						final NPC npc = registry.getNPC(entity);

						if (npc != null) {
							final Navigator gps = npc.getNavigator();
							final EntityTarget target = gps.getEntityTarget();

							if (target != null && target.getTarget() != null && !WorldGuardHook.canTarget(target.getTarget().getLocation()))
								gps.cancelNavigation();
						}
					}

				} else if (entity instanceof Creature) {
					final Creature creature = (Creature) entity;
					final LivingEntity target = creature.getTarget();

					if (target != null && !WorldGuardHook.canTarget(target.getLocation()))
						creature.setTarget(null);
				}
	}
}
