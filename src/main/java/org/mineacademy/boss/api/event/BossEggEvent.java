package org.mineacademy.boss.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.mineacademy.boss.api.Boss;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Triggered when a Player successfully clicks with a Boss egg.
 * <p>
 * Right click results in spawning the Boss.
 * Left click results in opening the Boss' menu.
 * <p>
 * The event only fires when the player was allowed clicking. When he doesn't
 * have the permission, it is cancelled within Boss and no event is fired.
 */
@RequiredArgsConstructor
public final class BossEggEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The Boss representation.
	 */
	@Getter
	private final Boss boss;

	/**
	 * What kind of click? Any right = about to spawn a Boss, any left = open Boss' menu.
	 */
	@Getter
	private final Action action;

	/**
	 * The player that clicked with egg, if any.
	 */
	@Getter
	private final Player player;

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