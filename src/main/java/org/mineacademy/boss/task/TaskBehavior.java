package org.mineacademy.boss.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.hook.CitizensHook;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossBarTracker;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.boss.spawn.SpawnData;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.RandomNoRepeatPicker;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Task that will run skills and returns back Bosses to regions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskBehavior extends SimpleRunnable {

	/**
	 * The singleton instance
	 */
	@Getter
	private static TaskBehavior instance = new TaskBehavior();

	/**
	 * The picker to pick random skills each time without repeating themselves.
	 */
	private final RandomNoRepeatPicker<BossSkill> skillPicker = RandomNoRepeatPicker.newPicker(BossSkill.class);

	/**
	 * The time when skills were executed the last for the boss entity uuid to respect skill delay.
	 */
	private final Map<UUID, Map<String, Long>> nextSkillsRun = new HashMap<>();

	/**
	 * Private timer for skills
	 */
	private long nowTicks;

	/**
	 * Last time of retarget
	 */
	private long lastRetargetTimeMs;

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		// Server is empty
		if (Remain.getOnlinePlayers().isEmpty())
			return;

		// Reset timer for skills
		this.nowTicks = TimeUtil.getCurrentTimeTicks();

		// Spawn Bosses
		SpawnRule.tick(SpawnData.fromBehaviorTask(), SpawnRuleType.LOCATION_PERIOD, SpawnRuleType.PERIOD, SpawnRuleType.RESPAWN_AFTER_DEATH);

		// Retarget all bosses on interval
		boolean canRetarget = false;
		if (Settings.Fighting.CitizensRetarget.ENABLED && HookManager.isCitizensLoaded()) {
			final long delay = Settings.Fighting.CitizensRetarget.DELAY.getTimeMilliseconds();

			if (this.lastRetargetTimeMs == 0 || (System.currentTimeMillis() - this.lastRetargetTimeMs) > delay) {
				canRetarget = true;

				this.lastRetargetTimeMs = System.currentTimeMillis();
			}
		}

		// Adjust behavior of all existing Bosses
		for (final SpawnedBoss spawned : Boss.findBossesAlive()) {

			// Apply skills
			this.skill(spawned);

			// Update boss bar viewers
			BossBarTracker.tickViewers(spawned);

			// Teleport back if escaped regions
			this.keepInRegions(spawned);

			// Retarget
			if (canRetarget)
				CitizensHook.retarget(spawned);
		}

		BossBarTracker.cleanupStale();
	}

	/*
	 * Execute Boss skills
	 */
	private void skill(SpawnedBoss spawnedBoss) {
		synchronized (this.nextSkillsRun) {

			final Boss boss = spawnedBoss.getBoss();
			final LivingEntity entity = spawnedBoss.getEntity();

			// Boss has no skills
			if (boss.getSkills().isEmpty()) {
				Debugger.debug("skills", "Boss '" + boss.getName() + "' has no skills enabled.");

				return;
			}

			// Load up the random no repeat picker
			this.skillPicker.setItems(boss.getSkills());

			BossSkill selectedSkill = null;

			while ((selectedSkill = this.skillPicker.pickRandom()) != null) {
				final long futureTime = this.nextSkillsRun.getOrDefault(entity.getUniqueId(), new HashMap<>()).getOrDefault(selectedSkill.getName(), -1L);

				if (futureTime != -1 && this.nowTicks < futureTime) {
					Debugger.debug("skills", "Not running " + selectedSkill.getName() + " for '" + boss.getName() + "', it was run recently.");

					continue;
				}

				boolean success;

				try {
					success = selectedSkill.execute(entity);

				} catch (final Throwable t) {
					Common.throwError(t,
							"Error running skill '" + selectedSkill.getName() + "'!",
							"Boss: " + boss.getName(),
							"Error: " + t.getMessage());

					break;
				}

				if (success) {
					final long nextTicks = selectedSkill.getDelay().getRandomTicks();
					Debugger.debug("skills", "Ran skill '" + selectedSkill.getName() + "' for '" + boss.getName() + "' boss and scheduled another in " + nextTicks + " ticks");

					final Map<String, Long> nextSkillTime = this.nextSkillsRun.getOrDefault(entity.getUniqueId(), new HashMap<>());
					nextSkillTime.put(selectedSkill.getName(), this.nowTicks + nextTicks);

					this.nextSkillsRun.put(entity.getUniqueId(), nextSkillTime);

					if (selectedSkill.isStopMoreSkills())
						break;

				} else
					Debugger.debug("skills", "Running " + selectedSkill.getName() + " for '" + boss.getName() + "' returned false, skill did not run.");
			}
		}
	}

	/*
	 * Keep Bosses in regions
	 */
	private void keepInRegions(SpawnedBoss spawnedBoss) {
		final Boss boss = spawnedBoss.getBoss();

		if (!boss.isKeptInSpawnRegion())
			return;

		final LivingEntity entity = spawnedBoss.getEntity();

		// Keep inside region
		final DiskRegion spawnRegion = spawnedBoss.getSpawnRegion();

		if (spawnRegion == null) {
			Debugger.debug("region-keep", "Skipping returning '" + boss.getName() + "' to region, because spawn region does not exist");

			return;
		}

		if (spawnRegion.isWithin(entity.getLocation())) {
			Debugger.debug("region-keep", "Skipping, because boss is already in his spawn region " + spawnRegion.getFileName());

			return;
		}

		// Use configured location
		final BossLocation returnToLocation = boss.getEscapeReturnLocation() == null ? null : BossLocation.findLocation(boss.getEscapeReturnLocation());

		// Or fall back to spawn location if null
		final Location spawnLocation = (returnToLocation != null ? returnToLocation.getLocation() : spawnedBoss.getSpawnLocation()).clone().add(0.5, 1, 0.5);
		Valid.checkNotNull(spawnLocation);

		if (boss.getEscapeReturnLocation() == null)
			Debugger.debug("region-keep", "Returning '" + boss.getName() + "' to region. Escape return location was null, found original spawn location: " + spawnLocation);
		else
			Debugger.debug("region-keep", "Returning '" + boss.getName() + "' to to region using escape return location '" + boss.getEscapeReturnLocation() + "' to: " + spawnLocation);

		// Walk back using Citizens pathfinding when possible, otherwise teleport
		final Entity vehicle = entity.getVehicle();

		if (vehicle == null && HookManager.isCitizensLoaded() && CitizensHook.sendToLocation(spawnedBoss, spawnLocation)) {
			Debugger.debug("region-keep", "Using Citizens pathfinding to walk '" + boss.getName() + "' back to region");

		} else {

			if (vehicle != null) {
				CompMetadata.setTempMetadata(entity, "BossDontPreventVehicleExit");
				entity.leaveVehicle();

				vehicle.setFallDistance(0);
				vehicle.teleport(spawnLocation);
				vehicle.setFallDistance(0);
			}

			entity.setFallDistance(0);
			entity.teleport(spawnLocation);
			entity.setFallDistance(0);
			CompMetadata.removeTempMetadata(entity, "BossDontPreventVehicleExit");

			if (vehicle != null) {
				if (Remain.hasEntityAddPassenger())
					vehicle.addPassenger(entity);
				else
					vehicle.setPassenger(entity);
			}
		}

		// Remove target if outside region
		if (entity instanceof Creature)
			((Creature) entity).setTarget(null);
	}

	/**
	 * Reset the future time for the given skill
	 *
	 * @param skill
	 */
	public void resetFutureTimes(BossSkill skill) {
		synchronized (this.nextSkillsRun) {
			this.nextSkillsRun.values().forEach(map -> map.remove(skill.getName()));
		}
	}
}
