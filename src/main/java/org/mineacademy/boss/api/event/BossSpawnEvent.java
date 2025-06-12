package org.mineacademy.boss.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.fo.event.SimpleEvent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Called when the Boss is about to spawn, you can get the entity
 * here and override its settings/attributes. Canceling this event
 * will remove the entity and its riding counterparts.
 */
@Getter
@RequiredArgsConstructor
public final class BossSpawnEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The boss being spawned
	 */
	private final Boss boss;

	/**
	 * The living entity you can modify
	 */
	private final LivingEntity entity;

	/**
	 * Why the boss is being spawned
	 */
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
