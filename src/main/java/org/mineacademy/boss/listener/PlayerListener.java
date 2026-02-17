package org.mineacademy.boss.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.mineacademy.boss.PlayerCache;
import org.mineacademy.boss.api.event.BossEggEvent;
import org.mineacademy.boss.hook.GriefPreventionHook;
import org.mineacademy.boss.menu.SelectBossMenu;
import org.mineacademy.boss.menu.boss.BossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossBarManager;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.BossSpawnResult;
import org.mineacademy.boss.model.Permissions;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.tool.EntityInfoTool;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * Player event listener.
 */
@AutoRegister
public final class PlayerListener extends BossListener {

	/**
	 * Clear up player cache on his disconnect.
	 *
	 * @param event
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();

		PlayerCache.removeCache(player);
		BossBarManager.getInstance().hideBarForPlayer(player);
	}

	/**
	 * Show entity information when clicking with Boss egg.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = false)
	public void onRightClickEntity(PlayerInteractEntityEvent event) {
		if (Remain.isInteractEventPrimaryHand(event))
			this.runIfBoss(event.getPlayer().getItemInHand(), bossOnItem -> {
				EntityInfoTool.getInstance().onEntityRightClick(event);

				event.setCancelled(true);
			});
	}

	/**
	 * Open Boss menu when clicking with egg.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
	public void onLeftClickEntity(EntityDamageByEntityEvent event) {
		final Entity damager = event.getDamager();

		if (damager instanceof Player) {
			final Player player = (Player) damager;

			// Show menu when left clicking holding egg
			this.runIfBoss(player.getItemInHand(), boss -> {

				if (!Valid.checkPermission(player, Permissions.Command.MENU))
					return;

				if (!Platform.callEvent(new BossEggEvent(boss, Action.LEFT_CLICK_BLOCK, player)))
					return;

				BossMenu.showTo(SelectBossMenu.create(), player, boss);

				event.setCancelled(true);
			});
		}
	}

	/**
	 * Spawn the Boss or open menu depending on the click holding a Boss egg.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
	public void onClick(PlayerInteractEvent event) {
		if (!Remain.isInteractEventPrimaryHand(event) || event.getAction() == Action.PHYSICAL)
			return;

		final Player player = event.getPlayer();

		// Fix bug in older Spigot versions where the event is called while browsing GUI
		if (player.hasMetadata(Menu.TAG_MENU_CURRENT))
			return;

		final String bossName = Boss.findBossName(player.getItemInHand());

		if (bossName == null)
			return;

		final Boss boss = Boss.findBoss(bossName);

		if (boss == null) {
			Messenger.warn(player, Lang.component("command-invalid-boss-uninstalled", "boss", bossName));

			event.setCancelled(true);
			return;
		}

		// Fix Spigot bug, see above
		if (CompMetadata.hasTempMetadata(player, "ShowingBossInfo")) {
			event.setCancelled(true);

			return;
		}

		// Show menu when left clicking holding egg
		if (event.getAction().toString().contains("LEFT_CLICK")) {

			if (!Valid.checkPermission(player, Permissions.Command.MENU))
				return;

			if (!Platform.callEvent(new BossEggEvent(boss, event.hasBlock() ? Action.LEFT_CLICK_BLOCK : Action.LEFT_CLICK_AIR, player))) {
				event.setCancelled(true);

				return;
			}

			BossMenu.showTo(SelectBossMenu.create(), player, boss);

			event.setCancelled(true);
			return;
		}

		if (!Valid.checkPermission(player, Permissions.Use.SPAWNER_EGG) || !Valid.checkPermission(player, Permissions.Spawn.BOSS.replace("{boss}", boss.getName())))
			return;

		if (!Platform.callEvent(new BossEggEvent(boss, event.hasBlock() ? Action.RIGHT_CLICK_BLOCK : Action.RIGHT_CLICK_AIR, player)))
			return;

		final Location location = this.findSpawnLocation(player, event.getClickedBlock());

		// Air spawn disabled or no permission
		if (location != null && this.checkClaims(player, location)) {

			// Spawn the boss
			final Tuple<BossSpawnResult, SpawnedBoss> tuple = boss.spawn(location, BossSpawnReason.EGG);

			// Take item and notify on fail
			if (tuple.getKey() == BossSpawnResult.SUCCESS) {
				if (player.getGameMode() != GameMode.CREATIVE)
					Remain.takeHandItem(player);

				final SpawnedBoss spawned = tuple.getValue();

				if (spawned.getEntity() instanceof Tameable) {
					((Tameable) spawned.getEntity()).setOwner(player);

					Messenger.success(player, Lang.component("egg-spawn-success-tamed", "boss", boss.getName()));
				}
			}

			else
				Messenger.error(player, Lang.component("egg-spawn-fail-1", "boss", boss.getName(), "reason", tuple.getKey().getFailReason())
						.append(Lang.component("egg-spawn-fail-2"))
						.onHover(Lang.component("egg-spawn-fail-hover"))
						.append(Lang.component("egg-spawn-fail-3")));
		}

		// Prevent vanilla spawning
		event.setCancelled(true);
	}

	/*
	 * Checks some plugin claims and prevents spawning.
	 */
	private boolean checkClaims(Player player, Location location) {

		if (player.hasPermission(Permissions.Bypass.CLAIM))
			return true;

		if (GriefPreventionHook.isEnabled()) {
			final String claimOwner = GriefPreventionHook.getClaimOwner(location);

			if (claimOwner != null && !claimOwner.equals(player.getName())) {
				Messenger.error(player, Lang.component("egg-spawn-fail-claim", "player", claimOwner));

				return false;
			}
		}

		return true;
	}

	/*
	 * Finds and prepares the spawning location.
	 */
	private Location findSpawnLocation(Player player, Block maybeBlock) {
		Location location = null;

		// Find location, use air spawning if possible
		if (maybeBlock != null)
			location = maybeBlock.getLocation().add(0, 1, 0);

		// Air spawn
		else if (Settings.Spawning.AIR_SPAWN && player.hasPermission(Permissions.Spawn.AIR))
			location = Remain.getTargetBlock(player, Settings.Spawning.AIR_SPAWN_MAX_DISTANCE).getLocation();

		if (location != null) {

			// Either teleport to highest free block or closer to player to prevent suffocation
			{
				boolean teleportedToTop = false;
				Block higherFreeBlock = location.getBlock();

				for (int i = 0; i < 3; i++) {
					final Block above = higherFreeBlock.getRelative(BlockFace.UP);

					if (CompMaterial.isAir(higherFreeBlock) && CompMaterial.isAir(above)) {
						location.add(0, i, 0);

						teleportedToTop = true;
						break;
					}

					higherFreeBlock = higherFreeBlock.getRelative(BlockFace.UP);
				}

				// Adjust position closer to player so Boss won't suffocate in the wall
				if (!teleportedToTop) {
					final Vector closerToPlayer = location.toVector().subtract(player.getLocation().toVector()).normalize();

					closerToPlayer.setY(0);
					closerToPlayer.normalize();

					closerToPlayer.setX(this.adjustPosition(closerToPlayer.getX()));
					closerToPlayer.setZ(this.adjustPosition(closerToPlayer.getZ()));

					location.subtract(closerToPlayer);
				}
			}

			// Center to block
			location.add(0.5, 0, 0.5);

			// Make the spawned mob face the player (requires newer MC versions)
			location.setYaw(PlayerUtil.alignYaw(player.getLocation().getYaw(), false) - 180);
		}

		return location;
	}

	/*
	 * Rounds the position number.
	 */
	private double adjustPosition(double position) {
		return position < 0 ? -1 : position > 0 ? 1 : 0;
	}
}
