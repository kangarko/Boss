package org.mineacademy.boss.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.BoxedMessage;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class BossUtil {

	private final Pattern NAME_PATTERN = Pattern.compile("\\W", Pattern.CASE_INSENSITIVE);

	/**
	 * Return true if the given name can be used to create file name for bosses
	 *
	 * @param bossName
	 * @return
	 */
	public boolean isValidFileName(final String bossName) {
		return !NAME_PATTERN.matcher(bossName).find();
	}

	/**
	 * Evaluates if the target can be affected by this skill.
	 *
	 * @param player the player
	 * @return if a skill can be applied
	 */
	public boolean canBossTarget(final Player player) {
		if (player == null)
			return false;

		if (player.isDead() || !player.isValid())
			return false;

		if (PlayerUtil.isVanished(player))
			return false;

		try {
			if (player.isInvulnerable())
				return false;
		} catch (final NoSuchMethodError ex) {
		}

		final GameMode gamemode = player.getGameMode();

		return gamemode == GameMode.SURVIVAL || gamemode == GameMode.ADVENTURE;
	}

	/**
	 * If Boss has the option enabled, set the target for him in the radius set in settings.yml
	 *
	 * @param boss
	 * @param spawnedBoss
	 */
	public static void setBossTargetInRadius(Boss boss, Entity spawnedBoss) {
		if (!(spawnedBoss instanceof Creature))
			return;

		if (!(boolean) boss.getSettings().getSpecificSetting(BossSpecificSetting.AUTO_TARGET))
			return;

		final int radius = Settings.Fight.Target.RADIUS;
		final Creature bossEntity = (Creature) spawnedBoss;

		if (bossEntity.isDead() || !bossEntity.isValid())
			return;

		// Ignore if entity already has a target
		if (bossEntity.getTarget() != null)
			return;

		final LivingEntity oldTarget = bossEntity.getTarget();
		final List<LivingEntity> possibleTargets = new ArrayList<>();

		for (final Entity nearby : bossEntity.getNearbyEntities(radius, radius - 1, radius)) {
			if (oldTarget != null && nearby.getUniqueId().equals(oldTarget.getUniqueId()))
				continue;

			if (nearby instanceof Creature && Settings.Fight.Target.CREATURES) {
				final Boss nearbyBoss = BossPlugin.getBossManager().findBoss(nearby);

				if (nearbyBoss != null && nearbyBoss.getName().equals(boss.getName()))
					continue;

				possibleTargets.add((Creature) nearby);
			}

			if (nearby instanceof Player && BossUtil.canBossTarget((Player) nearby) && Settings.Fight.Target.PLAYERS)
				possibleTargets.add((Player) nearby);

		}

		if (!possibleTargets.isEmpty())
			bossEntity.setTarget(RandomUtil.nextItem(possibleTargets));
	}

	public boolean checkSameWorld(final Location first, final Location second) {
		return first.getWorld() == null || second.getWorld() == null || first.getWorld().getName().equals(second.getWorld().getName());
	}

	public void runCommands(final Player target, final SpawnedBoss spawned, final AutoUpdateMap<String, Double> commands) {
		final LivingEntity en = spawned.getEntity();
		final Boss boss = spawned.getBoss();

		Debugger.debug("death-commands", "Running command for boss " + boss.getName() + ". Commands: " + commands.keySet());

		for (final Map.Entry<String, Double> e : commands.entrySet()) {
			if (!RandomUtil.chanceD(e.getValue())) {
				Debugger.debug("death-commands", "\tSkipping " + e.getKey() + ", its chance did not match");

				continue;
			}

			String command = e.getKey();
			if (command.startsWith("/") && !command.startsWith("//"))
				command = command.substring(1);

			final String killer = Common.getOrEmpty(target != null ? target.getName() : en.getKiller() != null ? en.getKiller().getName() : "");

			if (command.contains("{killer}") && killer.isEmpty()) {
				Debugger.debug("death-commands", "\tSkipping " + e.getKey() + ", contains {killer} but " + boss.getName() + " had none");

				continue;
			}

			final String translatedCommand = Common.colorize(
					command
							.replace("{boss}", boss.getSettings().getCustomName())
							.replace("{killer}", killer)
							.replace("{player}", target == null ? killer : target.getName())
							.replace("{world}", en.getWorld().getName())
							.replace("{location}", Common.shortLocation(en.getLocation())));

			if (translatedCommand.startsWith("say ") || translatedCommand.startsWith("broadcast ")) {
				final String[] copy = toBoxed0(translatedCommand.replaceAll("^(say|broadcast) ", ""));

				BoxedMessage.broadcast(copy);
			} else if (translatedCommand.startsWith("tell ")) {
				final String[] copy = toBoxed0(translatedCommand.replaceFirst("tell " + (target != null ? target.getName() : en.getKiller() != null ? en.getKiller().getName() + " " : ""), ""));

				Common.runLater(1, () -> {
					// Send to nearby players, likely the one who caused spawning by egg
					if (en.getKiller() == null && target == null) {
						for (final Entity nearby : en.getNearbyEntities(10, 10, 10))
							if (nearby instanceof Player)
								BoxedMessage.tell(nearby, copy);

					} else
						BoxedMessage.tell(target != null ? target : en.getKiller(), copy);
				});

			} else if (target == null) { // Run as console
				Debugger.debug("death-commands", "\tRunning " + e.getKey() + " as console");

				Common.dispatchCommand(null, translatedCommand);
			} else {
				Debugger.debug("death-commands", "\tRunning " + e.getKey() + " as player " + target.getName());

				if (Settings.Death.RUN_PLAYER_COMMANDS_AS_PLAYER)
					Common.dispatchCommandAsPlayer(target, translatedCommand);
				else
					Common.dispatchCommand(null, translatedCommand);
			}
		}
	}

	private String[] toBoxed0(final String message) {
		final String[] copy = message.split("\n");

		for (int i = 0; i < copy.length; i++)
			copy[i] = "<center>" + copy[i];

		return copy;
	}
}
