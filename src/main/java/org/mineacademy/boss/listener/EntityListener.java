package org.mineacademy.boss.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossAPI;
import org.mineacademy.boss.api.BossDrop;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.boss.api.BossSpawnReason;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.boss.api.event.BossDeathEvent;
import org.mineacademy.boss.hook.BossHeroesHook;
import org.mineacademy.boss.menu.MenuBossIndividual;
import org.mineacademy.boss.settings.Localization;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.storage.SimpleTagData;
import org.mineacademy.boss.tool.InspectorTool;
import org.mineacademy.boss.util.BossConditionedSpawnUtil;
import org.mineacademy.boss.util.BossNBTUtil;
import org.mineacademy.boss.util.BossTaggingUtil;
import org.mineacademy.boss.util.BossUtil;
import org.mineacademy.boss.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.HealthBarUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.RangedRandomValue;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.Remain;
import org.spigotmc.event.entity.EntityDismountEvent;

public final class EntityListener implements Listener {

	public EntityListener() {

		// Entity pickup item
		try {
			Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
			Common.registerEvents(new CompBossPickupListener());

		} catch (final ClassNotFoundException ex) {
		}

		// Entity dismount
		try {
			Class.forName("org.spigotmc.event.entity.EntityDismountEvent");
			Common.registerEvents(new CompBossDismountListener());

		} catch (final ClassNotFoundException ex) {
		}

		if (MinecraftVersion.olderThan(V.v1_9))
			Common.registerEvents(new CompBossPortalCreateEvent());
	}

	// -------------------------------------------------------------------------------------
	// Opens menu when clicked
	// -------------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onHitWithEgg(final EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player || !(event.getDamager() instanceof Player))
			return;

		final Player player = (Player) event.getDamager();

		if (!PlayerUtil.hasPerm(player, Permissions.Commands.MENU))
			return;

		if (InspectorTool.getInstance().getItem().equals(player.getItemInHand())) {
			final Boss boss = BossPlugin.getBossManager().findBoss(event.getEntity());

			if (boss != null) {
				Common.tell(player, Localization.Tools.INSPECT_OPEN.replace("{boss}", boss.getName()));

				new MenuBossIndividual(boss, false).displayTo(player);
			} else
				Common.tell(player, Localization.Tools.INSPECT_INVALID);

			event.setCancelled(true);
		}

		final String bossName = BossNBTUtil.readBossName(player.getItemInHand());

		if (bossName != null) {
			event.setCancelled(true);

			final Boss boss = BossPlugin.getBossManager().findBoss(bossName);

			if (boss != null)
				new MenuBossIndividual(boss, false).displayTo(player);
		}

		final Boss boss = BossAPI.getBoss(event.getEntity());

		if (boss != null)
			Debugger.debug("damage", player.getName() + " hit " + boss.getName() + " that now has " + ((LivingEntity) event.getEntity()).getHealth() + " HP");
	}

	/**
	 * Don't allow renaming boss mobs
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInteract(final PlayerInteractEntityEvent event) {
		final Entity entity = event.getRightClicked();
		final ItemStack hand = event.getPlayer().getItemInHand();

		if (entity instanceof LivingEntity && hand != null && hand.getType() == Material.NAME_TAG) {
			final Boss boss = BossPlugin.getBossManager().findBoss(entity);

			if (boss != null)
				event.setCancelled(true);
		}
	}

	// -------------------------------------------------------------------------------------
	// Handles boss properties
	// -------------------------------------------------------------------------------------

	/**
	 * When dies
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDeath(final EntityDeathEvent event) {
		if (event.getEntity() == null)
			return;

		final LivingEntity entity = event.getEntity();
		final Boss boss = BossPlugin.getBossManager().findBoss(entity);

		if (boss == null) {
			BossPlugin.getBossManager().removeCachedEntity(entity.getUniqueId());

			return;
		}

		// Remove mount
		if (boss.getSettings().isRemovingRidingOnDeath() && entity.isInsideVehicle())
			entity.getVehicle().remove();

		// Lightning
		if (boss.hasLightningOnDeath())
			entity.getWorld().strikeLightningEffect(entity.getLocation());

		{ // Drops

			// Collect Boss drops & his equipment
			final List<ItemStack> bukkitDrops = event.getDrops();

			if (!boss.getSettings().hasNaturalDrops()) {
				Debugger.debug("drops", "Vanilla drops disabled -> Clearing what should been dropped originally: " + bukkitDrops);

				bukkitDrops.clear();

				boss.getEquipment().forEach(drop -> {
					if (RandomUtil.chanceD(drop.getDropChance())) {
						Debugger.debug("drops", "Readded back equipment: " + drop.getItem());

						bukkitDrops.add(drop.getItem().clone());
					}
				});
			}

			final List<BossDrop> drops = new ArrayList<>(boss.getDrops().values());
			Collections.shuffle(drops);

			Debugger.debug("drops", "Adding custom drops: " + drops);

			Common.callEvent(new BossDeathEvent(boss, entity, drops, event));

			if (!boss.getSettings().hasInventoryDrops()) {
				Debugger.debug("drops", "================== Dropping " + drops.size() + " drop(s) on the floor");

				for (final BossDrop drop : drops) {
					if (drop == null) {
						Debugger.debug("drops", "NOT dropping (drop null):");

						continue;
					}

					final ItemStack item = new ItemStack(drop.getItem());

					if (item.getType() == Material.AIR) {
						Debugger.debug("drops", "NOT dropping (Material == AIR)" + drop.getItem());

						continue;
					}

					if (!RandomUtil.chance(MathUtil.ceiling(drop.getDropChance() * 100))) {
						Debugger.debug("drops", "NOT dropping (item null or chance " + drop.getDropChance() + " failed): " + drop.getItem());

						continue;
					}

					bukkitDrops.add(item);

					Debugger.debug("drops", "Dropping " + item);

					if (boss.getSettings().hasSingleDrops())
						break;
				}

			} else {
				Debugger.debug("drops", "Dropping to player inventory");

				final List<Player> playersToReward = boss.getDropsManager().getPlayersToReward();

				for (final Player toReward : playersToReward) {
					for (final BossDrop drop : drops)
						if (drop != null && drop.getItem() != null && !drop.getItem().getType().toString().contains("AIR") && RandomUtil.chance(MathUtil.ceiling(drop.getDropChance() * 100))) {
							final Map<Integer, ItemStack> remain = toReward.getInventory().addItem(drop.getItem());

							Debugger.debug("drops", "Giving " + drop.getItem() + " to " + toReward.getName());

							if (!remain.isEmpty()) {
								Debugger.debug("drops", toReward.getName() + " Has a full inventory - dropping next to player, instead");

								for (final ItemStack remainingItem : remain.values())
									toReward.getWorld().dropItemNaturally(toReward.getLocation(), remainingItem);
							}

							if (boss.getSettings().hasSingleDrops())
								break;
						}

					BossUtil.runCommands(toReward, new SpawnedBoss(boss, entity), boss.getDeathByPlayerCommands());
				}

				boss.getDropsManager().clearAll();
			}
		}

		{ // Experience
			final RangedRandomValue ranged = boss.getSettings().getDroppedExp();

			if (ranged != null) {
				final int amount = ranged.getRandom();
				final Player killer = entity.getKiller();

				if (killer != null && BossHeroesHook.isEnabled()) {
					event.setDroppedExp(0);

					BossHeroesHook.gainKillExp(event.getEntity().getLocation(), killer, amount);

				} else
					event.setDroppedExp(amount);
			}
		}

		{ // Reinforcements
			// Boss
			for (final Map.Entry<String, Integer> entry : boss.getReinforcementsBoss().entrySet()) {
				final String name = entry.getKey();
				final Boss reinforcement = BossPlugin.getBossManager().findBoss(name);

				if (reinforcement == null)
					Common.log("&cWARNING: &fCannot spawn reinforcement as the Boss '" + name + "' does not exist (if the name contains a number, edit your Boss yml file and add ' ,' between Boss name and the quantity");

				else {
					final Location loc = entity.getLocation();
					final BossSpawnReason reason = BossSpawnReason.REINFORCEMENTS;

					for (int i = 0; i < entry.getValue(); i++)
						if (Settings.Limits.APPLY_REINFORCEMENTS)
							BossConditionedSpawnUtil.spawnConditioned(reinforcement, loc, reason);
						else
							reinforcement.spawn(loc, reason);
				}
			}

			// Vanilla
			for (final Map.Entry<String, Integer> entry : boss.getReinforcementsVanilla().entrySet()) {
				final String name = entry.getKey();
				final EntityType type = ReflectionUtil.lookupEnum(EntityType.class, name, "Unknown reinforcement entity '" + name + "'! Available: {available}");

				for (int i = 0; i < entry.getValue(); i++)
					entity.getWorld().spawnEntity(entity.getLocation(), type);
			}
		}

		// Commands
		BossUtil.runCommands(null, new SpawnedBoss(boss, entity), boss.getDeathCommands());

		if (!Remain.hasScoreboardTags())
			SimpleTagData.$().removeTagIfExists(entity.getUniqueId());

		BossPlugin.getBossManager().removeCachedEntity(entity.getUniqueId());
	}

	@EventHandler
	public void onPortal(PortalCreateEvent event) {
		if (event.getEntity() instanceof EnderDragon) {
			final Boss boss = BossPlugin.getBossManager().findBoss(event.getEntity());

			if (boss != null)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();

		// Get the parent, if dragon
		if (entity instanceof ComplexEntityPart)
			entity = ((ComplexEntityPart) entity).getParent();

		final Boss boss = BossPlugin.getBossManager().findBoss(entity);

		if (boss != null)
			if (entity instanceof EnderDragon && !(boolean) boss.getSettings().getSpecificSetting(BossSpecificSetting.ENDERDRAGON_GRIEF))
				event.blockList().clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBurn(final EntityCombustEvent event) {
		if (event.getEntity() == null)
			return;

		final Entity en = event.getEntity();
		Boss boss = BossPlugin.getBossManager().findBoss(en);

		if (boss == null && en.hasMetadata("BossMinion")) {
			final String motherBoss = en.getMetadata("BossMinion").get(0).asString();

			boss = BossPlugin.getBossManager().findBoss(motherBoss);
		}

		if (boss != null) {
			final boolean burn = (boolean) boss.getSettings().getSpecificSetting(BossSpecificSetting.HOSTILE_SUN_BURN);

			if (!burn) {
				event.setCancelled(true);

				en.setFireTicks(0);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFight(final EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity))
			return;

		final LivingEntity victim = (LivingEntity) event.getEntity();
		final Entity damagerRaw = event.getDamager();
		final Player damager = findDamagerPlayer(damagerRaw);
		final double damage = Remain.getFinalDamage(event);

		// Display health bar or add the damager to last players
		// only if it is player and actually did hurt the boss, emotionally
		if (damager != null && damage > 0) {
			final Boss boss = BossPlugin.getBossManager().findBoss(victim);

			if (boss != null) {
				if (boss.getSettings().hasInventoryDrops())
					boss.getDropsManager().registerDamage(damager, damage);

				if (Settings.Fight.Target.OVERRIDE_ON_ATTACK && BossUtil.canBossTarget(damager))
					((Creature) victim).setTarget(damager);

				if (Settings.Fight.HEALTH_BAR)
					Common.runLater(1, () -> HealthBarUtil.display(damager, victim, 0));
			}
		} else {
			Boss boss = BossPlugin.getBossManager().findBoss(damagerRaw);

			if (boss == null && damagerRaw instanceof Projectile && ((Projectile) damagerRaw).getShooter() instanceof LivingEntity)
				boss = BossPlugin.getBossManager().findBoss((LivingEntity) ((Projectile) damagerRaw).getShooter());

			if (boss != null) {
				Debugger.debug("damage", boss.getName() + " damaged " + victim + ". Damage is: " + event.getDamage() + " * (multiplier " + boss.getSettings().getDamageMultiplier() + ")");

				event.setDamage(event.getDamage() * boss.getSettings().getDamageMultiplier());
			}
		}

	}

	private Player findDamagerPlayer(final Entity entity) {
		if (entity instanceof Player)
			return (Player) entity;

		if (entity instanceof Projectile && ((Projectile) entity).getShooter() instanceof Player)
			return (Player) ((Projectile) entity).getShooter();

		return null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEndermanTeleport(final EntityTeleportEvent event) {
		if (!(event.getEntity() instanceof Enderman))
			return;

		final Boss boss = BossPlugin.getBossManager().findBoss(event.getEntity());

		if (boss != null && !(boolean) boss.getSettings().getSpecificSetting(BossSpecificSetting.ENDERMAN_TELEPORT))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTarget(final EntityTargetEvent event) {
		if (!(event.getEntity() instanceof Creature) || !(event.getTarget() instanceof LivingEntity))
			return;

		final LivingEntity target = (LivingEntity) event.getTarget();
		final Creature en = (Creature) event.getEntity();

		// Try to find the boss from target, and ...
		final Boss targetBoss = BossPlugin.getBossManager().findBoss(target);

		Debugger.debug("target", en + " is targetting " + target + " due to " + event.getReason());

		// disable targeting the Boss, if set.
		if (targetBoss != null && !(boolean) targetBoss.getSettings().getSpecificSetting(BossSpecificSetting.TARGETABLE)) {
			event.setCancelled(true);
			Debugger.debug("target", "PREVENT due to entity being a boss with targetable flag off");

			return;
		}

		else
			Debugger.debug("target", "PASS, is boss? " + targetBoss + " either this is false or targetable flag is off");

		final Boss boss = BossPlugin.getBossManager().findBoss(en);

		// Settings below are only applied to Bosses
		if (boss == null)
			return;

		if (HookManager.isNPC(target)) {
			event.setCancelled(true);

			return;
		}

		if (target instanceof Monster && en.getType() == EntityType.IRON_GOLEM && !(boolean) boss.getSettings().getSpecificSetting(BossSpecificSetting.GOLEM_AGGRESSIVE)) {
			event.setCancelled(true);

			return;
		}

		if (!(event.getTarget() instanceof Player))
			return;

		final Player targetPlayer = (Player) target;

		keep:
		{
			final String keepInside = BossTaggingUtil.getTagKeepInside(en);

			if (keepInside == null)
				break keep;

			final BossRegionSettings settings = boss.getSpawning().getRegions().findRegion(BossRegionType.BOSS, keepInside);

			if (settings == null || !settings.getKeepInside())
				break keep;

			final Region rg = BossRegionType.BOSS.getBoundingBox(keepInside);

			if (rg != null && !rg.isWithin(targetPlayer.getLocation())) {
				event.setCancelled(true);

				return;
			}
		}
	}
}

final class CompBossDismountListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDismount(final EntityDismountEvent event) {
		final Entity en = event.getEntity();
		final Entity dismounted = event.getDismounted();
		final Boss boss = BossPlugin.getBossManager().findBoss(en);

		if (boss != null && dismounted != null && dismounted.isValid()) {
			Debugger.put("mount", "Boss " + boss.getName() + " got dismounted from " + dismounted.getType());

			if (boss.getSettings().isRemovingRidingOnDeath()) {
				Debugger.push(" - removing dismount as per Boss settings");

				dismounted.remove();
			} else
				try {
					Debugger.push(" - preventing dismount");

					event.setCancelled(true);
				} catch (final NoSuchMethodError err) {
				}
		}
	}
}

final class CompBossPickupListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPickupItem(final EntityPickupItemEvent e) {
		final Entity en = e.getEntity();
		final Boss boss = BossPlugin.getBossManager().findBoss(en);

		if (boss != null)
			if (!Boolean.valueOf(boss.getSettings().getSpecificSetting(BossSpecificSetting.PICKUP_ITEMS).toString()))
				e.setCancelled(true);
	}
}

final class CompBossPortalCreateEvent implements Listener {

	@EventHandler
	public void onPortal(EntityCreatePortalEvent event) {
		final Boss boss = BossPlugin.getBossManager().findBoss(event.getEntity());

		if (boss != null && event.getEntity() instanceof EnderDragon)
			event.setCancelled(true);
	}
}