package org.mineacademy.boss.model.task;

import java.awt.Point;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;

/**
 * A task that manages the spawning of the Bosses.
 */
public final class BossTimedSpawnTask extends BukkitRunnable {

	// ------------------------------------------
	// Static
	// ------------------------------------------

	private static final Map<String, Long> lagMap = new TreeMap<>();

	public static void logLag(final String section, final long time) {
		if (lagMap.containsKey(section))
			lagMap.put(section, lagMap.get(section) + time);
		else
			lagMap.put(section, time);
	}

	// ------------------------------------------

	private BossManager manager;

	private final Set<Chunk> activeChunks = new HashSet<>();

	@Getter
	private long lastSpawnTime = 0;

	private boolean firstIterationDone = false;

	/**
	 * Create a timed spawn task to spawn bosses manually
	 */
	public BossTimedSpawnTask() {
		HookManager.addPlaceholder("next_spawn_time", player -> TimeUtil.formatTimeGeneric(Settings.TimedSpawning.DELAY.getTimeSeconds() - (TimeUtil.currentTimeSeconds() - lastSpawnTime) + 1));
	}

	@Override
	public void run() {
		lastSpawnTime = TimeUtil.currentTimeSeconds();

		// Prevent reloading Boss spawning all at once
		if (!firstIterationDone) {
			firstIterationDone = true;

			return;
		}

		if (Remain.getOnlinePlayers().isEmpty())
			return;

		// Load chunks around all online players according to the radius from settings.yml
		loadChunks();

		for (final Chunk chunk : activeChunks) {
			final World world = chunk.getWorld();

			// Generate a random location within chunk
			final int x = RandomUtil.nextInt(16) + (chunk.getX() << 4) - 16;
			final int z = RandomUtil.nextInt(16) + (chunk.getZ() << 4) - 16;

			final Location loc = new Location(world, x, -1 /* Handled below */, z).add(0.5, 0, 0.5);

			// Execute the spawning according to conditions like chances, limits etc.
			// May or may not produce a Boss
			BossConditionedSpawnUtil.spawnTimed(loc);
		}
	}

	private void loadChunks() {
		activeChunks.clear();
		final HashSet<Point> addedChunks = new HashSet<>();

		final int radius = Settings.TimedSpawning.Performance.CHUNK_RADIUS;
		int chunkX, chunkZ;

		for (final World world : Bukkit.getWorlds()) {

			if (!Settings.TimedSpawning.WORLDS.contains(world.getName()))
				continue;

			if (Settings.Limits.Global.ENABLED && getManager().findBosses(world).size() >= Settings.Limits.Global.WORLD)
				continue;

			for (final Player player : world.getPlayers()) {
				chunkX = player.getLocation().getBlockX() >> 4;
				chunkZ = player.getLocation().getBlockZ() >> 4;

				for (int x = chunkX - radius; x <= chunkX + radius; ++x)
					for (int z = chunkZ - radius; z <= chunkZ + radius; ++z)
						if (world.isChunkLoaded(x, z)) {
							final Point point = new Point(x, z);

							if (!addedChunks.contains(point)) {
								addedChunks.add(point);

								activeChunks.add(world.getChunkAt(x, z));
							}
						}
			}
		}

		addedChunks.clear();
	}

	private BossManager getManager() {
		if (manager == null)
			manager = BossPlugin.getBossManager();

		return manager;
	}
}
