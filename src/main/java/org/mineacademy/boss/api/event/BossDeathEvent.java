package org.mineacademy.boss.api.event;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triggered when a {@link Boss} dies.
 */
@RequiredArgsConstructor
public final class BossDeathEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The Boss representation.
	 */
	@Getter
	private final Boss boss;

	/**
	 * The died entity, Bukkit representation,
	 */
	@Getter
	private final LivingEntity entity;

	/**
	 * The drops for the Boss. You can make final modifications here.
	 * Please note that this are NOT drops to player inventories, only drops on the floor.
	 */
	@Getter
	private final List<ItemStack> drops;

	/**
	 * The original event.
	 */
	@Getter
	private final EntityDeathEvent deathEvent;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}