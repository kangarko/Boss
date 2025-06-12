package org.mineacademy.boss.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mineacademy.boss.listener.ChunkListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.OfflineRegionScanner;
import org.mineacademy.fo.remain.Remain;

/**
 * Scan chunks and removes saved Bosses from them, see /boss scan.
 */
public final class BossRegionRemover extends OfflineRegionScanner {

	/**
	 * Which Boss names to remove
	 */
	private final IsInList<String> bossNamesToRemove;

	/**
	 * The world to remove Bosses from
	 */
	private final World world;

	/*
	 * The temporary the time of the scan start
	 */
	private long now = 0;

	/*
	 * The temporary amount of entities the last call to remove, removed
	 */
	private int removedAmount = 0;

	/*
	 * Create a new remover
	 */
	private BossRegionRemover(List<String> bossNamesToRemove, World world) {
		this.bossNamesToRemove = bossNamesToRemove == null ? IsInList.fromStar() : IsInList.fromList(bossNamesToRemove);
		this.world = world;
		this.now = System.currentTimeMillis();
	}

	/**
	 * @see org.mineacademy.fo.model.OfflineRegionScanner#onChunkScan(org.bukkit.Chunk)
	 */
	@Override
	protected void onChunkScan(final Chunk chunk) {

		// For logging purposes
		final Set<String> removedNames = new HashSet<>();
		int removedChunkAmount = 0;

		for (final Entity entity : chunk.getEntities()) {
			final SpawnedBoss boss = Boss.findBoss(entity);

			if (boss != null && this.bossNamesToRemove.contains(boss.getBoss().getName())) {
				entity.remove();
				EntityUtil.removeVehiclesAndPassengers(entity);

				removedChunkAmount++;
				removedNames.add(boss.getBoss().getName());

				ChunkListener.removeUnloadedEntity(entity.getUniqueId());
			}
		}

		this.removedAmount += removedChunkAmount;

		if (removedChunkAmount > 0)
			Common.log("Removed Bosses " + removedNames + " (" + removedChunkAmount + ") in chunk x: " + chunk.getX() + " z: " + chunk.getZ());
	}

	/**
	 * @see org.mineacademy.fo.model.OfflineRegionScanner#onScanFinished()
	 */
	@Override
	protected void onScanFinished() {
		Common.log("Removed " + this.removedAmount + " Bosses in total.");

		Common.log("3/4 Saving world ...");
		this.world.save();

		Common.log("4/4 Disabling whitelist ...");
		Bukkit.setWhitelist(false);

		Common.log(Common.chatLine());
		Common.log("Operation finished in " + TimeUtil.formatTimeGeneric((int) (System.currentTimeMillis() - this.now) / 1000));
		Common.log(Common.chatLine());

		this.now = 0;

		ChunkListener.saveToFile();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Start the scan
	 *
	 * @param bossNamesToRemove
	 * @param world
	 */
	public static void launch(List<String> bossNamesToRemove, World world) {
		Common.log("1/4 Kicking all players & enabling whitelist ...");

		for (final Player online : Remain.getOnlinePlayers())
			online.kickPlayer("Kicked due to server maintenance");

		Bukkit.setWhitelist(true);

		Common.log("2/4 Running region scan ...");
		new BossRegionRemover(bossNamesToRemove, world).scan(world);
	}
}
