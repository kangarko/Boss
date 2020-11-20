package org.mineacademy.boss.model.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.RandomNoRepeatPicker;

/**
 * A task that launches Boss' skills.
 */
public final class BossSkillTask implements Runnable {

	private final Map<UUID, Map<String, Long>> nextSkillsRun = new HashMap<>();

	private final RandomNoRepeatPicker<BossSkill> skillPicker = RandomNoRepeatPicker.newPicker(BossSkill.class);

	@Override
	public void run() {
		final BossManager manager = BossPlugin.getBossManager();
		final long now = TimeUtil.currentTimeTicks();

		for (final World world : Bukkit.getWorlds())
			bossLoop:
			for (final SpawnedBoss spawned : manager.findBosses(world)) {
				final Boss boss = spawned.getBoss();
				final LivingEntity entity = spawned.getEntity();

				// Boss has no skills
				if (boss.getSkills().isEmpty())
					continue;

				// Load up the random no repeat picker
				skillPicker.setItems(boss.getSkills());

				BossSkill selectedSkill = null;

				do {
					selectedSkill = skillPicker.pickRandom();

					if (selectedSkill == null)
						continue bossLoop;

					else {
						final long futureTime = nextSkillsRun.getOrDefault(entity.getUniqueId(), new HashMap<>()).getOrDefault(selectedSkill.getName(), -1L);
						Debugger.debug("skills", "Boss '" + boss.getName() + "' had last skill run: " + futureTime + " vs now " + now + ", difference: " + (futureTime - now));

						if (futureTime != -1 && now < futureTime)
							continue;

						boolean success;

						try {
							success = selectedSkill.execute(spawned);

						} catch (final Throwable t) {
							Common.throwError(t,
									"Error running skill '" + selectedSkill.getName() + "'!",
									"Boss: " + spawned.getBoss().getName(),
									"Error: " + t.getMessage());

							return;
						}

						if (success) {
							final long next = selectedSkill.getDelay().getDelay();

							Debugger.debug("skills", "Ran skill '" + selectedSkill.getName() + "' for '" + boss.getName() + "' boss and scheduled another in " + next + " ticks");

							final Map<String, Long> nextSkillTime = nextSkillsRun.getOrDefault(entity.getUniqueId(), new HashMap<>());

							nextSkillTime.put(selectedSkill.getName(), now + next);
							nextSkillsRun.put(entity.getUniqueId(), nextSkillTime);
							break;
						}
					}

				} while (true);
			}
	}
}
