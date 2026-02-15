package org.mineacademy.boss.listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.event.BossDeathEvent;
import org.mineacademy.boss.custom.CustomSetting;
import org.mineacademy.boss.hook.CitizensHook;
import org.mineacademy.boss.hook.HeroesHook;
import org.mineacademy.boss.hook.ModelEngineHook;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.boss.model.BossCheatDisable;
import org.mineacademy.boss.model.BossCommandType;
import org.mineacademy.boss.model.BossReinforcement;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.BossSpawnResult;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.settings.Settings.PreventVanillaMobs;
import org.mineacademy.boss.spawn.SpawnData;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.HealthBarUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * The Boss listener.
 */
@AutoRegister
public final class EntityListener extends BossListener {

	/**
	 * Register new listeners unavailable for old MC versions.
	 */
	public EntityListener() {

		// Entity pickup item
		try {
			Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
			Platform.registerEvents(new CompBossPickupListener());

		} catch (final ClassNotFoundException ex) {
		}

		// Entity dismount
		try {
			Class.forName("org.bukkit.event.entity.EntityDismountEvent");
			Platform.registerEvents(new NewDismountEventListener());

		} catch (final ClassNotFoundException | NoClassDefFoundError ex) {
		}

		try {
			Class.forName("org.spigotmc.event.entity.EntityDismountEvent");
			Platform.registerEvents(new CompSpigotBossDismountListener());

		} catch (final ClassNotFoundException | NoClassDefFoundError ex) {
		}

		// Armor stand
		try {
			Class.forName("org.bukkit.event.player.PlayerArmorStandManipulateEvent");
			Platform.registerEvents(new CompArmorStandEvent());

		} catch (final ClassNotFoundException ex) {
		}

		// Entity Transform
		try {
			Class.forName("org.bukkit.event.entity.EntityTransformEvent");
			Platform.registerEvents(new CompEntityTransformEvent());

		} catch (final ClassNotFoundException ex) {
		}

		if (MinecraftVersion.atLeast(V.v1_14))
			Platform.registerEvents(new CreatureSpawnWhileChunkGenListener());

		if (MinecraftVersion.olderThan(V.v1_9))
			Platform.registerEvents(new CompBossPortalCreateEvent());
	}

	/* ------------------------------------------------------------------------------- */
	/* Spawning */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Prevent vanilla mobs and handle replacing vanilla with Bosses.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSpawn(CreatureSpawnEvent event) {

		// Check only after our Boss tag has been added
		Platform.runTask(1, () -> handleEntityReplace(event.getEntity(), event.getSpawnReason()));
	}

	/**
	 * Handles the spawn rule Replace Vanilla.
	 *
	 * @param entity
	 * @param spawnReason
	 */
	static void handleEntityReplace(Entity entity, SpawnReason spawnReason) {

		// Ignore Bosses
		if (Boss.findBoss(entity) != null)
			return;

		// Prevent vanilla mobs
		if (PreventVanillaMobs.ENABLED && PreventVanillaMobs.WORLDS.contains(entity.getWorld().getName())
				&& PreventVanillaMobs.SPAWN_REASONS.contains(spawnReason) && PreventVanillaMobs.ENTITY_TYPES.contains(entity.getType())) {

			entity.remove();
			return;
		}

		// Run the replace vanilla spawn rule if the spawn reason is not ignored
		if (!Settings.Spawning.IGNORE_REPLACING_VANILLA_FROM.contains(spawnReason)) {
			final SpawnData spawned = SpawnRule.tick(SpawnData.fromVanillaReplace(entity), SpawnRuleType.REPLACE_VANILLA);

			if (!spawned.getBosses().isEmpty() || Settings.Spawning.CANCEL_VANILLA_IF_REPLACE_FAILS)
				entity.remove();
		} else
			Debugger.debug("spawning", "Ignoring replacing vanilla mob from creature spawn event due to spawn reason being ignored (" + spawnReason + ")");
	}

	/**
	 * Spawn from a dispenser.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onDispense(final BlockDispenseEvent event) {
		final BlockState state = event.getBlock().getState();

		// Ignore droppers etc.
		if (!(state instanceof Dispenser))
			return;

		final Dispenser dispenser = (Dispenser) state;
		final ItemStack dispensedItem = event.getItem();

		if (event.getItem() == null || !CompMaterial.isMonsterEgg(event.getItem().getType()) || !(event.getBlock().getState() instanceof Dispenser))
			return;

		final String bossName = Boss.findBossName(event.getItem());

		if (bossName == null)
			return;

		final Boss boss = Boss.findBoss(bossName);

		if (boss == null) {
			Common.log("Cannot dispense '" + bossName + "' at " + SerializeUtil.serializeLocation(dispenser.getLocation()) + " as the boss is not installed on this server.");

			event.setCancelled(true);
			return;
		}

		// Remove item
		Platform.runTask(1, () -> {
			final ItemStack[] content = dispenser.getInventory().getContents();

			for (int slot = 0; slot < content.length; slot++) {
				final ItemStack slotItem = content[slot];

				if (slotItem != null && ItemUtil.isSimilar(slotItem, dispensedItem)) {
					slotItem.setAmount(slotItem.getAmount() - 1);

					content[slot] = slotItem.getAmount() == 0 ? null : slotItem;
					break;
				}
			}

			dispenser.getInventory().setContents(content);
		});

		// Spawn boss
		final BlockFace face = this.getFace(dispenser);
		final Location location = dispenser.getLocation();

		this.adjustLocation(location, face);

		boss.spawn(location, BossSpawnReason.DISPENSE);
		event.setCancelled(true);
	}

	/*
	 * Return block face of the dispenser
	 */
	private BlockFace getFace(Dispenser dispenser) {

		if (MinecraftVersion.atLeast(V.v1_13)) {
			final org.bukkit.block.data.type.Dispenser dispenserData = (org.bukkit.block.data.type.Dispenser) dispenser.getBlockData();

			return dispenserData.getFacing();
		}

		switch (dispenser.getData().getData()) {
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

		throw new FoException("Unable to get dispensor face from " + dispenser);
	}

	/*
	 * Adjusts the location so that Boss spawns in front of the dispenser
	 */
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

	/* ------------------------------------------------------------------------------- */
	/* Death */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Prevent death messages for Bosses.
	 *
	 * @param event
	 */
	@EventHandler
	public void onPlayerNPCDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();

		if (HookManager.isNPC(player) && Boss.findBoss(player) != null)
			event.setDeathMessage(null);
	}

	/**
	 * Oh my, the Boss is dead!
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = false)
	public void onDeath(EntityDeathEvent event) {
		final Entity passenger = event.getEntity().getPassenger();

		// Set NBT tag to the passenger to prevent live updates re-setting riding if it was already killed
		if (passenger instanceof LivingEntity)
			this.runIfBoss(passenger, spawnedBoss -> CompMetadata.setMetadata(passenger, Boss.RIDING_KILLED_TAG, "true"));

		this.runIfBoss(event.getEntity(), spawnedBoss -> {
			final LivingEntity entity = spawnedBoss.getEntity();
			final Boss boss = spawnedBoss.getBoss();
			final Player killer = entity.getKiller();
			final Location location = entity.getLocation();
			final List<ItemStack> drops = event.getDrops();
			final int droppedExp = boss.getDroppedExp() == null ? event.getDroppedExp() : boss.getDroppedExp().getRandomInt();

			Debugger.debug("drops", "Boss " + boss.getName() + " at " + SerializeUtil.serializeLocation(location) + " has died. Collecting drops.");

			// Handle experience
			if (killer != null && HeroesHook.isEnabled()) {
				event.setDroppedExp(0);

				HeroesHook.giveKillExp(location, killer, droppedExp);

			} else
				event.setDroppedExp(droppedExp);

			if (killer != null)
				boss.registerKill(killer);

			// Clear up hanging entities
			if (boss.getRemoveRidingOnDeath())
				Platform.runTask(() -> EntityUtil.removeVehiclesAndPassengers(entity));

			// Strike lightning
			if (boss.hasLightningOnDeath())
				entity.getWorld().strikeLightningEffect(location);

			// Spawn reinforcements
			for (final BossReinforcement reinforcement : boss.getReinforcements())
				reinforcement.spawn(location);

			// Clear vanilla drops
			if (!boss.hasVanillaDrops()) {
				Debugger.debug("drops", "Clearing vanilla drops.");

				drops.clear();

				// Readding equipment
				for (final CompEquipmentSlot slot : CompEquipmentSlot.values()) {
					final ItemStack item = boss.getEquipmentItem(slot);

					if (item != null)
						Debugger.debug("drops", "Trying to drop " + slot + " equipment. Found item type " + item.getType() + " with chance " + boss.getEquipmentDropChance(slot));

					if (item != null && !CompMaterial.isAir(item) && RandomUtil.chanceD(boss.getEquipmentDropChance(slot))) {
						Debugger.debug("drops", "\tSUCCESS!");

						entity.getWorld().dropItem(entity.getLocation(), item);
					}
				}
			}

			// Custom drops
			for (final Tuple<ItemStack, Double> tuple : boss.getGeneralDrops()) {
				final ItemStack item = tuple != null ? tuple.getKey() : null;

				if (item != null)
					Debugger.debug("drops", "Trying to drop custom item type " + item.getType() + " with chance " + tuple.getValue());

				if (item != null && !CompMaterial.isAir(item) && RandomUtil.chanceD(tuple.getValue())) {
					Debugger.debug("drops", "\tSUCCESS!");

					entity.getWorld().dropItem(entity.getLocation(), item);
				}

			}

			// Call API
			Platform.callEvent(new BossDeathEvent(boss, entity, drops, event));

			// Player drops
			int damageOrder = 0;

			for (final Map.Entry<Double, Player> damageEntry : boss.getRecentDamagerPlayer(entity, true).entrySet()) {
				final double damage = damageEntry.getKey();
				final Player damager = damageEntry.getValue();

				// Drops
				final Map<ItemStack, Double> playerDrops = boss.getPlayerDrops(damageOrder);

				Debugger.debug("drops", "Player " + damager.getName() + " at " + SerializeUtil.serializeLocation(damager.getLocation()) + " has dealt " + damage + " damage to the Boss. Collecting drops: " + playerDrops);

				if (playerDrops != null) {
					final List<ItemStack> playerReward = new ArrayList<>();

					for (final Map.Entry<ItemStack, Double> dropEntry : playerDrops.entrySet()) {
						final ItemStack item = dropEntry.getKey();

						if (item != null && !CompMaterial.isAir(item))
							if (RandomUtil.chanceD(dropEntry.getValue())) {
								Debugger.debug("drops", "\tADDING ITEM TYPE " + item.getType());

								playerReward.add(item);
							} else
								Debugger.debug("drops", "\tSKIPPING ITEM TYPE " + item.getType() + " WITH CHANCE " + dropEntry.getValue());
					}

					final Map<Integer, ItemStack> itemsFailedToStore = PlayerUtil.addItems(damager.getInventory(), playerReward);

					for (final ItemStack floorItem : itemsFailedToStore.values())
						damager.getWorld().dropItem(damager.getLocation(), floorItem);

					if (!itemsFailedToStore.isEmpty())
						Messenger.info(damager, Lang.component("player-full-inventory"));
				}

				// Console command
				final List<String> commands = boss.getPlayerDropsCommands(damageOrder);

				for (final String command : commands)
					boss.runCommand(command
							.replace("{damage}", MathUtil.formatTwoDigits(damage))
							.replace("{order}", String.valueOf(damageOrder + 1)), Settings.Death.RUN_PVP_COMMANDS_AS_CONSOLE, damager, entity);

				damageOrder++;
			}

			// Clear up
			boss.clearLastTriggerCommands(entity);

			// Run commands
			boss.runCommands(BossCommandType.DEATH, entity);

			// Used above, so clear up now
			boss.clearRecentDamagers(entity);

			// Remove from Citizens to prevent reappearing on restart
			if (HookManager.isCitizensLoaded())
				HookManager.destroyNPC(entity);

			// Schedule respawn task
			final String spawnRuleName = spawnedBoss.getSpawnRuleName();

			if (spawnRuleName != null)
				boss.setLastDeathFromSpawnRule(spawnRuleName);
		});
	}

	/* ------------------------------------------------------------------------------- */
	/* Damage */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Handle player vs Boss and Boss vs others.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onDamage(EntityDamageByEntityEvent event) {
		Entity victim = event.getEntity();
		Entity damager = event.getDamager();

		if (victim instanceof Projectile && ((Projectile) victim).getShooter() instanceof LivingEntity)
			victim = (LivingEntity) ((Projectile) victim).getShooter();

		if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity)
			damager = (LivingEntity) ((Projectile) damager).getShooter();

		final Player victimPlayer = victim instanceof Player ? (Player) victim : null;
		final LivingEntity victimLivingEntity = victim instanceof LivingEntity ? (LivingEntity) victim : null;

		// If Boss attacked something, adjust damage
		this.runIfBoss(damager, spawnedBoss -> {
			final LivingEntity entity = spawnedBoss.getEntity();
			final Boss boss = spawnedBoss.getBoss();

			// Play attack animation if Boss uses ModelEngine custom models and has attack animations defined
			if (boss.isUseCustomModel() && boss.isUseCustomAttackAnimation() && !boss.getCustomAttackAnimations().isEmpty())
				ModelEngineHook.playAnimation(entity, boss.getRandomCustomAttackAnimation());

			// Adjust damage
			/*
			 * Previous approach:
			 * Used Remain#getFinalDamage(event), but this caused inconsistencies
			 * with high-level enchantments and custom attributes.
			 *
			 * Fix:
			 * The new implementation uses event.getDamage() directly for a more
			 * predictable and consistent damage calculation.
			 */

			// Trying previous approach to help identify root cause of damage inconsistencies.
			final double originalDamage = Remain.getFinalDamage(event);
			final double newDamage = MathUtil.range(originalDamage, 1, Double.MAX_VALUE) * boss.getAttribute(BossAttribute.DAMAGE_MULTIPLIER);

			Debugger.debug("damage", "Original (final) damage: " + originalDamage + ". After applying damage multiplier attribute: " + newDamage + ".");

			event.setDamage(newDamage);

			if (victimPlayer != null)
				this.disableCheats(victimPlayer);

			// If the Boss killed the victim
			if (victimLivingEntity != null && Remain.getHealth(victimLivingEntity) - newDamage <= 0 && HookManager.isCitizensLoaded())
				CitizensHook.retarget(new SpawnedBoss(boss, entity));
		});

		// If player attacked Boss, show health
		if (damager instanceof Player) {
			final Player damagerPlayer = (Player) damager;

			this.runIfBoss(victim, spawnedBoss -> {
				final LivingEntity entity = spawnedBoss.getEntity();
				final Boss boss = spawnedBoss.getBoss();

				/*
				 * Use final damage (after armor/enchantment reductions) for accurate health tracking.
				 * The health bar display runs 5 ticks later when entity health is already updated,
				 * so we pass 0 to avoid double-subtracting the damage.
				 */

				final double damage = Remain.getFinalDamage(event) * Double.parseDouble("1.0" + RandomUtil.nextInt(30));
				final double remainingHealth = Remain.getHealth(entity) - damage;

				// Run commands when Boss takes damage below given thresholds
				boss.runTriggerCommands(damagerPlayer, entity, remainingHealth);

				// Show health bar (damage already applied by the time this runs, so pass 0)
				Platform.runTask(5, () -> {
					if (Settings.Fighting.HealthBar.ENABLED) {
						final SimpleComponent fightMessage = SimpleComponent.fromMiniAmpersand(Settings.Fighting.HealthBar.FORMAT.replace("{damage}", MathUtil.formatTwoDigits(damage)));

						HealthBarUtil.display(damagerPlayer, entity, boss.replaceVariables(fightMessage, entity, damagerPlayer), 0);
					}
				});

				Debugger.debug("damage", "[Boss " + boss.getName() + "] Final damage: " + MathUtil.formatTwoDigits(damage)
						+ ". Health: " + MathUtil.formatTwoDigits(entity.getHealth()) + "/" + MathUtil.formatTwoDigits(entity.getMaxHealth()) + ". Remaining: " + MathUtil.formatTwoDigits(remainingHealth));

				// Register damage
				boss.registerDamage(entity, damagerPlayer, damage);

				// Re-register target
				if (entity instanceof Creature && RandomUtil.chanceD(Settings.Fighting.RETARGET_CHANCE))
					if (damagerPlayer.getGameMode() == GameMode.SURVIVAL || damagerPlayer.getGameMode() == GameMode.ADVENTURE)
						((Creature) entity).setTarget(damagerPlayer);

				this.disableCheats(damagerPlayer);
			});
		}

		if (victim.getType().toString().equals("ARMOR_STAND"))
			// Prevent destroying armor stand bosses if they are invulnerable
			this.runIfBoss(victim, spawnedBoss -> {
				if (spawnedBoss.getBoss().getCustomSetting(CustomSetting.INVULNERABLE))
					event.setCancelled(true);
			});
	}

	/*
	 * Prevent Boss from regenerating health.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRegainHealth(EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof LivingEntity && Settings.Health.PREVENT_REGENERATION) {
			this.runIfBoss(event.getEntity(), spawnedBoss -> {
				final Boss boss = spawnedBoss.getBoss();
				final LivingEntity entity = spawnedBoss.getEntity();

				Debugger.debug("health", "[regen] [Boss " + boss.getName() + "] Cancelling regaining " + MathUtil.formatTwoDigits(event.getAmount())
						+ " HP. Current health: " + MathUtil.formatTwoDigits(entity.getHealth()) + "/" + MathUtil.formatTwoDigits(entity.getMaxHealth()));

				event.setAmount(0);
				event.setCancelled(true);
			});
		}
	}

	/*
	 * Disable player cheats when attacking/attacked by a Boss.
	 */
	private void disableCheats(Player player) {
		final Set<String> disabledAbilities = new HashSet<>();
		final Set<BossCheatDisable> cheats = Settings.Fighting.DISABLE_CHEATS;

		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode().toString().equals("SPECTATOR"))
			return;

		if (cheats.contains(BossCheatDisable.FLIGHT) && (player.isFlying() || player.getAllowFlight())) {
			player.setFlying(false);
			player.setAllowFlight(false);

			disabledAbilities.add(Lang.legacy("fight-cheats-flight"));
		}

		if (cheats.contains(BossCheatDisable.GOD_MODE) && Remain.isInvulnerable(player)) {
			Remain.setInvulnerable(player, false);

			disabledAbilities.add(Lang.legacy("fight-cheats-god-mode"));
		}

		if (cheats.contains(BossCheatDisable.GOD_MODE) && HookManager.hasGodMode(player)) {
			HookManager.setGodMode(player, false);

			disabledAbilities.add(Lang.legacy("fight-cheats-god-mode"));
		}

		if (cheats.contains(BossCheatDisable.INVISIBILITY) && PlayerUtil.isVanished(player)) {
			PlayerUtil.setVanished(player, false);

			disabledAbilities.add(Lang.legacy("fight-cheats-vanish"));
		}

		if (!disabledAbilities.isEmpty())
			Messenger.warn(player, Lang.component("fight-cheats-cheats-disabled", "abilities", String.join(", ", disabledAbilities)));
	}

	/* ------------------------------------------------------------------------------- */
	/* Targetting */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Control targetting.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTarget(EntityTargetEvent event) {
		if (!(event.getEntity() instanceof Creature) || !(event.getTarget() instanceof LivingEntity))
			return;

		final Creature fromEntity = (Creature) event.getEntity();
		final LivingEntity target = (LivingEntity) event.getTarget();

		// Try to find the boss from target, and ...
		final SpawnedBoss spawnedTargetBoss = Boss.findBoss(target);
		final Boss targetBoss = spawnedTargetBoss != null ? spawnedTargetBoss.getBoss() : null;

		// Disable targeting the Boss, if set.
		if (targetBoss != null && !targetBoss.getCustomSetting(CustomSetting.TARGETABLE)) {
			event.setCancelled(true);

			return;
		}

		final SpawnedBoss fromSpawnedBoss = Boss.findBoss(fromEntity);
		final Boss fromBoss = fromSpawnedBoss != null ? fromSpawnedBoss.getBoss() : null;

		// Settings below are only applied to Bosses
		if (fromBoss != null) {

			// Prevent targeting entities outside region
			if (fromBoss.isKeptInSpawnRegion() && fromSpawnedBoss.getSpawnRegion() != null && !fromSpawnedBoss.getSpawnRegion().isWithin(target.getLocation())) {
				fromEntity.setTarget(null);
				event.setCancelled(true);

				return;
			}

			if (!Boss.canTarget(target)) {
				event.setCancelled(true);

				return;
			}

			if (target instanceof Monster && fromEntity.getType() == CompEntityType.IRON_GOLEM && !fromBoss.getCustomSetting(CustomSetting.IRON_GOLEM_AGGRESSIVE)) {
				event.setCancelled(true);

				return;
			}
		}

		// Prevent bosses from targeting themselves
		if (fromBoss != null && targetBoss != null)
			event.setCancelled(true);
	}

	/* ------------------------------------------------------------------------------- */
	/* Boss options */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Prevent boss taming
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTame(final EntityTameEvent event) {
		final Entity entity = event.getEntity();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null)
			event.setCancelled(true);
	}

	/**
	 * Control Boss combusting.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBurn(final EntityCombustEvent event) {
		final Entity entity = event.getEntity();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null && !boss.getBoss().getCustomSetting(CustomSetting.SUN_DAMAGE)) {
			event.setCancelled(true);

			entity.setFireTicks(0);
		}
	}

	/**
	 * Control Enderdragon Boss griefing.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDragonGrief(EntityExplodeEvent event) {
		Entity entity = event.getEntity();

		// Get the parent, if dragon
		if (entity instanceof ComplexEntityPart)
			entity = ((ComplexEntityPart) entity).getParent();

		if (entity instanceof EnderDragon)
			this.runIfBoss(entity, spawnedBoss -> {
				if (!spawnedBoss.getBoss().getCustomSetting(CustomSetting.ENDERDRAGON_GRIEF))
					event.blockList().clear();
			});
	}

	/**
	 * Control Enderman Boss teleport.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEndermanTeleport(EntityTeleportEvent event) {

		if (!(event.getEntity() instanceof Enderman))
			return;

		this.runIfBoss(event.getEntity(), spawnedBoss -> {
			if (!spawnedBoss.getBoss().getCustomSetting(CustomSetting.ENDERMAN_TELEPORT))
				event.setCancelled(true);
		});
	}

	/**
	 * Control Slime Boss splitting.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSlimeSplit(final SlimeSplitEvent event) {
		this.runIfBoss(event.getEntity(), spawnedBoss -> {
			final LivingEntity entity = spawnedBoss.getEntity();
			final Boss boss = spawnedBoss.getBoss();
			final int size = ((Slime) entity).getSize();

			event.setCancelled(true);

			if (boss.getCustomSetting(CustomSetting.SLIME_BABIES_ON_DEATH))
				for (int i = 0; i < event.getCount(); i++) {
					final Tuple<BossSpawnResult, SpawnedBoss> babySlime = boss.spawn(entity.getLocation().add(Math.random(), 0, Math.random()), BossSpawnReason.SLIME_SPLIT);

					if (babySlime.getKey() == BossSpawnResult.SUCCESS)
						((Slime) babySlime.getValue().getEntity()).setSize(size - 1);
				}
		});
	}

	/**
	 * Control Snowman Boss forming a trail.
	 *
	 * @param event
	 */
	@EventHandler
	public void onSnowForm(EntityBlockFormEvent event) {
		final Entity entity = event.getEntity();
		final BlockState newState = event.getNewState();

		if (entity instanceof Snowman && newState.getType() == Material.SNOW)
			this.runIfBoss(event.getEntity(), spawnedBoss -> {
				event.setCancelled(true);

				event.getNewState().setType(Material.AIR);
			});
	}

	/**
	 * Prevent creating portal by Bosses.
	 *
	 * @param event
	 */
	@EventHandler
	public void onPortalCreate(PortalCreateEvent event) {
		try {
			if (event.getEntity() != null)
				this.runIfBoss(event.getEntity(), spawnedBoss -> event.setCancelled(true));

		} catch (final NoSuchMethodError err) {
			// Ignore old MC
		}
	}

}

/**
 * Legacy listener for dismounting event.
 */
final class NewDismountEventListener implements Listener {

	/**
	 * Control dismounting of Bosses.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDismount(final org.bukkit.event.entity.EntityDismountEvent event) {
		final Entity entity = event.getEntity();
		final Entity dismountedEntity = event.getDismounted();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null && dismountedEntity != null && dismountedEntity.isValid())
			if (boss.getBoss().getRemoveRidingOnDeath())
				dismountedEntity.remove();

			else if (!CompMetadata.hasTempMetadata(entity, "BossDontPreventVehicleExit"))
				try {
					event.setCancelled(true);
				} catch (final NoSuchMethodError err) {
				}
	}
}

/**
 * Legacy listener for dismounting event.
 */
final class CompSpigotBossDismountListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDismount(final org.spigotmc.event.entity.EntityDismountEvent event) {
		final Entity entity = event.getEntity();
		final Entity dismountedEntity = event.getDismounted();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null && dismountedEntity != null && dismountedEntity.isValid())
			if (boss.getBoss().getRemoveRidingOnDeath())
				dismountedEntity.remove();

			else if (!CompMetadata.hasTempMetadata(entity, "BossDontPreventVehicleExit"))
				try {
					event.setCancelled(true);
				} catch (final NoSuchMethodError err) {
				}
	}
}

/**
 * Legacy listener for pickup event.
 */
final class CompBossPickupListener implements Listener {

	/**
	 * Control Bosses picking items from the ground.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPickupItem(final EntityPickupItemEvent event) {
		final Entity entity = event.getEntity();
		final SpawnedBoss boss = Boss.findBoss(entity);

		if (boss != null && !boss.getBoss().getCustomSetting(CustomSetting.PICKUP_ITEMS))
			event.setCancelled(true);
	}
}

/**
 * Legacy listener for portal creation.
 */
final class CompBossPortalCreateEvent implements Listener {

	/**
	 * Prevent Bosses from creating portals.
	 *
	 * @param event
	 */
	@EventHandler
	public void onPortal(EntityCreatePortalEvent event) {
		final SpawnedBoss boss = Boss.findBoss(event.getEntity());

		if (boss != null)
			event.setCancelled(true);
	}
}

/**
 * Listener for armor stands Bosses.
 */
final class CompArmorStandEvent implements Listener {

	/**
	 * Prevent equipping armor from armor stand Bosses.
	 *
	 * @param event
	 */
	@EventHandler
	public void onPortal(PlayerArmorStandManipulateEvent event) {
		final SpawnedBoss boss = Boss.findBoss(event.getRightClicked());

		if (boss != null)
			event.setCancelled(true);
	}
}

final class CompEntityTransformEvent implements Listener {

	@EventHandler
	public void onTransform(EntityTransformEvent event) {

		final Entity entity = event.getEntity();
		final String bossTag = Boss.findBossName(entity);

		if (bossTag != null)
			try {
				event.setCancelled(true);
			} catch (final NoSuchMethodError err) {
			}
	}
}

final class CreatureSpawnWhileChunkGenListener implements Listener {

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if (event.isNewChunk() && !Settings.Spawning.IGNORE_REPLACING_VANILLA_FROM.contains(SpawnReason.CHUNK_GEN))
			for (final Entity entity : event.getChunk().getEntities())
				EntityListener.handleEntityReplace(entity, SpawnReason.CHUNK_GEN);
	}
}