package org.mineacademy.boss.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossBarTracker {

	private static final Map<UUID, BossBar> entityBars  = new HashMap<>();
	private static final Map<UUID, Set<UUID>> barViewers = new HashMap<>();
	private static final Set<UUID> tickedThisCycle      = new HashSet<>();

	public static void createBar(Boss boss, LivingEntity entity) {
		if (!MinecraftVersion.atLeast(V.v1_9))
			return;

		final UUID entityId = entity.getUniqueId();

		if (entityBars.containsKey(entityId))
			removeBarById(entityId);

		final BossBar bar = BossBar.bossBar(
				formatName(boss, entity, entity.getHealth()),
				1.0f,
				parseColor(Settings.Fighting.BossBar.COLOR),
				parseOverlay(Settings.Fighting.BossBar.STYLE));

		entityBars.put(entityId, bar);
		barViewers.put(entityId, new HashSet<>());
	}

	public static void updateBar(Boss boss, LivingEntity entity, double health) {
		final BossBar bar = entityBars.get(entity.getUniqueId());

		if (bar == null)
			return;

		updateProgress(bar, health, entity.getMaxHealth());
		bar.name(formatName(boss, entity, health));
	}

	public static void tickViewers(SpawnedBoss spawned) {
		final LivingEntity entity = spawned.getEntity();
		final UUID entityId = entity.getUniqueId();
		final BossBar bar = entityBars.get(entityId);

		if (bar == null)
			return;

		tickedThisCycle.add(entityId);

		updateProgress(bar, entity.getHealth(), entity.getMaxHealth());
		bar.name(formatName(spawned.getBoss(), entity, entity.getHealth()));

		final Set<UUID> currentViewers = barViewers.get(entityId);
		final int radius = Settings.Fighting.BossBar.RADIUS;
		final Set<UUID> nowNearby = new HashSet<>();

		for (final Entity nearby : Remain.getNearbyEntities(entity.getLocation(), radius))
			if (nearby instanceof Player && Boss.canTarget(nearby))
				nowNearby.add(nearby.getUniqueId());

		for (final UUID playerId : nowNearby)
			if (!currentViewers.contains(playerId)) {
				final Player player = Bukkit.getPlayer(playerId);

				if (player != null)
					Platform.toPlayer(player).showBossBar(bar);
			}

		for (final Iterator<UUID> it = currentViewers.iterator(); it.hasNext();) {
			final UUID viewerId = it.next();

			if (!nowNearby.contains(viewerId)) {
				hideBarFromPlayer(viewerId, bar);

				it.remove();
			}
		}

		currentViewers.addAll(nowNearby);
	}

	public static void removeBar(Entity entity) {
		removeBarById(entity.getUniqueId());
	}

	public static void removeViewer(Player player) {
		final UUID playerId = player.getUniqueId();

		for (final Map.Entry<UUID, Set<UUID>> entry : barViewers.entrySet())
			if (entry.getValue().remove(playerId)) {
				final BossBar bar = entityBars.get(entry.getKey());

				if (bar != null)
					Platform.toPlayer(player).hideBossBar(bar);
			}
	}

	public static void cleanupStale() {
		for (final Iterator<Map.Entry<UUID, BossBar>> it = entityBars.entrySet().iterator(); it.hasNext();) {
			final Map.Entry<UUID, BossBar> entry = it.next();

			if (!tickedThisCycle.contains(entry.getKey())) {
				hideBarFromAllViewers(entry.getKey(), entry.getValue());

				it.remove();
			}
		}

		tickedThisCycle.clear();
	}

	private static void removeBarById(UUID entityId) {
		final BossBar bar = entityBars.remove(entityId);

		if (bar == null)
			return;

		hideBarFromAllViewers(entityId, bar);
	}

	private static void hideBarFromAllViewers(UUID entityId, BossBar bar) {
		final Set<UUID> viewers = barViewers.remove(entityId);

		if (viewers != null)
			for (final UUID viewerId : viewers)
				hideBarFromPlayer(viewerId, bar);
	}

	private static void hideBarFromPlayer(UUID playerId, BossBar bar) {
		final Player player = Bukkit.getPlayer(playerId);

		if (player != null)
			Platform.toPlayer(player).hideBossBar(bar);
	}

	private static void updateProgress(BossBar bar, double health, double maxHealth) {
		bar.progress((float) MathUtil.range(maxHealth > 0 ? health / maxHealth : 0.0, 0.0, 1.0));
	}

	private static BossBar.Color parseColor(String color) {
		try {
			return BossBar.Color.valueOf(color.toUpperCase());

		} catch (final IllegalArgumentException ex) {
			Common.logTimed(60 * 60, "Invalid Boss_Bar color '" + color + "' in settings.yml, using RED. Available: " + java.util.Arrays.toString(BossBar.Color.values()));

			return BossBar.Color.RED;
		}
	}

	private static BossBar.Overlay parseOverlay(String style) {
		try {
			return BossBar.Overlay.valueOf(style.toUpperCase());

		} catch (final IllegalArgumentException ex) {
			Common.logTimed(60 * 60, "Invalid Boss_Bar style '" + style + "' in settings.yml, using PROGRESS. Available: " + java.util.Arrays.toString(BossBar.Overlay.values()));

			return BossBar.Overlay.PROGRESS;
		}
	}

	private static net.kyori.adventure.text.Component formatName(Boss boss, LivingEntity entity, double health) {
		final String format = Settings.Fighting.BossBar.FORMAT
				.replace("{health}", MathUtil.formatTwoDigits(health))
				.replace("{max_health}", MathUtil.formatTwoDigits(entity.getMaxHealth()))
				.replace("{health_percent}", String.valueOf((int) (health / entity.getMaxHealth() * 100)));

		SimpleComponent component = SimpleComponent.fromMiniAmpersand(format);
		component = boss.replaceVariables(component, entity, null);

		return component.toAdventure();
	}
}
