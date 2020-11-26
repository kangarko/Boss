package org.mineacademy.boss.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triggered just after a new {@link Boss} is spawned.
 */
@RequiredArgsConstructor
public final class BossPostSpawnEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The Boss representation.
	 */
	@Getter
	private final Boss boss;

	/**
	 * The spawned entity, Bukkit representation,
	 */
	@Getter
	private final LivingEntity entity;

	/**
	 * The cause of the spawn.
	 */
	@Getter
	private final BossSpawnReason spawnReason;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}