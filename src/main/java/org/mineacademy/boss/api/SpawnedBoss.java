package org.mineacademy.boss.api;

import org.bukkit.entity.LivingEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a {@link Boss} that is spawned in the world.
 */
@Getter
@RequiredArgsConstructor
public final class SpawnedBoss {

	/**
	 * The Boss.
	 */
	private final Boss boss;

	/**
	 * The entity in the world.
	 */
	private final LivingEntity entity;

	/**
	 * Get if the Boss is valid and alive
	 *
	 * @return if the Boss is valid and alive
	 */
	public boolean isAlive() {
		return entity != null && entity.isValid() && !entity.isDead();
	}

	@Override
	public int hashCode() {
		return boss.getName().hashCode() + entity.getEntityId();
	}
}
