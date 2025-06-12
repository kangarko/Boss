package org.mineacademy.boss.model;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a Boss that is alive and well (hopefully).
 */
@Getter
public final class SpawnedBoss {

	/**
	 * The Boss
	 */
	private final Boss boss;

	/**
	 * The living entity of the Boss
	 */
	private final LivingEntity entity;

	/**
	 * Create a new SpawnedBoss from the given boss and entity
	 *
	 * @param boss
	 * @param entity
	 */
	public SpawnedBoss(@NonNull Boss boss, @NonNull LivingEntity entity) {
		this.boss = boss;
		this.entity = entity;
	}

	/**
	 * Return the name of the Boss
	 *
	 * @return
	 */
	public String getName() {
		return this.boss.getName();
	}

	/**
	 * Return the unique ID of the Boss
	 *
	 * @return
	 */
	public UUID getUniqueId() {
		return this.entity.getUniqueId();
	}

	/**
	 * Return the origin spawn location of Boss
	 *
	 * @return
	 */
	public Location getSpawnLocation() {
		final String locationRaw = CompMetadata.getMetadata(this.entity, Boss.SPAWN_LOCATION_TAG);

		return SerializeUtil.deserializeLocation(locationRaw);
	}

	/**
	 * Return the region where Boss initially spawned, if any.
	 *
	 * @return
	 */
	@Nullable
	public DiskRegion getSpawnRegion() {
		final String regionName = CompMetadata.getMetadata(this.entity, Boss.REGION_TAG);

		return regionName != null ? DiskRegion.findRegion(regionName) : null;
	}

	/**
	 * Return the spawn rule name that was used to spawn this Boss
	 *
	 * @return
	 */
	@Nullable
	public String getSpawnRuleName() {
		return CompMetadata.getMetadata(this.entity, Boss.SPAWN_RULE_TAG);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof SpawnedBoss && ((SpawnedBoss) obj).getBoss().equals(this.boss) && ((SpawnedBoss) obj).getEntity().equals(this.entity);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Valid.checkNotNull(this.boss, "Boss cannot be null!");
		Valid.checkNotNull(this.entity, "Entity cannot be null!");

		return "SpawnedBoss{boss=" + this.boss.getName() + ", entity=" + this.entity + "}";
	}
}
