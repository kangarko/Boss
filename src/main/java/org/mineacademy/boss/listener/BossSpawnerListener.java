package org.mineacademy.boss.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.menu.MenuSpawner;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.storage.SimpleSpawnerData;
import org.mineacademy.boss.tool.SpawnerTool;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

public final class BossSpawnerListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMenuOpen(final PlayerInteractEvent e) {
		final Block block = e.getClickedBlock();

		if (block == null || e.getAction() == null)
			return;

		if (!Remain.isInteractEventPrimaryHand(e) || !e.getAction().toString().contains("RIGHT"))
			return;

		if (!block.hasMetadata(SpawnerTool.METADATA))
			return;

		if (!Valid.checkPermission(e.getPlayer(), Permissions.Use.SPAWNER))
			return;

		e.setCancelled(true);
		new MenuSpawner((CreatureSpawner) block.getState()).displayTo(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpawn(final SpawnerSpawnEvent e) {
		final CreatureSpawner spawner = e.getSpawner();

		if (spawner == null || !spawner.hasMetadata(SpawnerTool.METADATA))
			return;

		if (e.isCancelled()) {
			Debugger.debug("spawner", "Something cancelled Boss spawner at " + Common.shortLocation(e.getLocation()) + " from spawning");

			return;
		}

		final String bossName = spawner.getMetadata(SpawnerTool.METADATA).get(0).asString();

		if (SpawnerTool.METADATA_UNSPECIFIED.equals(bossName)) {
			Debugger.debug("spawner", "Preventing unset Boss spawner at " + Common.shortLocation(e.getLocation()) + " from spawning");
			e.setCancelled(true);

			return;
		}

		final Boss boss = BossPlugin.getBossManager().findBoss(bossName);

		if (boss == null) {
			Common.log("Boss spawner at " + Common.shortLocation(e.getLocation()) + " has Boss '" + bossName + "' that is not installed on the server, skipping..");

			e.setCancelled(true);
			return;
		}

		Debugger.debug("spawner", "Spawning '" + boss.getName() + "' at " + Common.shortLocation(e.getLocation()) + " from Boss spawner. Spawner's delay is " + spawner.getDelay());

		// Custom max limit
		int maxLimit = 16;

		try {
			maxLimit = spawner.getMaxNearbyEntities();
		} catch (final NoSuchMethodError ex) {
			// outdated MC
		}

		// Custom radius
		int limitRadius = 4;

		try {
			limitRadius = spawner.getSpawnRange();
		} catch (final NoSuchMethodError ex) {
			// outdated MC
		}

		int spawnedBosses = 0;

		for (final SpawnedBoss spawnedBoss : BossPlugin.getBossManager().findBosses(spawner.getLocation(), limitRadius))
			if (spawnedBoss.getBoss().equals(boss))
				spawnedBosses++;

		Debugger.debug("spawner", "Boss spawner at " + Common.shortLocation(e.getLocation()) + " has " + spawnedBosses + " Bosses nearby with max nearby limit " + maxLimit);

		// Custom limit end

		e.setCancelled(true);

		if (spawnedBosses < maxLimit)
			if (Settings.Limits.APPLY_SPAWNERS)
				BossConditionedSpawnUtil.spawnConditioned(boss, e.getLocation(), BossSpawnReason.SPAWNER);
			else
				boss.spawn(e.getLocation(), BossSpawnReason.SPAWNER);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(final BlockBreakEvent e) {
		final SimpleSpawnerData data = SimpleSpawnerData.$();

		final Block block = e.getBlock();
		final Location loc = block.getLocation();
		final Player player = e.getPlayer();

		if (data.hasSpawner(loc))
			if (!Valid.checkPermission(player, Permissions.Use.SPAWNER)) {
				Debugger.debug("spawner", player.getName() + " lacks permission to break Boss spawner at " + Common.shortLocation(loc));

				e.setCancelled(true);
			} else {
				Debugger.debug("spawner", "Removing Boss spawner at " + Common.shortLocation(loc));

				data.removeSpawner(loc);

				if (block.hasMetadata(SpawnerTool.METADATA))
					block.removeMetadata(SpawnerTool.METADATA, SimplePlugin.getInstance());
			}
	}
}
