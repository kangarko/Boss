package org.mineacademy.boss.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.api.event.BossEggEvent;
import org.mineacademy.boss.api.event.BossSpawnEvent;
import org.mineacademy.boss.menu.MenuBossIndividual;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.boss.util.BossEggSpawnUtil;
import org.mineacademy.boss.util.BossNBTUtil;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

public final class SpawningListener implements Listener {

	// -------------------------------------------------------------------------------------
	// Handles Egg spawning
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(final PlayerInteractEvent playerInteractEvent) {
		final ItemStack item = playerInteractEvent.getItem();

		if (item == null || item.getType() == Material.AIR)
			return;

		if (playerInteractEvent.getPlayer().getGameMode() == GameMode.SPECTATOR)
			return;

		LagCatcher.start("interact");

		try {

			final String bossName = BossNBTUtil.readBossName(item);

			if (bossName == null)
				return;

			final Player player = playerInteractEvent.getPlayer();
			final Action action = playerInteractEvent.getAction();
			final boolean cancelled = playerInteractEvent.isCancelled();

			final Boss boss = BossPlugin.getBossManager().findBoss(bossName);

			if (boss == null) {
				Common.tell(player, Localization.Spawning.NOT_INSTALLED.replace("{boss}", bossName));

				playerInteractEvent.setCancelled(true);
				return;
			}

			if (!PlayerUtil.hasPerm(player, Permissions.Use.SPAWNER_EGG)) {
				Common.tell(player, Localization.Spawning.NO_PERMISSION);

				playerInteractEvent.setCancelled(true);
				return;
			}

			if (Settings.EggSpawning.FORCE_LATEST_EGG)
				if (!ItemUtil.isSimilar(boss.asEgg(), player.getItemInHand())) {
					Common.tell(player, "Your Boss item is outdated and wont spawn. Requires " + boss.asEgg());

					playerInteractEvent.setCancelled(true);
					return;
				}

			final String spawnPerm = Permissions.Use.SPAWN.replace("{plugin_name}", SimplePlugin.getNamed().toLowerCase()).replace("{name}", boss.getSpawnPermission());

			if (!PlayerUtil.hasPerm(player, spawnPerm)) {
				final String customMessage = boss.getSettings().getNoSpawnPermissionMessage();
				Common.tell(player, (customMessage != null && !customMessage.isEmpty() ? customMessage : SimpleLocalization.NO_PERMISSION).replace("{boss}", boss.getName()).replace("{permission}", spawnPerm));

				playerInteractEvent.setCancelled(true);
				return;
			}

			if (action.toString().contains("LEFT_CLICK")) {
				if (PlayerUtil.hasPerm(player, Permissions.Commands.MENU) && Common.callEvent(new BossEggEvent(boss, action, player)))
					new MenuBossIndividual(boss, false).displayTo(player);

				playerInteractEvent.setCancelled(true);
				return;
			}

			if (!Settings.EggSpawning.SPAWN_IF_CANCELLED && cancelled)
				return;

			Location spawnLocation = null;

			if (action == Action.RIGHT_CLICK_BLOCK)
				spawnLocation = BossEggSpawnUtil.moveToPlayerIfNecessary(playerInteractEvent.getClickedBlock(), player, 10);

			else if (action == Action.RIGHT_CLICK_AIR && PlayerUtil.hasPerm(player, Permissions.AIRSPAWN))
				spawnLocation = BossEggSpawnUtil.getSpawnLocation(player, Settings.EggSpawning.RADIUS);

			if (spawnLocation == null)
				return;

			// Do not spawn bosses in the ground, lift them
			while (!CompMaterial.isAir(spawnLocation.getBlock()))
				spawnLocation.add(0, 1, 0);

			playerInteractEvent.setCancelled(true);

			if (Remain.isInteractEventPrimaryHand(playerInteractEvent)) {
				if (Common.callEvent(new BossEggEvent(boss, action, player)))
					if (BossConditionedSpawnUtil.spawnWithLimits(boss, spawnLocation, BossSpawnReason.EGG, player)) {
						if (player.getGameMode() != GameMode.CREATIVE) {
							Remain.takeItemAndSetAsHand(player, item);

							playerInteractEvent.setUseItemInHand(Result.DENY);
						}

					} else
						Common.tellReplaced(player, Localization.Spawning.FAIL, "boss", boss.getName());
			} else
				Common.tell(player, Localization.Spawning.MAIN_HAND);

		} finally {
			LagCatcher.end("interact");
		}
	}

	// -------------------------------------------------------------------------------------
	// Handles converting
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onSpawnEarly(final CreatureSpawnEvent e) {
		final LivingEntity entity = e.getEntity();

		if (entity == null)
			return;

		if (!Settings.PreventVanillaMobs.ENABLED || !Settings.PreventVanillaMobs.WORLDS.contains(entity.getWorld().getName()))
			return;

		final Boss boss = BossPlugin.getBossManager().findBoss(entity);

		// Allow Bosses
		if (boss == null && Settings.PreventVanillaMobs.SPAWN_REASONS.contains(e.getSpawnReason()) && Settings.PreventVanillaMobs.ENTITY_TYPES.contains(e.getEntityType()))
			e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSpawn(final CreatureSpawnEvent e) {
		final LivingEntity en = e.getEntity();

		if (en == null)
			return;

		if (!Settings.Converting.ENABLED)
			return;

		if (BossPlugin.getBossManager().findBoss(en) != null || Settings.Converting.IGNORED_CAUSES.contains(e.getSpawnReason()))
			return;

		final boolean success = BossConditionedSpawnUtil.convert(en.getType(), en.getLocation(), BossSpawnReason.CONVERTED);

		if (success)
			e.setCancelled(true);
	}

	// -------------------------------------------------------------------------------------
	// Handles spawning by dispenser
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onDispense(final BlockDispenseEvent e) {
		final boolean valid = e.getItem() != null && CompMaterial.isMonsterEgg(e.getItem().getType()) && e.getBlock().getState() instanceof Dispenser;

		if (!valid)
			return;

		final Dispenser b = (Dispenser) e.getBlock().getState();
		final ItemStack is = e.getItem();

		final String bossName = BossNBTUtil.readBossName(e.getItem());

		if (bossName == null)
			return;

		final Boss boss = BossPlugin.getBossManager().findBoss(bossName);

		if (boss == null) {
			Common.log("Cannot dispense '" + bossName + "' at " + Common.shortLocation(b.getLocation()) + " as the boss is not installed on this server.");

			e.setCancelled(true);
			return;
		}

		{ // Remove item
			Common.runLater(1, () -> {
				final ItemStack[] content = b.getInventory().getContents();

				for (int i = 0; i < content.length; i++) {
					final ItemStack slot = content[i];

					if (slot != null && ItemUtil.isSimilar(slot, is)) {
						slot.setAmount(slot.getAmount() - 1);

						content[i] = slot;
						break;
					}
				}

				b.getInventory().setContents(content);
				b.update();
			});
		}

		{ // Spawn boss
			final BlockFace face = getFace(b.getData().getData());
			final Location location = b.getLocation();

			adjustLocation(location, face);
			BossConditionedSpawnUtil.spawnWithLimits(boss, location, BossSpawnReason.DISPENSE);
		}

		e.setCancelled(true);
	}

	private BlockFace getFace(final int data) {
		switch (data) {
			case 0:
			case 8:
			case 14:
				return BlockFace.DOWN;
			case 1:
			case 9:
			case 15:
				return BlockFace.UP;
			case 2:
			case 10:
				return BlockFace.NORTH;
			case 3:
			case 11:
				return BlockFace.SOUTH;
			case 4:
			case 12:
				return BlockFace.WEST;
			case 5:
			case 13:
				return BlockFace.EAST;
		}

		throw new FoException("Unable to get dispensor face from data " + data);
	}

	private void adjustLocation(final Location location, final BlockFace face) {
		if (face == BlockFace.DOWN)
			location.add(0.5, -2, 0.5);

		if (face == BlockFace.UP)
			location.add(0.5, 1, 0.5);

		if (face == BlockFace.NORTH)
			location.add(0.5, 0, -1);

		if (face == BlockFace.SOUTH)
			location.add(0.5, 0, 1.5);

		if (face == BlockFace.WEST)
			location.add(-1, 0, 0.5);

		if (face == BlockFace.EAST)
			location.add(1.5, 0, 0.5);
	}

	// -------------------------------------------------------------------------------------
	// Additional fixes after the Boss is spawned
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBossSpawn(final BossSpawnEvent event) {
		final LivingEntity entity = event.getEntity();
		final Boss boss = event.getBoss();

		if (entity.isValid() && !entity.isDead()) {
			if ((event.getSpawnReason() == BossSpawnReason.CONVERTED && Settings.Converting.LIGHTNING)
					|| (event.getSpawnReason() == BossSpawnReason.TIMED && Settings.TimedSpawning.LIGHTNING))
				entity.getWorld().strikeLightningEffect(entity.getLocation());

			BossUtil.setBossTargetInRadius(boss, entity);
			BossUtil.runCommands(null, new SpawnedBoss(boss, entity), boss.getSpawnCommands());
		}
	}

	// -------------------------------------------------------------------------------------
	// Automatically adjust baby slime size
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onSlimeSpawn(final CreatureSpawnEvent e) {
		if (e.isCancelled() || e.getEntity() == null || e.getSpawnReason() != SpawnReason.SLIME_SPLIT || e.getEntity().getType() != EntityType.SLIME)
			return;

		final LivingEntity en = e.getEntity();
		final String name = Common.getOrEmpty(en.getCustomName());

		final Boss parent = BossPlugin.getBossManager().findBoss(name);

		if (parent != null) {
			e.setCancelled(true);

			if (Boolean.valueOf(parent.getSettings().getSpecificSetting(BossSpecificSetting.SLIME_BABIES).toString())) {
				final int size = ((Slime) en).getSize();

				Common.runLater(() -> {

					final SpawnedBoss baby = parent.spawn(en.getLocation(), BossSpawnReason.SLIME_SPLIT);
					Valid.checkBoolean(baby.getEntity().getType() == EntityType.SLIME, "Invalid entity!");

					((Slime) baby.getEntity()).setSize(size);
				});
			}
		}
	}
}