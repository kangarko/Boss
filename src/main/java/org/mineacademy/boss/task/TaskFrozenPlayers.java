package org.mineacademy.boss.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.mineacademy.boss.skill.SkillFreeze;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Task managing automatic de-freezing of frozen players from {@link SkillFreeze}
 * as well as teleporting them back to prevent movement
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskFrozenPlayers extends SimpleRunnable {

	/**
	 * The singleton instance
	 */
	@Getter
	private static final TaskFrozenPlayers instance = new TaskFrozenPlayers();

	/**
	 * Stores frozen player data
	 */
	private final Map<Player, FreezeData> frozenPlayers = new HashMap<>();

	/**
	 * Teleport players back when they move and unfreeze offline and expired players
	 */
	@Override
	public void run() {
		synchronized (this.frozenPlayers) {
			final long now = System.currentTimeMillis();

			for (final Iterator<Map.Entry<Player, FreezeData>> it = this.frozenPlayers.entrySet().iterator(); it.hasNext();) {
				final Map.Entry<Player, FreezeData> entry = it.next();
				final Player player = entry.getKey();
				final FreezeData data = entry.getValue();

				if (!player.isOnline() || now >= data.getExpireTime()) {
					this.unfreeze(player, data);

					it.remove();
					return;
				}

				final Location initialLocation = data.getInitialLocation();
				final Location currentLocation = player.getLocation();

				if (!Valid.locationEquals(currentLocation, initialLocation)) {
					final Location teleportLocation = initialLocation.clone();

					teleportLocation.setPitch(currentLocation.getPitch());
					teleportLocation.setYaw(currentLocation.getYaw());

					player.teleport(teleportLocation);
				}
			}
		}
	}

	/**
	 * Unfreeze all players
	 */
	public void unfreezeAll() {
		synchronized (this.frozenPlayers) {
			for (final Map.Entry<Player, FreezeData> entry : this.frozenPlayers.entrySet()) {
				final Player player = entry.getKey();
				final FreezeData data = entry.getValue();

				this.unfreeze(player, data);
			}

			this.frozenPlayers.clear();
		}
	}

	/**
	 * Check if the player is frozen
	 *
	 * @param player
	 * @return
	 */
	public boolean isFrozen(Player player) {
		synchronized (this.frozenPlayers) {
			return this.frozenPlayers.containsKey(player);
		}
	}

	/**
	 * Freeze the player
	 *
	 * @param player
	 * @param duration
	 * @param changeBlock
	 */
	public void freeze(Player player, SimpleTime duration, boolean changeBlock) {
		synchronized (this.frozenPlayers) {
			player.setAllowFlight(false);
			player.setFlying(false);
			player.setFlySpeed(0);
			player.setWalkSpeed(0);

			BlockState originalBlock = null;

			if (changeBlock) {
				final Block block = player.getLocation().getBlock();

				originalBlock = block.getState();

				if (block.getType() == Material.AIR)
					block.setType(CompMaterial.COBWEB.getMaterial());
			}

			this.frozenPlayers.put(player, FreezeData.fromPlayer(player, duration, changeBlock, originalBlock));

		}
	}

	/**
	 * Unfreeze the player
	 *
	 * @param player
	 */
	public void unfreezeIfFrozen(Player player) {
		synchronized (this.frozenPlayers) {
			final FreezeData data = this.frozenPlayers.remove(player);

			if (data != null)
				this.unfreeze(player, data);
		}
	}

	/*
	 * Unfreeze the player with the given data
	 */
	private void unfreeze(Player player, @NonNull FreezeData data) {
		if (player.isOnline()) {
			player.setAllowFlight(data.isFlightAllowed());
			player.setFlying(data.isFlying());
			player.setFlySpeed(data.getFlySpeed() < 0.2F ? 0.2F : data.getFlySpeed());
			player.setWalkSpeed(data.getWalkSpeed() < 0.2F ? 0.2F : data.getWalkSpeed());
		}

		if (data.changedBlock) {
			data.getInitialLocation().getBlock().setType(Material.AIR);

			if (data.getOriginalBlock() != null)
				data.getOriginalBlock().update(true, false);
		}
	}

	/**
	 * Data class storing frozen player information
	 */
	@Getter(AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public final static class FreezeData {

		/**
		 * The time when the player will be unfrozen
		 */
		private long expireTime;

		/**
		 * Whether the block under the player has been changed to cobweb
		 */
		private boolean changedBlock;

		/**
		 * The initial location of the player
		 */
		private Location initialLocation;

		/**
		 * Whether the player was allowed to fly
		 */
		private boolean isFlightAllowed;

		/**
		 * Whether the player was flying
		 */
		private boolean flying;

		/**
		 * The fly speed of the player
		 */
		private float flySpeed;

		/**
		 * The walk speed of the player
		 */
		private float walkSpeed;

		/**
		 * The original block
		 */
		private BlockState originalBlock;

		/**
		 * Create a new freeze data from the player
		 *
		 * @param player
		 * @param duration
		 * @param changeBlock
		 * @param originalBlock
		 * @return
		 */
		public static FreezeData fromPlayer(Player player, SimpleTime duration, boolean changeBlock, BlockState originalBlock) {
			final FreezeData data = new FreezeData();

			data.expireTime = System.currentTimeMillis() + duration.getTimeMilliseconds();
			data.changedBlock = changeBlock;
			data.originalBlock = originalBlock;

			data.initialLocation = player.getLocation();
			data.isFlightAllowed = player.getAllowFlight();
			data.flying = player.isFlying();
			data.flySpeed = player.getFlySpeed();
			data.walkSpeed = player.getWalkSpeed();

			return data;
		}
	}

}
