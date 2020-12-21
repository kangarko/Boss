package org.mineacademy.boss.model.task;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.util.BossUtil;

public final class BossRetargetTask implements Runnable {

	@Override
	public void run() {
		for (final World world : Bukkit.getWorlds())
			for (final SpawnedBoss boss : BossPlugin.getBossManager().findBosses(world))
				BossUtil.setBossTargetInRadius(boss.getBoss(), boss.getEntity());
	}
}