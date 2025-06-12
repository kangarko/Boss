package org.mineacademy.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;
import lombok.Setter;

/**
 * A player cache storing temporary player information.
 */
@Getter
public final class PlayerCache {

	/**
	 * The player cache map caching data for players online.
	 */
	private static final Map<UUID, PlayerCache> cacheMap = new HashMap<>();

	/**
	 * This instance's player's unique id
	 */
	private final UUID uniqueId;

	/**
	 * The created region, if any
	 */
	@Setter
	private VisualizedRegion createdRegion = new VisualizedRegion();

	/*
	 * Creates a new player cache (see the bottom)
	 */
	private PlayerCache(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	/* ------------------------------------------------------------------------------- */
	/* Misc methods */
	/* ------------------------------------------------------------------------------- */

	@Override
	public String toString() {
		return "PlayerCache{" + this.uniqueId + "}";
	}

	/* ------------------------------------------------------------------------------- */
	/* Static access */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return or create new player cache for the given player
	 *
	 * @param player
	 * @return
	 */
	public static PlayerCache from(Player player) {
		final UUID uniqueId = player.getUniqueId();

		PlayerCache cache = cacheMap.get(uniqueId);

		if (cache == null) {
			cache = new PlayerCache(uniqueId);

			cacheMap.put(uniqueId, cache);
		}

		return cache;
	}

	/**
	 * Clear the cache for the player
	 *
	 * @param player
	 */
	public static void removeCache(Player player) {
		cacheMap.remove(player.getUniqueId());
	}
}
