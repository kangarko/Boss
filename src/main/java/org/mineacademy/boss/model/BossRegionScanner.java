package org.mineacademy.boss.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.mineacademy.boss.util.BossTaggingUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.OfflineRegionScanner;

public final class BossRegionScanner extends OfflineRegionScanner {

	private IsInList<String> bossesToRemove;

	private int totalRemovedAmount;

	public void launch(final List<String> bossesToRemove, final World world) {
		this.bossesToRemove = new IsInList<>(bossesToRemove == null ? Arrays.asList("*") : bossesToRemove);
		this.totalRemovedAmount = 0;

		scan(world);
	}

	@Override
	protected void onChunkScan(final Chunk chunk) {
		final List<String> removedNames = new ArrayList<>();
		int removedChunkBosses = 0;

		for (final Entity entity : chunk.getEntities()) {
			final String tag = BossTaggingUtil.getTag(entity);

			if (tag != null && bossesToRemove.contains(tag)) {
				entity.remove();

				totalRemovedAmount++;
				removedChunkBosses++;

				if (!removedNames.contains(tag))
					removedNames.add(tag);
			}
		}

		if (removedChunkBosses > 0)
			Common.log("Removed Bosses " + removedNames + " (" + removedChunkBosses + ") in chunk x: " + chunk.getX() + " z: " + chunk.getZ());
	}

	@Override
	protected void onScanFinished() {
		Common.log("Removed " + totalRemovedAmount + " Bosses in total.");
	}
}
