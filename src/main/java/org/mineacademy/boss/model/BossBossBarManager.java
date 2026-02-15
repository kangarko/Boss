package org.mineacademy.boss.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;

import net.kyori.adventure.bossbar.BossBar;

/**
 * Manages BossBar display for Boss entities, showing health progress to nearby players.
 */
public final class BossBossBarManager {

	/**
	 * Active BossBar instances keyed by boss entity UUID.
	 */
	private static final Map<UUID, BossBar> activeBars = new HashMap<>();

	/**
	 * Tracks which players are currently viewing each boss entity's bar.
	 */
	private static final Map<UUID, Set<UUID>> viewers = new HashMap<>();

	private BossBossBarManager() {
	}

	/**
	 * Shows or updates the BossBar for the given boss entity to the given player.
	 *
	 * @param boss   the Boss configuration
	 * @param entity the living boss entity
	 * @param player the player to show the bar to
	 */
	public static void showOrUpdate(final Boss boss, final LivingEntity entity, final Player player) {
		if (!boss.isBossBarEnabled())
			return;

		final UUID entityId = entity.getUniqueId();
		final float progress = calculateProgress(entity);
		final SimpleComponent title = buildTitle(boss, entity, player);

		BossBar bar = activeBars.get(entityId);

		if (bar == null) {
			bar = BossBar.bossBar(
					title.toAdventure(Platform.toPlayer(player)),
					progress,
					boss.getBossBarColor(),
					boss.getBossBarStyle());

			activeBars.put(entityId, bar);
			viewers.put(entityId, new HashSet<>());
		} else {
			bar.name(title.toAdventure(Platform.toPlayer(player)));
			bar.progress(progress);
		}

		final Set<UUID> entityViewers = viewers.get(entityId);

		if (!entityViewers.contains(player.getUniqueId())) {
			final FoundationPlayer audience = Platform.toPlayer(player);

			audience.showBossBar(bar);
			entityViewers.add(player.getUniqueId());
		}
	}

	/**
	 * Removes the BossBar for the given boss entity from all viewers.
	 *
	 * @param entityId the UUID of the boss entity
	 */
	public static void removeAll(final UUID entityId) {
		final BossBar bar = activeBars.remove(entityId);
		final Set<UUID> entityViewers = viewers.remove(entityId);

		if (bar != null && entityViewers != null)
			for (final UUID playerId : entityViewers) {
				final FoundationPlayer audience = Platform.getPlayer(playerId);

				if (audience != null && audience.isPlayerOnline())
					audience.hideBossBar(bar);
			}
	}

	/**
	 * Removes all active BossBars from all viewers. Called on plugin disable/reload.
	 */
	public static void clearAll() {
		for (final Map.Entry<UUID, BossBar> entry : activeBars.entrySet()) {
			final BossBar bar = entry.getValue();
			final Set<UUID> entityViewers = viewers.get(entry.getKey());

			if (entityViewers != null)
				for (final UUID playerId : entityViewers) {
					final FoundationPlayer audience = Platform.getPlayer(playerId);

					if (audience != null && audience.isPlayerOnline())
						audience.hideBossBar(bar);
				}
		}

		activeBars.clear();
		viewers.clear();
	}

	/**
	 * Removes distant players from the BossBar of the given boss entity.
	 *
	 * @param boss   the Boss configuration
	 * @param entity the living boss entity
	 */
	public static void removeDistant(final Boss boss, final LivingEntity entity) {
		final UUID entityId = entity.getUniqueId();
		final BossBar bar = activeBars.get(entityId);
		final Set<UUID> entityViewers = viewers.get(entityId);

		if (bar == null || entityViewers == null)
			return;

		final double radius = boss.getBossBarRadius();

		for (final Iterator<UUID> it = entityViewers.iterator(); it.hasNext();) {
			final UUID playerId = it.next();
			final FoundationPlayer audience = Platform.getPlayer(playerId);

			if (audience == null || !audience.isPlayerOnline()) {
				it.remove();

				continue;
			}

			final Player player = audience.getPlayer();

			if (player == null || !player.getWorld().equals(entity.getWorld())
					|| player.getLocation().distance(entity.getLocation()) > radius) {
				audience.hideBossBar(bar);
				it.remove();
			}
		}

		if (entityViewers.isEmpty()) {
			activeBars.remove(entityId);
			viewers.remove(entityId);
		}
	}

	/*
	 * Calculates the progress (0.0-1.0) from current/max health.
	 */
	private static float calculateProgress(final LivingEntity entity) {
		final double current = Remain.getHealth(entity);
		final double max = Remain.getMaxHealth(entity);

		return max > 0 ? (float) MathUtil.range(current / max, 0D, 1D) : 0F;
	}

	/*
	 * Builds the title component with variable replacement.
	 */
	private static SimpleComponent buildTitle(final Boss boss, final LivingEntity entity, final Player player) {
		String title = boss.getBossBarTitle();

		title = title
				.replace("{health}", MathUtil.formatTwoDigits(Remain.getHealth(entity)))
				.replace("{max_health}", MathUtil.formatTwoDigits(Remain.getMaxHealth(entity)));

		return boss.replaceVariables(SimpleComponent.fromMiniAmpersand(title), entity, player);
	}
}
