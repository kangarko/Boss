package org.mineacademy.boss.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Triggered when a new {@link Boss} is spawned.
 */
@RequiredArgsConstructor
public final class BossSpawnEvent extends Event implements Cancellable {

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

	/**
	 * Shall we prevent the Boss from spawning?
	 */
	@Getter
	@Setter
	private boolean cancelled;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}