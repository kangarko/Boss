package org.mineacademy.boss.spawn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.region.DiskRegion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents tranferable data over all spawn rules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpawnData {

	/**
	 * The custom data that we can get at the end of spawn rule cycle.
	 * Or we can inject custom data for spawn rules to manage.
	 */
	private final Map<Tag, Object> data = new LinkedHashMap<>();

	/**
	 * The list of Bosses the spawn rule summoned successfully.
	 */
	@Getter
	private final List<SpawnedBoss> bosses = new ArrayList<>();

	/**
	 * Return the location where the entity should spawn.
	 *
	 * @return
	 */
	@Nullable
	public Location getLocation() {
		return this.data.containsKey(Tag.LOCATION) ? (Location) this.data.get(Tag.LOCATION) : null;
	}

	/**
	 * Return the entity type that should be spawned.
	 *
	 * @return
	 */
	@Nullable
	public EntityType getMatchingType() {
		return this.data.containsKey(Tag.MATCHING_TYPE) ? (EntityType) this.data.get(Tag.MATCHING_TYPE) : null;
	}

	/**
	 * Return the region where the entity should spawn.
	 *
	 * @return
	 */
	@Nullable
	public DiskRegion getRegion() {
		return this.data.containsKey(Tag.REGION) ? (DiskRegion) this.data.get(Tag.REGION) : null;
	}

	/**
	 * Create spawn data for spawn rule to replace vanilla mobs.
	 *
	 * @param entity
	 * @return
	 */
	public static SpawnData fromVanillaReplace(Entity entity) {
		final SpawnData data = new SpawnData();

		data.data.put(Tag.LOCATION, entity.getLocation());
		data.data.put(Tag.MATCHING_TYPE, entity.getType());

		return data;
	}

	/**
	 * Create spawn data for spawn rule on region enter.
	 *
	 * @param region
	 * @return
	 */
	public static SpawnData fromRegionEnter(DiskRegion region) {
		final SpawnData data = new SpawnData();

		data.data.put(Tag.REGION, region);

		return data;
	}

	/**
	 * Create spawn data for spawn rule on behavior task.
	 *
	 * @return
	 */
	public static SpawnData fromBehaviorTask() {
		return new SpawnData();
	}

	private enum Tag {
		LOCATION,
		MATCHING_TYPE,
		CAUSE,
		REGION
	}
}