package org.mineacademy.boss.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.mineacademy.boss.listener.ChunkListener.UnloadedBosses;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.settings.YamlConfig;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import lombok.Data;

/**
 * Chunk event listener.
 */
public final class ChunkListener implements Listener {

	/**
	 * Stores unloaded Bosses by world name, then the boss name and the entity list.
	 */
	final static Map<String, UnloadedBosses> unloadedBosses = new HashMap<>();

	@Data
	public static class UnloadedBosses implements ConfigSerializable {

		private final Map<String /* Boss Name */, Map<String /* Chunk Key */, Set<UUID>>> bossesChunkMap;

		UnloadedBosses() {
			this(new HashMap<>());
		}

		UnloadedBosses(Map<String, Map<String, Set<UUID>>> bossesChunkMap) {
			this.bossesChunkMap = bossesChunkMap;
		}

		public void addUnloadedBoss(SpawnedBoss boss) {
			final Entity entity = boss.getEntity();

			// Find the chunk for the Boss
			final Chunk chunk = entity.getLocation().getChunk();
			final String chunkKey = chunk.getX() + " " + chunk.getZ();

			final Map<String, Set<UUID>> bossesInWorld = this.bossesChunkMap.computeIfAbsent(boss.getName(), k -> new HashMap<>());
			final Set<UUID> bossesOfType = bossesInWorld.computeIfAbsent(chunkKey, k -> new HashSet<>());

			bossesOfType.add(entity.getUniqueId());
		}

		public void removeUnloadedBoss(UUID entityUUID) {
			for (final Iterator<Map.Entry<String, Map<String, Set<UUID>>>> typeIterator = this.bossesChunkMap.entrySet().iterator(); typeIterator.hasNext();) {
				final Map.Entry<String, Map<String, Set<UUID>>> entry = typeIterator.next();

				final Map<String, Set<UUID>> bossesInWorld = entry.getValue();

				for (final Iterator<Map.Entry<String, Set<UUID>>> chunkIterator = bossesInWorld.entrySet().iterator(); chunkIterator.hasNext();) {
					final Map.Entry<String, Set<UUID>> entry2 = chunkIterator.next();

					final Set<UUID> bossesOfType = entry2.getValue();

					if (bossesOfType.remove(entityUUID)) {
						if (bossesOfType.isEmpty())
							chunkIterator.remove();

						break;
					}
				}

				if (bossesInWorld.isEmpty())
					typeIterator.remove();
			}
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			for (final Map.Entry<String, Map<String, Set<UUID>>> entry : bossesChunkMap.entrySet()) {
				final String worldName = entry.getKey();
				final Map<String, Set<UUID>> bossesInChunks = entry.getValue();

				map.put(worldName, bossesInChunks);
			}

			return map;
		}

		public static UnloadedBosses deserialize(SerializedMap map) {
			final Map<String, Map<String, Set<UUID>>> bossesChunkMap = new HashMap<>();

			for (final Map.Entry<String, Object> entry : map.entrySet()) {
				final String bossName = entry.getKey();
				final SerializedMap chunkLists = SerializedMap.fromObject(entry.getValue());

				final Map<String, Set<UUID>> bossesInChunks = new HashMap<>();

				for (final Map.Entry<String, Object> entry2 : chunkLists.entrySet()) {
					final String chunkKey = entry2.getKey();
					final Collection<?> bossRawUuids = (Collection<?>) entry2.getValue();

					final Set<UUID> bossesOfType = new HashSet<>();

					for (final Object uuid : bossRawUuids)
						bossesOfType.add(UUID.fromString(uuid.toString()));

					bossesInChunks.put(chunkKey, bossesOfType);
				}

				bossesChunkMap.put(bossName, bossesInChunks);
			}

			return new UnloadedBosses(bossesChunkMap);
		}
	}

	/**
	 * The configuration file for unloaded bosses for persistence.
	 */
	private final static YamlConfig config = new YamlConfig();

	public ChunkListener() {
		try {
			Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");

			if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS)
				Platform.registerEvents(new CompPaperRemoveEntityEvent());

		} catch (final ClassNotFoundException | NoClassDefFoundError err) {
			// Ignore
		}
	}

	/**
	 * When a chunk loads, remove the boss from the unloadedBosses map
	 *
	 * @param event
	 */
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		final World world = event.getWorld();
		final String worldName = world.getName();

		for (final Entity entity : event.getChunk().getEntities()) {
			final SpawnedBoss boss = Boss.findBoss(entity);

			if (boss != null) {
				if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS) {
					final String bossName = boss.getBoss().getName();

					Debugger.debug("unloaded", "Removing loaded " + bossName + " from " + worldName);
					unloadedBosses.computeIfAbsent(worldName, k -> new UnloadedBosses(new HashMap<>())).removeUnloadedBoss(entity.getUniqueId());
				}

				CompMetadata.setTempMetadata(entity, Boss.NBT_TAG, boss.getName());
			}
		}
	}

	/**
	 * Get the amount of unloaded bosses in the world for the given boss.
	 *
	 * @param world
	 * @param typeOf
	 *
	 * @return
	 */
	public static Integer countUnloadedBosses(World world, Boss typeOf) {
		int count = 0;

		if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS) {
			final UnloadedBosses unloaded = unloadedBosses.get(world.getName());

			if (unloaded != null) {
				final Map<String, Set<UUID>> bossesInChunks = unloaded.getBossesChunkMap().get(typeOf.getName());

				if (bossesInChunks != null) {
					for (final Set<UUID> bossSet : bossesInChunks.values())
						count += bossSet.size();

					return count;
				}
			}
		}

		return count;
	}

	/**
	 * Get the amount of all unloaded bosses in the world.
	 *
	 * @param world
	 * @return
	 */
	public static Integer countAllBosses(World world) {
		int count = 0;

		if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS) {
			final UnloadedBosses unloaded = unloadedBosses.get(world.getName());

			if (unloaded != null)
				for (final Map<String, Set<UUID>> bossesInChunks : unloaded.getBossesChunkMap().values())
					if (bossesInChunks != null) {
						for (final Set<UUID> bossSet : bossesInChunks.values())
							count += bossSet.size();

						return count;
					}
		}

		return count;
	}

	/**
	 * Remove the given entity from the unloaded bosses.
	 *
	 * @param entityUUID
	 */
	public static void removeUnloadedEntity(UUID entityUUID) {
		unloadedBosses.values().forEach(unloaded -> unloaded.removeUnloadedBoss(entityUUID));
	}

	/**
	 * Load the unloaded bosses from file.
	 */
	public static void loadFromFile() {
		synchronized (config) {
			if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS) {

				config.loadFromFile(FileUtil.createIfNotExists("unloaded-bosses.yml"));
				config.setHeader(
						"This file stores unloaded Bosses to apply world limits. The first section is the world,",
						"then the Boss type with chunk coords and a list of saved entity UUIDs.",
						"",
						"You can safely delete this or parts of this file while server is stopped.");

				unloadedBosses.clear();

				// Top sections are world names
				for (final String worldName : config.getKeys(false))
					unloadedBosses.put(worldName, config.get(worldName, UnloadedBosses.class));
			}
		}
	}

	/**
	 * Save the unloaded bosses to file.
	 */
	public static void saveToFile() {
		synchronized (config) {
			if (Settings.Spawning.COUNT_UNLOADED_BOSSES_IN_LIMITS) {
				if (config.getFile() == null)
					config.setFile(FileUtil.createIfNotExists("unloaded-bosses.yml"));

				for (final Map.Entry<String, UnloadedBosses> entry : unloadedBosses.entrySet()) {
					final String worldName = entry.getKey();
					final UnloadedBosses bosses = entry.getValue();

					config.set(worldName, bosses.serialize());
				}

				config.save();
			}
		}
	}
}

final class CompPaperRemoveEntityEvent implements Listener {

	/**
	 * When a chunk unloads, add the boss to the unloadedBosses map
	 *
	 * @param event
	 */
	@EventHandler
	public void onChunkUnload(EntityRemoveFromWorldEvent event) {
		World world;

		try {
			world = event.getWorld();

		} catch (final NoSuchMethodError err) {
			// 1.20.2 and lower
			world = event.getEntity().getWorld();
		}

		final String worldName = world.getName();

		final Entity entity = event.getEntity();
		final UUID entityUid = entity.getUniqueId();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null) {
			final UnloadedBosses unloadedBosses = ChunkListener.unloadedBosses.computeIfAbsent(worldName, k -> new UnloadedBosses());
			final String bossName = boss.getBoss().getName();

			if (!entity.isDead()) {
				Debugger.debug("unloaded", "Saving alive unloaded Boss " + bossName + " in world " + worldName);

				unloadedBosses.addUnloadedBoss(boss);

			} else {
				unloadedBosses.removeUnloadedBoss(entityUid);

				Debugger.debug("unloaded", "Ignoring dead unloaded Boss " + bossName + " in world " + worldName);
			}
		}
	}
}
