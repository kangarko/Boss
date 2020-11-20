package org.mineacademy.boss.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.boss.storage.SimpleSpawnerData;

import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;

public final class SilkSpawnersHook implements Listener {

	@EventHandler
	public void onSpawnerChange(SilkSpawnersSpawnerChangeEvent e) {
		final SimpleSpawnerData data = SimpleSpawnerData.$();

		if (data.hasSpawner(e.getBlock().getLocation()))
			e.setCancelled(true);

	}
}
