package org.mineacademy.boss.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.boss.spawn.SpawnData;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.Remain;

/**
 * The task to tick regions and detect entering.
 */
public final class TaskRegionEnter extends SimpleRunnable {

	/**
	 * Player unique ids with their regions they are currently in.
	 */
	private final Map<UUID, Set<String>> playerRegions = new HashMap<>();

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		for (final Player online : Remain.getOnlinePlayers()) {
			final UUID uniqueId = online.getUniqueId();
			final Set<String> previousRegions = this.playerRegions.getOrDefault(uniqueId, new HashSet<>());
			final List<DiskRegion> currentRegions = DiskRegion.findRegions(online.getLocation());

			for (final DiskRegion currentRegion : currentRegions)
				if (!previousRegions.contains(currentRegion.getFileName()))
					this.onRegionEnter(currentRegion);

			this.playerRegions.put(uniqueId, new HashSet<>(Common.convertList(currentRegions, DiskRegion::getFileName)));
		}
	}

	/*
	 * Called when player enters the given boss region
	 */
	private void onRegionEnter(DiskRegion region) {
		SpawnRule.tick(SpawnData.fromRegionEnter(region), SpawnRuleType.REGION_ENTER);
	}
}
