package org.mineacademy.boss.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragon.Phase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.api.event.BossSpawnEvent;
import org.mineacademy.boss.custom.CustomSetting;
import org.mineacademy.boss.goal.GoalManager;
import org.mineacademy.boss.goal.GoalManagerCheck;
import org.mineacademy.boss.hook.CitizensHook;
import org.mineacademy.boss.hook.LandsHook;
import org.mineacademy.boss.hook.ScaleTrait;
import org.mineacademy.boss.hook.WorldGuardHook;
import org.mineacademy.boss.listener.ChunkListener;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.ConfigStringSerializable;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a simple boss
 */
public final class Boss extends YamlConfig implements ConfigStringSerializable {

	/**
	 * The custom NBT tag injected to entities to mark them
	 */
	public static final String NBT_TAG = "Boss_V4";

	/**
	 * The custom NBT tag indicating the riding entity was killed, to prevent
	 * spawning it again in live updating.
	 */
	public static final String RIDING_KILLED_TAG = "Boss_Riding_Killed";

	/**
	 * The custom NBT tag with the value of a region name to keep the Boss within,
	 * if enabled.
	 */
	public static final String REGION_TAG = "Boss_Region";

	/**
	 * The origin location where Boss spawned.
	 */
	public static final String SPAWN_LOCATION_TAG = "Boss_Spawn_Location";

	/**
	 * The spawn rule that spawned the Boss.
	 */
	public static final String SPAWN_RULE_TAG = "Boss_Spawn_Rule";

	/**
	 * The loaded bosses
	 */
	private static final ConfigItems<Boss> loadedBosses = ConfigItems.fromFolder("bosses", Boss.class,
			(Function<List<Boss>, List<Boss>>) list -> {
				if (Settings.SORT_BY_TYPE) {
					final Map<EntityType, List<Boss>> grouppedBosses = new TreeMap<>((first, second) -> first.name().compareTo(second.name()));

					for (final Boss boss : list) {
						final EntityType type = boss.getType();

						grouppedBosses.computeIfAbsent(type, k -> new ArrayList<>()).add(boss);
					}

					for (final List<Boss> typeBosses : grouppedBosses.values())
						Collections.sort(typeBosses,
								Comparator.comparing(Boss::getName, String.CASE_INSENSITIVE_ORDER));

					final List<Boss> sortedBosses = new ArrayList<>();

					for (final List<Boss> typeBosses : grouppedBosses.values())
						sortedBosses.addAll(typeBosses);

					return sortedBosses;

				} else {
					Collections.sort(list, Comparator.comparing(Boss::getName, String.CASE_INSENSITIVE_ORDER));

					return list;
				}
			});

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Main class content */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * The entity type of the Boss.
	 */
	@Getter
	private EntityType type;

	/**
	 * The alias, what shows in head and in the {boss_alias} variable.
	 */
	private String alias;

	/**
	 * The health given at Boss spawning.
	 */
	@Getter
	private double maxHealth;

	/**
	 * Amount of dropped exp on death.
	 */
	@Getter
	private RangedValue droppedExp;

	/**
	 * Initial Boss equipment with drop chances.
	 */
	private Map<CompEquipmentSlot, Tuple<ItemStack, Double>> equipment;

	/**
	 * Should we leave vanilla rules to give random equipment for not set slots on
	 * {@link #equipment}?
	 */
	@Getter
	private boolean emptyEquipmentRandomlyEquipped;

	/**
	 * Applied potion effects.
	 */
	private Map<PotionEffectType, Integer> potionEffects;

	/**
	 * What entities this Boss is riding?
	 */
	@Getter
	private List<EntityType> ridingEntitiesVanilla;

	/**
	 * What Bosses this Boss is riding?
	 */
	@Getter
	private List<String> ridingEntitiesBoss;

	/**
	 * Remove riding entities when Boss is killed?
	 */
	private boolean removeRidingOnDeath;

	/**
	 * Commands to trigger with different actions for this Boss.
	 */
	private List<BossCommand> commands;

	/**
	 * Should we only run the first successful command and then stop?
	 */
	@Getter
	private boolean commandsStoppedAfterFirst;

	/**
	 * Attributes applied to Boss.
	 */
	private Map<BossAttribute, Double> attributes;

	/**
	 * Should we strike lightning on death?
	 */
	private boolean lightningOnDeath;

	/**
	 * Should we strike lightning on spawn?
	 */
	private boolean lightningOnSpawn;

	/**
	 * What Boss reinforcement should we spawn?
	 */
	@Getter
	private List<BossReinforcement> reinforcements;

	/**
	 * The Boss custom settings.
	 */
	private Map<String, Object> customSettings;

	/**
	 * The Boss skills.
	 */
	private Map<String, BossSkill> skills;

	/**
	 * Should we allow vanilla rules to add drops for the Boss?
	 */
	private boolean vanillaDrops;

	/**
	 * A list of drops appearing on the floor when Boss dies with drop chances.
	 */
	private List<Tuple<ItemStack, Double>> generalDrops;

	/**
	 * A list of drops given the players from their TOP to LOWEST damage in the list
	 * order with drop chances.
	 */
	private List<Map<ItemStack, Double>> playerDrops;

	/**
	 * A list of commands run as console for the players from their TOP to LOWEST
	 * damage to Boss on death
	 * in the list order.
	 */
	private List<String> playerDropsCommands;

	/**
	 * How far back in time we should account for players damaging the Boss on its
	 * death?
	 */
	@Getter
	private SimpleTime playerDropsTimeThreshold;

	/**
	 * What spawn reasons should we limit by spawning limits?
	 */
	private Set<BossSpawnReason> limitReasons;

	/**
	 * Stop spawning more Bosses when there are so many bosses in the given radius
	 * (radius-limit key-value pair)
	 * of each player we find near the Boss.
	 */
	private Tuple<Integer /* Max Bosses */, Double/* Radius */> nearbyBossesLimit;

	/**
	 * Stop spawning more Bosses when there are as many as the limit in each world.
	 */
	private Map<String /* world name */, Integer /* boss limit */> worldLimit;

	/**
	 * Custom citizens rules.
	 */
	@Getter
	private BossCitizensSettings citizensSettings;

	/**
	 * Keep the Boss in the region he spawned in?
	 */
	@Getter
	private boolean keptInSpawnRegion;

	/**
	 * The location where we return Boss after he scaped from region, see
	 * {@link #keptInSpawnRegion}.
	 */
	@Getter
	private String escapeReturnLocation;

	/**
	 * The EGG material, by default it's MONSTER_EGG customized for the given
	 * {@link #getType()}
	 */
	@Getter
	private CompMaterial eggMaterial;

	/**
	 * The spawn egg title
	 */
	@Getter
	private String eggTitle;

	/**
	 * The spawn egg lore
	 */
	@Getter
	private List<String> eggLore;

	/**
	 * The last time the Boss spawned from a spawn rule died.
	 */
	private Map<String, Long> lastDeathFromSpawnRule;

	/**
	 * Whether the native attack goal is enabled for this Boss (persistent)
	 */
	@Getter
	private boolean nativeAttackGoalEnabled = false;

	//
	// Non saveable fields below
	//

	/*
	 * Damage cache for alive bosses, where we register the time of the damage with
	 * the player UUID and the damage amount.
	 */
	private final Map<UUID /* Boss UUID */, Map<UUID/* Player ID */, List<Tuple<Long /* When damage occured */, Double /*
																														 * Damage
																														 * amount
																														 */>>>> damageCache = new HashMap<>();

	/*
	 * Cache for commands run at life decrease, marking the last trigger so they
	 * wont be run twice.
	 */
	private final Map<UUID /* Boss UUID */, Double /* Last trigger */> triggerCommandCache = new HashMap<>();

	/*
	 * Non-saveable helper to avoid race condition when spawning riding Boss that
	 * spawns itself over and over again
	 */
	private boolean canAddRidingEntities = true;

	/*
	 * Non-saveable helper to save calculations and avoid spawning dummy boss each
	 * time
	 */
	private final Map<BossAttribute, Double> defaultAttributes = new LinkedHashMap<>();

	/*
	 * Non saveable helper to save calculations and only spawn dummy boss once
	 */
	private Double defaultHealth;

	/*
	 * Create a new Boss from file.
	 */
	private Boss(String name) {
		this(name, null);
	}

	/*
	 * Create a new Boss from command or menu.
	 */
	private Boss(@NonNull String name, @Nullable EntityType type) {
		this.type = type;

		this.setHeader(
				" -------------------------------------------------------------------------------------------------",
				" The main Boss configuration file. Send it to friends or publish online, we'll load it automatically.",
				" We recommend AGAINST editing it here. Use /boss menu instead. MineAcademy.org is not responsible",
				" for data loss, hair loss, your ego or thermonuclear war from mistakes in your edit.",
				" -------------------------------------------------------------------------------------------------");

		this.loadAndExtract(NO_DEFAULT, "bosses/" + name + ".yml");
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Loading */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {

		// Type is not null when Boss is created through API
		if (this.type == null) {
			if (!this.isSet("Type"))
				throw new FoException("Corrupted Boss file: " + this.getFile()
						+ ", lacks the 'Type' key to determine entity type for the boss.", false);

			this.type = this.get("Type", EntityType.class);
		}

		this.alias = this.getString("Alias");
		this.maxHealth = this.isSet("Health") ? this.getDouble("Health") : this.getDefaultHealth();
		final String xp = this.getString("Dropped_Exp");
		this.droppedExp = "default".equalsIgnoreCase(xp) || "def".equalsIgnoreCase(xp) || "-1".equals(xp)
				|| "-1 - -1".equals(xp) ? null : this.get("Dropped_Exp", RangedValue.class);
		this.equipment = this.loadEquipment();
		this.emptyEquipmentRandomlyEquipped = this.getBoolean("Random_Equipment_On_Empty_Slots", false);
		this.potionEffects = this.getMap("Potion_Effects", PotionEffectType.class, Integer.class);
		this.ridingEntitiesVanilla = this.getList("Riding.Vanilla", EntityType.class);
		this.ridingEntitiesBoss = this.getStringList("Riding.Boss");
		this.removeRidingOnDeath = this.getBoolean("Riding.Remove_On_Death", false);
		this.commands = this.getList("Commands", BossCommand.class, this);

		for (final BossCommand command : this.commands)
			Valid.checkBoolean(command.getType() != BossCommandType.SKILL,
					"Cannot put skill commands in 'Commands' key for Boss " + this.getFileName()
							+ " because they are stored for each skill separatedly. Remove them and configure in-game using GUI.");

		this.commandsStoppedAfterFirst = this.getBoolean("Commands_Stop_After_First", false);
		this.attributes = this.getMap("Attributes", BossAttribute.class, Double.class);
		this.lightningOnDeath = this.getBoolean("Lightning.Death", false);
		this.lightningOnSpawn = this.getBoolean("Lightning.Spawn", false);
		this.reinforcements = this.getList("Reinforcements", BossReinforcement.class);
		this.customSettings = this.getMap("Custom_Settings", String.class, Object.class);
		this.skills = this.loadSkills();
		this.vanillaDrops = this.getBoolean("Drops.Vanilla", true);
		this.generalDrops = this.loadGeneralDrops();
		this.playerDrops = this.loadPlayerDrops();
		this.playerDropsTimeThreshold = this.isSet("Drops.Player_Time_Threshold")
				? this.getTime("Drops.Player_Time_Threshold")
				: SimpleTime.fromString("15 seconds");
		this.playerDropsCommands = this.getList("Drops.Player_Commands", String.class);
		this.limitReasons = this.isSet("Limit_Reasons") ? this.getSet("Limit_Reasons", BossSpawnReason.class)
				: Common.newSet(BossSpawnReason.SPAWN_RULE);
		this.nearbyBossesLimit = this.getTuple("Limit.Nearby_Bosses", new Tuple<>(100, 20.0D), Integer.class,
				Double.class);

		this.worldLimit = this.getMap("Limit.Worlds", String.class, Integer.class);
		this.citizensSettings = BossCitizensSettings.deserialize(this.getMap("Citizens"), this);
		this.keptInSpawnRegion = this.getBoolean("Keep_In_Spawn_Region", false);
		this.escapeReturnLocation = this.getString("Return_Location_On_Escape_Spawn");
		this.eggMaterial = this.get("Egg.Material", CompMaterial.class);
		this.eggTitle = this.getString("Egg.Title");
		this.eggLore = this.getStringList("Egg.Lore");
		this.lastDeathFromSpawnRule = this.getMap("Last_Death_From_Spawn_Rule", String.class, Long.class);
		this.nativeAttackGoalEnabled = GoalManagerCheck.isAvailable() ? getBoolean("Native_Attack_Goal_Enabled", false) : false;

		this.initDefaultAttributes();

		// Save in case the values were adjusted
		this.save();
	}

	private Map<CompEquipmentSlot, Tuple<ItemStack, Double>> loadEquipment() {
		final Map<CompEquipmentSlot, Tuple<ItemStack, Double>> equipment = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : this.getMap("Equipment").entrySet()) {
			final CompEquipmentSlot slot = CompEquipmentSlot.fromKey(entry.getKey());
			final Tuple<ItemStack, Double> tuple = Tuple.deserialize(SerializedMap.fromObject(entry.getValue()),
					ItemStack.class, Double.class);

			equipment.put(slot, tuple);
		}

		return equipment;
	}

	private List<Map<ItemStack, Double>> loadPlayerDrops() {
		final List<Map<ItemStack, Double>> drops = new ArrayList<>();
		final Object obj = this.getObject("Drops.Player");

		if (obj != null) {
			if (obj instanceof ConfigurationSection)
				throw new FoException(
						"Drops.Player must be a list of maps, not a map of maps. Contact us to report this issue. Got: "
								+ ((ConfigurationSection) obj).getValues(false));

			if (!(obj instanceof List))
				throw new FoException("Drops.Player must be a list of maps, not a " + obj.getClass().getSimpleName()
						+ ". Contact us to report this issue. Got: " + obj);

			for (final Object raw : (List<?>) obj) {
				final Map<?, ?> map = (Map<?, ?>) raw;
				final Map<ItemStack, Double> converted = new LinkedHashMap<>();

				for (final Map.Entry<?, ?> entry : map.entrySet())
					if (entry.getKey() != null && entry.getValue() != null) {
						final ItemStack item = SerializeUtil.deserialize(SerializeUtil.Language.YAML, ItemStack.class,
								entry.getKey());
						final double dropChance = Double.parseDouble(entry.getValue().toString());

						converted.put(item, dropChance);
					}

				drops.add(converted);
			}
		}

		return drops;
	}

	private List<Tuple<ItemStack, Double>> loadGeneralDrops() {
		final List<Tuple<ItemStack, Double>> drops = new ArrayList<>();

		for (final Object raw : this.getList("Drops.General"))
			drops.add(raw == null ? null
					: Tuple.deserialize(SerializedMap.fromObject(raw), ItemStack.class, Double.class));

		return drops;
	}

	/*
	 * Helper to load skills
	 */
	private Map<String, BossSkill> loadSkills() {
		final Map<String, BossSkill> loaded = new LinkedHashMap<>();
		final Map<String, SerializedMap> savedSkills = this.getMap("Skills", String.class, SerializedMap.class);

		for (final String skillName : BossSkill.getSkillsNames()) {
			final SerializedMap settings = savedSkills.getOrDefault(skillName, new SerializedMap());

			if (!settings.isEmpty()) {
				final BossSkill instance = BossSkill.createInstance(skillName, this, settings);

				if (instance != null)
					loaded.put(skillName, instance);
			}
		}

		return loaded;
	}

	/*
	 * Spawns a dummy boss to extract default attributes from.
	 */
	private void initDefaultAttributes() {
		this.defaultAttributes.clear();

		if (this.type == CompEntityType.PLAYER) {
			this.defaultAttributes.put(BossAttribute.DAMAGE_MULTIPLIER, 1D);
			if (MinecraftVersion.atLeast(V.v1_20))
				this.defaultAttributes.put(BossAttribute.SCALE, 1D);

			return;
		}

		final Map<BossAttribute, Double> defaultValues = new LinkedHashMap<>();

		final World firstWorld = Bukkit.getWorlds().get(0);
		final Location spawnLocation = firstWorld.getSpawnLocation();
		final LivingEntity dummyBoss = (LivingEntity) firstWorld
				.spawnEntity(new Location(firstWorld, spawnLocation.getX(), 0, spawnLocation.getZ()), this.type);

		for (final BossAttribute attribute : BossAttribute.values())
			if (attribute.isAvailable(dummyBoss)) {
				final Double defaultValue = attribute.getDefaultValue(dummyBoss);

				if (defaultValue != null)
					defaultValues.put(attribute, defaultValue);
			}

		dummyBoss.remove();

		this.defaultAttributes.putAll(defaultValues);
	}

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#serialize()
	 */
	@Override
	protected void onSave() {
		this.set("Type", this.type);
		this.set("Alias", this.alias);
		this.set("Health", this.maxHealth);
		this.set("Dropped_Exp", this.droppedExp);
		this.set("Equipment", this.equipment);
		this.set("Random_Equipment_On_Empty_Slots", this.emptyEquipmentRandomlyEquipped);
		this.set("Potion_Effects", this.potionEffects);
		this.set("Riding.Vanilla", this.ridingEntitiesVanilla);
		this.set("Riding.Boss", this.ridingEntitiesBoss);
		this.set("Riding.Remove_On_Death", this.removeRidingOnDeath);
		this.set("Commands", this.commands);
		this.set("Commands_Stop_After_First", this.commandsStoppedAfterFirst);
		this.set("Attributes", this.attributes);
		this.set("Lightning.Death", this.lightningOnDeath);
		this.set("Lightning.Spawn", this.lightningOnSpawn);
		this.set("Reinforcements", this.reinforcements);
		this.set("Custom_Settings", this.customSettings);
		this.set("Skills", this.skills);
		this.set("Drops.Vanilla", this.vanillaDrops);
		this.set("Drops.General", this.generalDrops);
		this.set("Drops.Player", this.playerDrops);
		this.set("Drops.Player_Time_Threshold", this.playerDropsTimeThreshold);
		this.set("Drops.Player_Commands", this.playerDropsCommands);
		this.set("Limit_Reasons", this.limitReasons);
		this.set("Limit.Nearby_Bosses", this.nearbyBossesLimit);

		// Quote the world name:
		this.set("Limit.Worlds", this.worldLimit.entrySet().stream().collect(Collectors.toMap(entry -> {
			String name = entry.getKey();

			while (name.startsWith("'"))
				name = name.substring(1);

			while (name.endsWith("'"))
				name = name.substring(0, name.length() - 1);

			return name;

		}, Entry::getValue)));

		this.set("Citizens", this.citizensSettings);
		this.set("Return_Location_On_Escape_Spawn", this.escapeReturnLocation);
		this.set("Keep_In_Spawn_Region", this.keptInSpawnRegion);
		this.set("Egg.Material", this.eggMaterial);
		this.set("Egg.Title", this.eggTitle);
		this.set("Egg.Lore", this.eggLore);
		this.set("Last_Death_From_Spawn_Rule", this.lastDeathFromSpawnRule);
		set("Native_Attack_Goal_Enabled", this.nativeAttackGoalEnabled);

		// Automatically rerender all Bosses of this instance
		this.updateBosses();
	}

	/*
	 * Updates all alive bosses with latest properties.
	 */
	private void updateBosses() {
		if (Settings.Spawning.LIVE_UPDATES)
			for (final World world : Bukkit.getWorlds())
				for (final Entity entity : world.getEntities()) {
					final SpawnedBoss boss = findBoss(entity);

					if (boss != null && this.equals(boss.getBoss())) {
						boss.getBoss().applyProperties((LivingEntity) entity, true);

						if (HookManager.isCitizensLoaded())
							CitizensHook.update(boss.getBoss(), entity);
					}
				}
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Spawning */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Attempts to spawn a boss at the given location, always returning a result,
	 * with nullable spawnedboss on failure.
	 *
	 * @param location
	 * @param reason
	 *
	 * @return
	 */
	@Nullable
	public Tuple<BossSpawnResult, SpawnedBoss> spawn(Location location, BossSpawnReason reason) {
		return this.spawn(location, reason, null);
	}

	/**
	 * Attempts to spawn a boss at the given location, always returning a result,
	 * with nullable spawnedboss on failure.
	 *
	 * @param location
	 * @param reason
	 * @param spawnRule the spawn rule from which the boss spawned, or null if not
	 *                  applicable
	 *
	 * @return
	 */
	@Nullable
	public Tuple<BossSpawnResult, SpawnedBoss> spawn(Location location, BossSpawnReason reason,
			@Nullable SpawnRule spawnRule) {

		// Apply limits
		if (this.limitReasons.contains(reason) && this.canAddRidingEntities /*
																			 * bypass limits if we are adding a riding
																			 * entity
																			 */) {

			final World world = location.getWorld();
			final int individualWorldLimit = this.worldLimit.getOrDefault(world.getName(), -1);

			int otherSameBosses = ChunkListener.countUnloadedBosses(world, this);

			for (final SpawnedBoss otherBoss : Boss.findBossesAliveIn(world))
				if (otherBoss.getBoss().equals(this))
					otherSameBosses++;

			if (individualWorldLimit != -1 && otherSameBosses >= individualWorldLimit) {
				Debugger.debug("spawning",
						"[Boss=" + this.getName() + "] Failed to spawn due to it being over same Boss limit. Found: "
								+ otherSameBosses + " vs limit: " + individualWorldLimit);

				return new Tuple<>(BossSpawnResult.FAIL_LIMIT, null);
			}

			final int nearbyLimit = this.nearbyBossesLimit.getKey();
			final double nearbyRadius = this.nearbyBossesLimit.getValue();
			int sameBossesNearby = 0;

			for (final SpawnedBoss nearby : Boss.findBossesInRange(location, nearbyRadius))
				if (nearby.getBoss().equals(this))
					sameBossesNearby++;

			if (nearbyLimit != -1 && sameBossesNearby >= nearbyLimit) {
				Debugger.debug("spawning", "[Boss=" + this.getName() + "] Failed to spawn due to it being over limit ("
						+ nearbyLimit + "): " + sameBossesNearby + " nearby bosses around " + nearbyRadius + " blocks");

				return new Tuple<>(BossSpawnResult.FAIL_LIMIT, null);
			}

			else
				Debugger.debug("spawning",
						"[Boss=" + this.getName() + "] Continuing spawning due to it being within limit (" + nearbyLimit
								+ "): : " + sameBossesNearby + " nearby bosses around " + nearbyRadius + " blocks");

			if (Settings.Spawning.Integration.LANDS && !LandsHook.canSpawn(location, this)) {
				Debugger.debug("spawning",
						"[Boss=" + this.getName() + "] Failed to spawn due to Lands mob/animal/phanom flag denying it");

				return new Tuple<>(BossSpawnResult.FAIL_CANCELLED, null);
			}
		}

		// Spawn the entity
		final LivingEntity entity = this.summonEntity(location);

		if (entity == null)
			return new Tuple<>(BossSpawnResult.FAIL_CANCELLED, null);

		// Transform to Boss
		this.applyProperties(entity, false);

		// Apparently citizens returns non valid entity at this point but it is valid
		if (!HookManager.isCitizensLoaded() && (!entity.isValid() || entity.isDead())) {
			Debugger.debug("spawning", "[Boss=" + this.getName()
					+ "] Failed to spawn because properties failed to apply or he died prematurely (typically caused by other plugins or server settings disallowing mobs)");

			return new Tuple<>(BossSpawnResult.FAIL_CANCELLED, null);
		}

		// Create spawned boss to call kill() from API below
		final SpawnedBoss spawned = new SpawnedBoss(this, entity);

		// Allow developers to modify our Boss entity after properties were applied
		if (!Platform.callEvent(new BossSpawnEvent(this, entity, reason))) {
			Remain.removeEntityWithPassengersAndNPC(entity);

			Debugger.debug("spawning", "[Boss=" + this.getName() + "] Failed to spawn because API event was cancelled");
			return new Tuple<>(BossSpawnResult.FAIL_API_CANCELLED, null);
		}

		// Run commands
		this.runCommands(BossCommandType.SPAWN, entity);

		// Strike lightning
		if (this.lightningOnSpawn)
			entity.getWorld().strikeLightningEffect(entity.getLocation());

		// Find targets immediately
		if (HookManager.isCitizensLoaded())
			CitizensHook.retarget(spawned);

		// Find the first region the Boss is inside
		for (final DiskRegion region : DiskRegion.getRegions()) {
			final boolean within = region.isWithin(entity.getLocation());

			if (within) {
				CompMetadata.setMetadata(entity, REGION_TAG, region.getFileName());

				break;
			}
		}

		if (spawnRule != null)
			CompMetadata.setMetadata(entity, SPAWN_RULE_TAG, spawnRule.getName());

		CompMetadata.setMetadata(entity, SPAWN_LOCATION_TAG, SerializeUtil.serializeLocation(entity.getLocation()));

		return new Tuple<>(BossSpawnResult.SUCCESS, spawned);
	}

	/*
	 * Summons an entity (not a Boss yet) at the given location.
	 */
	private LivingEntity summonEntity(Location location) {

		// Fail spawning to avoid errors when Citizens has been uninstalled
		if (this.type == CompEntityType.PLAYER && !HookManager.isCitizensLoaded())
			return null;

		// Load the chunk if it is not
		final Chunk chunk = location.getChunk();

		if (!chunk.isLoaded())
			chunk.load(true);

		if (HookManager.isCitizensLoaded() && (this.type == CompEntityType.PLAYER || this.citizensSettings.isEnabled()))
			return (LivingEntity) CitizensHook.spawn(this, location);

		return (LivingEntity) location.getWorld().spawnEntity(location, this.type);
	}

	/*
	 * Applies our custom settings that will make the entity a Boss.
	 */
	private void applyProperties(LivingEntity entity, boolean keepOldHealth) {

		// Set health
		try {
			entity.setMaxHealth(this.maxHealth);

			if (!keepOldHealth)
				entity.setHealth(this.maxHealth);

		} catch (final Throwable t) {
			// Do not report to sentry since this is user configuration error
			Common.log("Failed to give " + this.getName() + " Boss health " + this.maxHealth
					+ " HP! Your server allows maximum of " + Remain.getMaxHealth()
					+ " HP. Increase your spigot.yml > settings.attribute.maxHealth.max");

			t.printStackTrace();
		}

		// Custom name
		if ("hidden".equals(this.alias))
			Remain.removeCustomName(entity);
		else
			Remain.setCustomName(entity, this.getAlias(), true);

		// Equipment
		if (this.type == CompEntityType.HORSE) {
			final Tuple<ItemStack, Double> tuple = this.equipment.get(CompEquipmentSlot.CHEST);
			final Horse horse = (Horse) entity;
			ItemStack item = tuple != null ? tuple.getKey() : null;

			// If somehow it got hacked in
			if (item != null && !item.getType().toString().contains("HORSE_ARMOR"))
				item = null;

			horse.getInventory().setArmor(item);

		} else
			for (final CompEquipmentSlot slot : CompEquipmentSlot.values()) {
				if (slot == CompEquipmentSlot.OFF_HAND && MinecraftVersion.olderThan(V.v1_9))
					continue;

				final Tuple<ItemStack, Double> tuple = this.equipment.get(slot);
				final ItemStack item = tuple != null ? tuple.getKey() : null;

				final boolean hasItem = item != null && !CompMaterial.isAir(item.getType());
				final Double dropChance = tuple != null ? tuple.getValue() : 0.0D;

				if (!this.emptyEquipmentRandomlyEquipped) {
					if (hasItem)
						slot.applyTo(entity, item, dropChance);
					else {
						// Take off vanilla equipment if disabled, needs to be delayed
						Platform.runTask(2, () -> {
							if (entity.isValid())
								slot.clear(entity);
						});
					}
				} else if (hasItem)
					slot.applyTo(entity, item, dropChance);
			}

		// Clear active potions and add our
		for (final PotionEffect currentEffect : entity.getActivePotionEffects())
			entity.removePotionEffect(currentEffect.getType());

		for (final Map.Entry<PotionEffectType, Integer> entry : this.potionEffects.entrySet()) {
			final PotionEffectType type = entry.getKey();
			final int level = entry.getValue();

			entity.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, level - 1), true);
		}

		// Riding remove old and re-add
		if (!CompMetadata.hasMetadata(entity, RIDING_KILLED_TAG)) {
			EntityUtil.removeVehiclesAndPassengers(entity);

			if (this.canAddRidingEntities && !this.citizensSettings.isEnabled()) {
				this.addRiding(entity, this.ridingEntitiesBoss, bossName -> {
					final Boss ridingBoss = Boss.findBoss(bossName);

					// Prevent infinite loop
					ridingBoss.canAddRidingEntities = false;

					final Tuple<BossSpawnResult, SpawnedBoss> spawned = ridingBoss.spawn(entity.getLocation(),
							BossSpawnReason.RIDING);

					ridingBoss.canAddRidingEntities = true;

					if (spawned.getKey() == BossSpawnResult.SUCCESS)
						return spawned.getValue().getEntity();

					return null;
				});

				this.addRiding(entity, this.ridingEntitiesVanilla,
						entityType -> entity.getLocation().getWorld().spawnEntity(entity.getLocation(), entityType));
			}
		}

		// Attributes
		for (final BossAttribute attribute : BossAttribute.values()) {

			if (attribute.equals(BossAttribute.SCALE) && this.type.equals(EntityType.PLAYER) && (this.attributes.containsKey(attribute) || this.defaultAttributes.containsKey(attribute))) {
				final double value = this.attributes.containsKey(attribute) ? this.attributes.get(attribute) : this.defaultAttributes.get(attribute);

				ScaleTrait.updateScale(entity, value);
				continue;
			}

			if (this.attributes.containsKey(attribute)) {
				final double value = this.attributes.get(attribute);

				try {
					attribute.apply(entity, value);

				} catch(final IllegalStateException e) {
					Common.logTimed(60 * 60, "Failed to apply attribute " + attribute + " to " + this.getName() + ": " + e.getMessage() + ". Most likely your server does not support this. This message will be shown once per hour.");
				}

			} else if (this.defaultAttributes.containsKey(attribute))
				attribute.apply(entity, this.defaultAttributes.get(attribute));
		}

		// Custom settings
		for (final CustomSetting<?> customSetting : CustomSetting.getRegisteredSettings()) {
			customSetting.setBoss(this);

			if (customSetting.canApplyTo(this.type))
				customSetting.onSpawn(this, entity);
		}

		if (this.type == CompEntityType.ENDER_DRAGON)
			try {
				final EnderDragon dragon = (EnderDragon) entity;

				dragon.setPhase(Phase.CIRCLING);
			} catch (final Throwable t) {
				// Legacy MC
			}

		try {
			if (this.type == CompEntityType.PIGLIN)
				((Piglin) entity).setImmuneToZombification(true);

		} catch (final Throwable t) {
			// Legacy MC
		}

		// Set the mob natively aggressive or not
		if(this.nativeAttackGoalEnabled && GoalManagerCheck.isAvailable() && entity instanceof Mob)
			GoalManager.makeAggressive((Mob) entity, true);

		// Finish by labeling this entity as Boss
		CompMetadata.setMetadata(entity, NBT_TAG, this.getName());
		CompMetadata.setTempMetadata(entity, NBT_TAG, this.getName());
	}

	/*
	 * Add riding entities for this Boss.
	 */
	private <T> void addRiding(LivingEntity boss, List<T> riding, Function<T, Entity> spawnerFunction) {

		if (riding.isEmpty())
			return;

		// Make copy
		riding = new ArrayList<>(riding);
		Collections.reverse(riding);

		// Legacy support
		if (!Remain.hasEntityAddPassenger()) {
			if (!riding.isEmpty()) {
				final Entity entity = spawnerFunction.apply(riding.get(0));
				this.fixIfHorse(entity);

				entity.setPassenger(boss);
			}

			return;
		}

		Entity stackEntity = null;

		for (int i = riding.size() - 1; i >= 0; i--) {

			if (stackEntity == null)
				stackEntity = spawnerFunction.apply(riding.get(i));

			// Something prevented the other boss from spawning
			if (stackEntity == null)
				continue;

			this.fixIfHorse(stackEntity);

			if (i - 1 < 0) {
				stackEntity.addPassenger(boss); // Compatibility handled above in Remain#hasAddPassenger

				return;
			}

			final Entity aboveEntity = spawnerFunction.apply(riding.get(i - 1));
			final boolean success = stackEntity.addPassenger(aboveEntity); // Compatibility handled above in
																			// Remain#hasAddPassenger

			if (success)
				stackEntity = aboveEntity;
		}
	}

	/*
	 * Prevent untamed horses kicking bosses out
	 */
	private void fixIfHorse(Entity entity) {
		if (entity instanceof Horse)
			((Horse) entity).setTamed(true);
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Commands */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Runs commands for this Boss.
	 *
	 * @param commandType
	 * @param bossEntity
	 */
	public void runCommands(BossCommandType commandType, LivingEntity bossEntity) {
		final Player killer = bossEntity.getKiller();
		final List<BossCommand> commands = this.getCommands(commandType);

		for (final BossCommand command : commands) {
			final boolean success = this.runCommand(command.getCommand(), command.getChance(), command.isConsole(),
					killer, bossEntity);

			if (success && this.commandsStoppedAfterFirst)
				break;
		}
	}

	/**
	 * Runs commands at life decrease for this Boss.
	 *
	 * @param damagerPlayer
	 * @param entity
	 * @param remainingHealth
	 */
	public void runTriggerCommands(Player damagerPlayer, LivingEntity entity, double remainingHealth) {
		final List<BossCommand> sorted = new ArrayList<>();
		final double lastTrigger = this.triggerCommandCache.getOrDefault(entity.getUniqueId(), -1D);

		BossCommand lastTriggerCommand = null;

		for (final BossCommand command : this.getCommands(BossCommandType.HEALTH_TRIGGER))
			if (command.getHealthTrigger() != null) {
				if (remainingHealth <= 0 && command.isIgnoreIfDead())
					continue;

				sorted.add(command);
			}

		// Sort from top to bottom
		Collections.sort(sorted, Comparator.comparing(BossCommand::getHealthTrigger).reversed());

		for (final BossCommand command : sorted) {
			final Double healthTrigger = command.getHealthTrigger();

			if (healthTrigger != null && remainingHealth <= healthTrigger
					&& (lastTrigger == -1 || healthTrigger < lastTrigger))
				lastTriggerCommand = command;
		}

		if (lastTriggerCommand != null) {
			this.runCommand(lastTriggerCommand.getCommand(), lastTriggerCommand.isConsole(), damagerPlayer, entity);

			this.triggerCommandCache.put(entity.getUniqueId(), lastTriggerCommand.getHealthTrigger());
		}
	}

	/**
	 * Called on Boss death to clear life decrease commands.
	 *
	 * @param entity
	 */
	public void clearLastTriggerCommands(LivingEntity entity) {
		this.triggerCommandCache.remove(entity.getUniqueId());
	}

	/**
	 * Run the specific command with the given arguments.
	 *
	 * @param command
	 * @param console
	 * @param player
	 * @param bossEntity
	 * @return
	 */
	public boolean runCommand(String command, boolean console, @Nullable Player player, LivingEntity bossEntity) {
		return this.runCommand(command, 1D, console, player, bossEntity);
	}

	/**
	 * Run the specific command with the given arguments.
	 *
	 * @param command
	 * @param chance
	 * @param console
	 * @param player
	 * @param bossEntity
	 * @return
	 */
	public boolean runCommand(String command, double chance, boolean console, @Nullable Player player,
			LivingEntity bossEntity) {

		if (!RandomUtil.chanceD(chance))
			return false;

		// Do not run if for killer but not supplied
		if (player == null && (command.contains("{killer}") || command.contains("{player}"))) {
			final String baseMessage = "Not running Boss " + this.getName() + " command '" + command
					+ "' because it contains {player} or {killer} and no player was found.";
			Common.logTimed(60 * 60, baseMessage
					+ " This can be normal (i.e. the Commands skill does not support this), so we only display this warning once per hour, but if your commands do not work, set Debug in settings.yml to [commands] to see this message each time and adjust as needed.");

			Debugger.debug("commands", baseMessage);
			return false;
		}

		// Handle special commands
		if (command.startsWith("broadcast ")) {
			for (final FoundationPlayer audience : Platform.getOnlinePlayers())
				for (final String line : command.substring("broadcast ".length()).split("\\|")) {
					final SimpleComponent commandComponent = this
							.replaceVariables(SimpleComponent.fromMiniAmpersand(line), bossEntity, player);

					audience.sendMessage(commandComponent);
				}

		} else if (command.startsWith("tell-damagers ")) {
			final String[] split = command.split(" ");

			final Map<Double, Player> recentDamagers = this.getRecentDamagerPlayer(bossEntity, false);
			final String message = Common.joinRange(1, split);

			// Calculate total damage for percentage
			double totalDamage = 0;
			for (final Double damage : recentDamagers.keySet())
				totalDamage += damage;

			for (final Map.Entry<Double, Player> entry : recentDamagers.entrySet()) {
				final Player damager = entry.getValue();
				final double damage = entry.getKey();

				if (damage > 0) {
					final double damagePercent = totalDamage > 0 ? (damage / totalDamage) * 100 : 0;

					Platform.toPlayer(damager).sendMessage(this.replaceVariables(SimpleComponent.fromMiniAmpersand(replaceTopDamagePercents(recentDamagers, totalDamage, message)
							.replace("{damager}", damager.getName())
							.replace("{damage}", MathUtil.formatTwoDigits(damage))
							.replace("{damage_percent}", MathUtil.formatTwoDigits(damagePercent) + "%")), bossEntity, player));
				}
			}

		} else if (command.startsWith("tell-damagers-list ") || command.startsWith("broadcast-damagers-list ")) {
			final String[] split = command.split(" ");

			final Map<Double, Player> recentDamagers = this.getRecentDamagerPlayer(bossEntity, false);
			final String format = Common.joinRange(1, split);
			final List<SimpleComponent> messages = new ArrayList<>();
			int order = 1;

			messages.add(SimpleComponent.empty());

			final String[] formatSplit = format.split("\\|");

			final String title = formatSplit.length > 1 ? formatSplit[0]
					: "&6Top " + this.getAlias() + " &r&6Damagers:";
			final String content = formatSplit.length > 1 ? formatSplit[1] : formatSplit[0];

			// Calculate total damage for percentage
			double totalDamage = 0;
			for (final Double damage : recentDamagers.keySet())
				totalDamage += damage;

			for (final Map.Entry<Double, Player> entry : recentDamagers.entrySet()) {
				final Player damager = entry.getValue();
				final double damage = entry.getKey();
				final double damagePercent = totalDamage > 0 ? (damage / totalDamage) * 100 : 0;

				for (final String line : content.split("\\|"))
					messages.add(this.replaceVariables(SimpleComponent.fromMiniAmpersand(replaceTopDamagePercents(recentDamagers, totalDamage, line)
							.replace("{order}", order++ + "")
							.replace("{damager}", damager.getName())
							.replace("{damage}", MathUtil.formatTwoDigits(damage))
							.replace("{damage_percent}", MathUtil.formatTwoDigits(damagePercent) + "%")), bossEntity, player));
			}

			final ChatPaginator pages = new ChatPaginator()
					.setHeader(this.replaceVariables(SimpleComponent.fromMiniAmpersand(title), bossEntity, player))
					.setPages(messages);

			for (final Player damager : command.startsWith("tell-damagers-list ") ? recentDamagers.values()
					: Remain.getOnlinePlayers())
				pages.send(Platform.toPlayer(damager));

		} else if (command.startsWith("tell ")) {
			final String[] commands = command.substring("tell ".length()).split("\\|");

			Platform.runTask(1, () -> {
				final List<SimpleComponent> components = new ArrayList<>();

				for (final String line : commands)
					components.add(this.replaceVariables(SimpleComponent.fromMiniAmpersand(line), bossEntity, player));

				// Send to nearby players, likely the one who triggered this command
				if (player == null) {
					for (final Entity nearby : Remain.getNearbyEntities(bossEntity.getLocation(), 10))
						if (nearby instanceof Player) {
							final FoundationPlayer audience = Platform.toPlayer(nearby);

							for (final SimpleComponent component : components)
								audience.sendMessage(component);
						}

				} else {
					final FoundationPlayer audience = Platform.toPlayer(player);

					for (final SimpleComponent component : components)
						audience.sendMessage(component);
				}
			});
		}

		else if (command.startsWith("discord ")) {
			command = command.replace("discord ", "");

			Valid.checkBoolean(!command.isEmpty() && command.split(" ").length >= 2, "Invalid 'discord' "
					+ this.getName() + " Boss command syntax, expected: discord <channel> <message>, got: " + command);

			if (!HookManager.isDiscordSRVLoaded()) {
				Common.logTimed(60 * 30, this.getName() + "'s Boss discord command (" + command
						+ ") requires DiscordSRV installed and running! (This message only appears once per 30 minutes)");

				return false;
			}

			final String[] split = command.split(" ");
			HookManager.sendDiscordMessage(split[0],
					this.replaceVariablesLegacy(Common.joinRange(1, split), bossEntity, player));

		} else if (player == null || console)
			Platform.dispatchConsoleCommand(player == null ? null : Platform.toPlayer(player),
					this.replaceVariablesLegacy(command, bossEntity, player));

		else
			Platform.toPlayer(player).dispatchCommand(command);

		return true;
	}

	/**
	 * Replaces {damage_percent_X} placeholders in the given line with the top 10 damagers' damage percentages.
	 *
	 * @param recentDamagers Map of damage to Player, where the key is the amount of damage dealt.
	 * @param totalDamage    The total damage dealt to the boss.
	 * @param lineToReplace  The string containing placeholders to replace.
	 * @return The string with {damage_percent_X} replaced by the corresponding player's damage percent.
	 */
	private String replaceTopDamagePercents(Map<Double, Player> recentDamagers, double totalDamage, String lineToReplace) {
		if (recentDamagers == null || recentDamagers.isEmpty())
			return lineToReplace;

		// Sort entries by damage descending
		List<Double> sortedDamages = recentDamagers.keySet().stream()
				.sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());

		for (int i = 1; i <= 10; i++) {
			if (i - 1 >= sortedDamages.size()) {
				lineToReplace = lineToReplace.replace("{damage_percent_" + i + "}", "0%");
				continue;
			}

			double damage = sortedDamages.get(i - 1);
			double percent = totalDamage > 0 ? (damage / totalDamage) * 100 : 0;
			lineToReplace = lineToReplace.replace("{damage_percent_" + i + "}", MathUtil.formatTwoDigits(percent) + "%");
		}
		return lineToReplace;
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Functions */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Replace Boss variables in the given message for the living Boss entity.
	 *
	 * @param message
	 * @param bossEntity
	 * @param player
	 *
	 * @return
	 */
	public String replaceVariablesLegacy(String message, @NonNull LivingEntity bossEntity, @Nullable Player player) {
		final Location loc = bossEntity.getLocation();

		return Variables.builder().audience(player).placeholderArray(
				"player", player == null ? "" : player.getName(),
				"player_name", player == null ? "" : player.getName(),
				"killer", player == null ? "" : player.getName(),
				"killer_name", player == null ? "" : player.getName(),

				"boss_name", this.getName(),
				"boss_alias", SimpleComponent.fromMiniAmpersand(this.getAlias()),
				"boss_location", SerializeUtil.serializeLocation(loc),
				"boss_world", loc.getWorld().getName(),
				"boss_x", loc.getBlockX(),
				"boss_y", loc.getBlockY(),
				"boss_z", loc.getBlockZ(),

				// Deprecated since PAPI is now supported
				"boss", this.getName(),
				"alias", SimpleComponent.fromMiniAmpersand(this.getAlias()),
				"location", SerializeUtil.serializeLocation(loc),
				"world", loc.getWorld().getName(),
				"x", loc.getBlockX(),
				"y", loc.getBlockY(),
				"z", loc.getBlockZ()).replaceLegacy(message);
	}

	/**
	 * Replace Boss variables in the given message for the living Boss entity.
	 *
	 * @param message
	 * @param bossEntity
	 * @param player
	 *
	 * @return
	 */
	public SimpleComponent replaceVariables(SimpleComponent message, @NonNull LivingEntity bossEntity,
			@Nullable Player player) {
		final Location loc = bossEntity.getLocation();

		return Variables.builder().audience(player).placeholderArray(
				"player", player == null ? "" : player.getName(),
				"player_name", player == null ? "" : player.getName(),
				"killer", player == null ? "" : player.getName(),
				"killer_name", player == null ? "" : player.getName(),

				"boss_name", this.getName(),
				"boss_alias", SimpleComponent.fromMiniAmpersand(this.getAlias()),
				"boss_location", SerializeUtil.serializeLocation(loc),
				"boss_world", loc.getWorld().getName(),
				"boss_x", loc.getBlockX(),
				"boss_y", loc.getBlockY(),
				"boss_z", loc.getBlockZ(),

				// Deprecated since PAPI is now supported
				"boss", this.getName(),
				"alias", SimpleComponent.fromMiniAmpersand(this.getAlias()),
				"location", SerializeUtil.serializeLocation(loc),
				"world", loc.getWorld().getName(),
				"x", loc.getBlockX(),
				"y", loc.getBlockY(),
				"z", loc.getBlockZ()).replaceComponent(message);
	}

	/**
	 * Return if this Boss type can have equipment.
	 *
	 * @return
	 */
	public boolean canHaveEquipment() {
		return Monster.class.isAssignableFrom(this.type.getEntityClass()) || this.type == CompEntityType.PLAYER
				|| this.type == CompEntityType.HORSE || this.type.toString().equals("ARMOR_STAND");
	}

	/**
	 * Compiles a Spawn Egg.
	 *
	 * @return
	 */
	public ItemStack getEgg() {
		return this.getEgg(1);
	}

	/**
	 * Compiles a Spawn Egg.
	 *
	 * @param amount
	 * @return
	 */
	public ItemStack getEgg(int amount) {

		final boolean isPlayer = this.type == CompEntityType.PLAYER;

		// Compile title
		final String title = Common.getOrDefault(this.eggTitle, this.getEggDefaultTitle())
				.replace("{boss}", this.getName())
				.replace("{boss_name}", this.getName())
				.replace("{boss_alias}", this.getAlias());

		// Compile lore
		List<String> lore = this.eggLore == null || this.eggLore.isEmpty() ? this.getEggDefaultLore() : this.eggLore;

		if (lore.size() == 1 && lore.get(0).equalsIgnoreCase("hidden"))
			lore = Arrays.asList("");

		final ItemCreator def = isPlayer ? ItemCreator.fromMaterial(CompMaterial.PLAYER_HEAD)
				: ItemCreator.fromMonsterEgg(this.type);

		// Compile the egg itself differently for players and animals
		return (this.eggMaterial == null ? def : ItemCreator.fromMaterial(this.eggMaterial))
				.name(title)
				.lore(lore)
				.amount(amount)
				.glow(true)
				.tag(NBT_TAG, this.getName())
				.make();
	}

	/**
	 * Get default egg title
	 *
	 * @return
	 */
	public String getEggDefaultTitle() {
		return this.type == CompEntityType.PLAYER ? "Player NPC" : "Spawn " + this.getName();
	}

	/**
	 * Get default egg lore
	 *
	 * @return
	 */
	public List<String> getEggDefaultLore() {
		return Arrays.asList(
				"",
				"&2&l< &7Left click for menu.",
				"&2&l> &7Right click to summon.");
	}

	/**
	 * Get default egg material
	 *
	 * @return
	 */
	public CompMaterial getEggDefaultMaterial() {
		return this.type == CompEntityType.PLAYER ? CompMaterial.PLAYER_HEAD
				: Common.getOrDefault(CompEntityType.getSpawnEgg(this.type), CompMaterial.SHEEP_SPAWN_EGG);
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Disk data */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Return the Boss alias or name if not set.
	 *
	 * @return
	 */
	public String getAlias() {
		return Common.getOrDefault(this.alias, "&4" + this.getName());
	}

	/**
	 * Set the alias
	 *
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias != null && !"default".equals(alias) && !alias.isEmpty() ? alias : null;

		this.save();
	}

	/**
	 * Set the max health
	 *
	 * @param health the health to set
	 */
	public void setMaxHealth(double health) {
		this.maxHealth = health;

		this.save();
	}

	/**
	 * Return the dropped experience, or "Default" if not set.
	 *
	 * @return
	 */
	public String getDroppedExpAsString() {
		return this.droppedExp == null ? "Default"
				: this.droppedExp.toString().equals("0") ? "None" : this.droppedExp.toString();
	}

	/**
	 * Set the dropped exp.
	 *
	 * @param droppedExp the droppedExp to set, null to use Minecraft default, 0 to
	 *                   disable exp drops
	 */
	public void setDroppedExp(@Nullable RangedValue droppedExp) {
		this.droppedExp = droppedExp;

		this.save();
	}

	/**
	 * Return equipment.
	 *
	 * @param slot
	 * @return
	 */
	public ItemStack getEquipmentItem(@NonNull CompEquipmentSlot slot) {
		final Tuple<ItemStack, Double> tuple = this.equipment.get(slot);

		return tuple != null ? tuple.getKey() : null;
	}

	/**
	 * Return equipment.
	 *
	 * @param slot
	 * @return
	 */
	public double getEquipmentDropChance(@NonNull CompEquipmentSlot slot) {
		final Tuple<ItemStack, Double> tuple = this.equipment.get(slot);

		if (tuple != null) {
			final Double dropChance = tuple.getValue();
			Valid.checkNotNull(dropChance, "Drop chance cannot be null for " + slot + ", raw: " + tuple);

			return tuple.getValue();
		}

		return 1.0D;
	}

	/**
	 * Set equipment without saving, used in menu to prevent flushing disk 4X for
	 * each slot.
	 *
	 * @param slot
	 * @param tuple
	 */
	public void setEquipmentNoSave(@NonNull CompEquipmentSlot slot, @Nullable Tuple<ItemStack, Double> tuple) {
		if (tuple == null)
			this.equipment.remove(slot);

		else {
			Valid.checkBoolean(tuple.getKey() != null && !CompMaterial.isAir(tuple.getKey().getType()),
					"Cannot save null or AIR items for " + slot + " -> " + tuple);
			Valid.checkNotNull(tuple.getValue(),
					"Cannot have null drop chance when saving non-null " + slot + " equipment item " + tuple);

			this.equipment.put(slot, tuple);
		}
	}

	/**
	 * Set equipment.
	 *
	 * @param slot
	 * @param item
	 */
	public void setEquipmentItemNoSave(@NonNull CompEquipmentSlot slot, ItemStack item) {
		if (item == null || CompMaterial.isAir(item.getType()))
			this.equipment.remove(slot);

		else
			this.equipment.put(slot, new Tuple<>(item, 1D));
	}

	/**
	 * Return equipment drop chance.
	 *
	 * @param slot
	 * @param dropChance
	 */
	public void setEquipmentDropChanceNoSave(@NonNull CompEquipmentSlot slot, double dropChance) {
		Valid.checkBoolean(this.equipment.containsKey(slot),
				"Cannot set drop chance for non existing equipment type " + slot);
		Valid.checkBoolean(dropChance >= 0.0 && dropChance <= 1.0,
				"Drop chance must be between 0.0 and 1.0, got: " + dropChance);

		final ItemStack item = this.equipment.get(slot).getKey();

		this.equipment.put(slot, new Tuple<>(item, dropChance));
	}

	/**
	 * Set if empty equipment should attain vanilla equipment
	 *
	 * @param emptySlotsRandomlyEquipped
	 */
	public void setEmptyEquipmentRandomlyEquipped(boolean emptySlotsRandomlyEquipped) {
		this.emptyEquipmentRandomlyEquipped = emptySlotsRandomlyEquipped;

		this.save();
	}

	/**
	 * Get potion effect level for type, 0
	 *
	 * @param type
	 * @return
	 */
	public int getPotionEffectLevel(@NonNull PotionEffectType type) {
		return this.potionEffects.getOrDefault(type, 0);
	}

	/**
	 * Set potion effect
	 *
	 * @param type
	 * @param level
	 */
	public void setPotionEffectNoSave(@NonNull PotionEffectType type, int level) {
		if (level > 0)
			this.potionEffects.put(type, level);
		else
			this.potionEffects.remove(type);
	}

	/**
	 * Set riding vanilla entities
	 *
	 * @param entities
	 */
	public void setRidingVanilla(@NonNull List<EntityType> entities) {
		this.ridingEntitiesVanilla = entities;

		this.save();
	}

	/**
	 * Set Bosses to ride
	 *
	 * @param bossNames
	 */
	public void setRidingBoss(@NonNull List<String> bossNames) {
		this.ridingEntitiesBoss = bossNames;

		this.save();
	}

	/**
	 * Return if we remove riding dudes and gods on death
	 *
	 * @return
	 */
	public boolean getRemoveRidingOnDeath() {
		return this.removeRidingOnDeath;
	}

	/**
	 * Set to remove riding entities on death
	 *
	 * @param removeRidingOnDeath
	 */
	public void setRemoveRidingOnDeath(boolean removeRidingOnDeath) {
		this.removeRidingOnDeath = removeRidingOnDeath;

		this.save();
	}

	/**
	 * Get commands
	 *
	 * @param type
	 * @return
	 */
	public List<BossCommand> getCommands(BossCommandType type) {
		Valid.checkBoolean(type != BossCommandType.SKILL,
				"Cannot use Boss#getCommands for SKILL command, as they are stored in each skill individually");

		final List<BossCommand> filtered = new ArrayList<>();

		for (final BossCommand command : this.commands)
			if (command.getType() == type)
				filtered.add(command);

		return filtered;
	}

	/**
	 * Create a new command
	 *
	 * @param type
	 * @param command
	 */
	public void addCommand(BossCommandType type, String command) {
		this.commands.add(BossCommand.create(this, type, command));

		this.save();
	}

	/**
	 * Remove command
	 *
	 * @param command
	 */
	public void removeCommand(BossCommand command) {
		this.commands.remove(command);

		this.save();
	}

	/**
	 * Set flag
	 *
	 * @param commandsStoppedAfterFirst
	 */
	public void setCommandsStoppedAfterFirst(boolean commandsStoppedAfterFirst) {
		this.commandsStoppedAfterFirst = commandsStoppedAfterFirst;

		this.save();
	}

	/**
	 * Return attribute
	 *
	 * @param attribute
	 * @return
	 */
	@Nullable
	public Double getAttribute(BossAttribute attribute) {
		return this.attributes.getOrDefault(attribute, this.getDefaultAttribute(attribute));
	}

	/**
	 * Return default attribute
	 *
	 * @param attribute
	 * @return
	 */
	public double getDefaultAttribute(BossAttribute attribute) {
		return this.defaultAttributes.getOrDefault(attribute, 1.0D);
	}

	/**
	 * Return all default attributes
	 *
	 * @return
	 */
	public Set<BossAttribute> getDefaultAttributes() {
		return this.defaultAttributes.keySet();
	}

	/**
	 * Set attribute
	 *
	 * @param attribute
	 * @param value
	 */
	public void setAttribute(BossAttribute attribute, @Nullable Double value) {
		if (value == null)
			this.attributes.remove(attribute);
		else
			this.attributes.put(attribute, value);

		this.save();
	}

	/**
	 * Has lightning on death?
	 *
	 * @return
	 */
	public boolean hasLightningOnDeath() {
		return this.lightningOnDeath;
	}

	/**
	 * Set flag
	 *
	 * @param lightning
	 */
	public void setLightningOnDeath(boolean lightning) {
		this.lightningOnDeath = lightning;

		this.save();
	}

	/**
	 * Has lightning on spawn?
	 *
	 * @return
	 */
	public boolean hasLightningOnSpawn() {
		return this.lightningOnSpawn;
	}

	/**
	 * Set flag
	 *
	 * @param lightning
	 */
	public void setLightningOnSpawn(boolean lightning) {
		this.lightningOnSpawn = lightning;

		this.save();
	}

	/**
	 * Set reinforcements
	 *
	 * @param reinforcements
	 */
	public void setReinforcements(List<BossReinforcement> reinforcements) {
		this.reinforcements = reinforcements;

		this.save();
	}

	/**
	 * Set custom setting. Put null as value to remove
	 *
	 * @param <T>
	 * @param setting
	 * @param value
	 */
	public <T> void setCustomSetting(CustomSetting<T> setting, @Nullable T value) {
		final String key = setting.getKey();

		if (value == null)
			this.customSettings.remove(key);
		else
			this.customSettings.put(key, value);

		this.save();
	}

	/**
	 * Get custom setting value
	 *
	 * @param <T>
	 * @param setting
	 * @return
	 */
	@Nullable
	public <T> T getCustomSetting(CustomSetting<T> setting) {
		final T value = (T) this.customSettings.get(setting.getKey());

		return value != null ? value : setting.getDefault();
	}

	/**
	 * Get custom settings this Boss can have.
	 *
	 * @return
	 */
	public List<CustomSetting<?>> getApplicableCustomSettings() {
		final List<CustomSetting<?>> applicable = new ArrayList<>();

		for (final CustomSetting<?> setting : CustomSetting.getRegisteredSettings()) {
			setting.setBoss(this);

			if (setting.canApplyTo(this.type))
				applicable.add(setting);
		}

		return Collections.unmodifiableList(applicable);
	}

	/**
	 * Get skills this Boss has not.
	 *
	 * @return
	 */
	public Collection<BossSkill> getUnequippedSkills() {
		final List<BossSkill> available = new ArrayList<>();

		for (final String skillName : BossSkill.getSkillsNames())
			if (!this.hasSkill(skillName)) {
				final BossSkill instance = BossSkill.createInstance(skillName, this, new SerializedMap());

				if (instance != null)
					available.add(instance);
			}

		Collections.sort(available, (first, second) -> first.getName().compareTo(second.getName()));

		return available;
	}

	/**
	 * Get Boss skills.
	 *
	 * @return
	 */
	public Collection<BossSkill> getSkills() {
		return this.skills.values();
	}

	/**
	 * Return if Boss has skill
	 *
	 * @param skill
	 * @return
	 */
	public boolean hasSkill(BossSkill skill) {
		return this.hasSkill(skill.getName());
	}

	/**
	 * Return if Boss has skill by its name
	 *
	 * @param skillName
	 * @return
	 */
	public boolean hasSkill(String skillName) {
		return this.skills != null && this.skills.containsKey(skillName);
	}

	/**
	 * Add skill to Boss
	 *
	 * @param skill
	 */
	public void addSkill(BossSkill skill) {
		this.skills.put(skill.getName(), skill);

		this.save();
	}

	/**
	 * Remove skill from Boss
	 *
	 * @param skill
	 */
	public void removeSkill(BossSkill skill) {
		this.skills.remove(skill.getName());

		this.save();
	}

	/**
	 * Get flag
	 *
	 * @return
	 */
	public boolean hasVanillaDrops() {
		return this.vanillaDrops;
	}

	/**
	 * Set flag
	 *
	 * @param vanillaDrops
	 */
	public void setVanillaDrops(boolean vanillaDrops) {
		this.vanillaDrops = vanillaDrops;

		this.save();
	}

	/**
	 * Get drop at menu slot
	 *
	 * @param slot
	 * @return
	 */
	@Nullable
	public ItemStack getGeneralDropAt(int slot) {
		final List<Tuple<ItemStack, Double>> drops = this.getGeneralDrops();

		if (slot < drops.size()) {
			final Tuple<ItemStack, Double> tuple = drops.get(slot);

			return tuple != null ? tuple.getKey() : null;
		}

		return null;
	}

	/**
	 * Get drop chance at menu slot
	 *
	 * @param slot
	 * @return
	 */
	@Nullable
	public Double getGeneralDropChanceAt(int slot) {
		if (slot < this.generalDrops.size()) {
			final Tuple<ItemStack, Double> tuple = this.generalDrops.get(slot);

			return tuple != null ? tuple.getValue() : null;
		}

		return null;
	}

	/**
	 * Set drops by their menu slots
	 *
	 * @param generalDrops
	 */
	public void setGeneralDrops(Collection<Tuple<ItemStack, Double>> generalDrops) {
		this.generalDrops = new ArrayList<>(generalDrops);

		this.save();
	}

	/**
	 * Get player drop orders
	 *
	 * @return
	 */
	public List<Integer> getPlayerDropsOrders() {
		final List<Integer> orders = new ArrayList<>();

		for (int i = 0; i < this.playerDrops.size(); i++)
			orders.add(i + 1);

		return orders;
	}

	/**
	 * Get player drop at the given damage order
	 *
	 * @param order
	 * @return
	 */
	@Nullable
	public Map<ItemStack, Double> getPlayerDrops(int order) {
		return order < this.playerDrops.size()
				? Collections.unmodifiableMap(new LinkedHashMap<>(this.playerDrops.get(order)))
				: null;
	}

	/**
	 * @return the generalDrops
	 */
	public List<Tuple<ItemStack, Double>> getGeneralDrops() {
		final List<Tuple<ItemStack, Double>> copy = new ArrayList<>();
		copy.addAll(this.generalDrops);

		return Collections.unmodifiableList(copy);
	}

	/**
	 * Set player drops by their damage order
	 *
	 * @param order
	 * @param drops
	 */
	public void setPlayerDrops(int order, Map<ItemStack, Double> drops) {
		if (order < this.playerDrops.size())
			this.playerDrops.set(order, drops);

		else
			this.playerDrops.add(drops);

		this.save();
	}

	/**
	 * Remove drop at damage order
	 *
	 * @param order
	 */
	public void removePlayerDrops(int order) {
		Valid.checkBoolean(order < this.playerDrops.size(),
				"Cannot remove player drops on order " + order + " when size is only " + this.playerDrops.size());

		this.playerDrops.remove(order);

		this.save();
	}

	/**
	 * Set how old damagers we should reward
	 *
	 * @param playerDropsTimeThreshold
	 */
	public void setPlayerDropsTimeThreshold(@NonNull SimpleTime playerDropsTimeThreshold) {
		this.playerDropsTimeThreshold = playerDropsTimeThreshold;

		this.save();
	}

	/**
	 * Get commands at the given damage order
	 *
	 * @param order
	 * @return
	 */
	public List<String> getPlayerDropsCommands(int order) {
		return order < this.playerDropsCommands.size() ? Arrays.asList(this.playerDropsCommands.get(order).split("\\|"))
				: new ArrayList<>();
	}

	/**
	 * Set commands at the given damage order
	 *
	 * @param order
	 * @param command
	 */
	public void setPlayerDropsCommand(int order, String command) {
		if (order < this.playerDropsCommands.size())
			this.playerDropsCommands.set(order, command);

		else
			this.playerDropsCommands.add(command);

		this.save();
	}

	/**
	 * Remove command at damage order
	 *
	 * @param order
	 */
	public void removePlayerDropsCommand(int order) {

		if (order == 0 && this.playerDropsCommands.isEmpty())
			return;

		Valid.checkBoolean(order < this.playerDrops.size(),
				"Cannot remove player drops on order " + order + " when size is only " + this.playerDrops.size());

		this.playerDropsCommands.remove(order);
		this.save();
	}

	/**
	 * Return if spawn limits are applied for the given spawn reason
	 *
	 * @param reason
	 * @return
	 */
	public boolean areLimitsAppliedTo(BossSpawnReason reason) {
		return this.limitReasons.contains(reason);
	}

	/**
	 * Add limit for reason
	 *
	 * @param reason
	 */
	public void addLimitReason(BossSpawnReason reason) {
		this.limitReasons.add(reason);

		this.save();
	}

	/**
	 * Remove limit for reason
	 *
	 * @param reason
	 */
	public void removeLimitReason(BossSpawnReason reason) {
		this.limitReasons.remove(reason);

		this.save();
	}

	/**
	 * Get nearby boss limit
	 *
	 * @return
	 */
	public int getNearbyBossesLimit() {
		return this.nearbyBossesLimit.getKey();
	}

	/**
	 * Get nearby boss radius for limit
	 *
	 * @return
	 */
	public double getNearbyBossesRadiusForLimit() {
		return this.nearbyBossesLimit.getValue();
	}

	/**
	 * Set nearby bosses limit, amount of bosses and radius
	 *
	 * @param nearbyBossesLimit
	 */
	public void setNearbyBossesLimit(Tuple<Integer, Double> nearbyBossesLimit) {
		this.nearbyBossesLimit = nearbyBossesLimit;

		this.save();
	}

	/**
	 * Get max bosses in world, or return -1 for no limit
	 *
	 * @param worldName
	 * @return
	 */
	public int getWorldLimit(String worldName) {
		return this.worldLimit.getOrDefault(worldName, -1);
	}

	/**
	 * Set world limit, set to -1 for unlimited
	 *
	 * @param worldName
	 * @param bossLimit
	 */
	public void setWorldLimit(String worldName, int bossLimit) {
		if (bossLimit == -1)
			this.worldLimit.remove(worldName);

		else
			this.worldLimit.put(worldName, bossLimit);

		this.save();
	}

	/**
	 * Set if we keep Boss in his spawn region
	 *
	 * @param keptInSpawnRegion
	 */
	public void setKeptInSpawnRegion(boolean keptInSpawnRegion) {
		this.keptInSpawnRegion = keptInSpawnRegion;

		this.save();
	}

	/**
	 * Set the location to return Boss to when escaped from spawn region
	 *
	 * @param escapeReturnLocation
	 */
	public void setEscapeReturnLocation(@Nullable BossLocation escapeReturnLocation) {
		this.escapeReturnLocation = escapeReturnLocation == null ? null : escapeReturnLocation.getName();

		this.save();
	}

	/**
	 * Set the egg material
	 *
	 * @param eggMaterial
	 */
	public void setEggMaterial(@Nullable CompMaterial eggMaterial) {
		this.eggMaterial = eggMaterial;

		this.save();
	}

	/**
	 * Set the egg title
	 *
	 * @param eggTitle
	 */
	public void setEggTitle(@Nullable String eggTitle) {
		this.eggTitle = eggTitle;

		this.save();
	}

	/**
	 * Set the egg lore
	 *
	 * @param eggLore
	 */
	public void setEggLore(@Nullable List<String> eggLore) {
		this.eggLore = eggLore;

		this.save();
	}

	/**
	 * Return the last time the boss spawned from the given spawn rule
	 *
	 * @param spawnRule
	 * @return
	 */
	public long getLastDeathFromSpawnRule(SpawnRule spawnRule) {
		return this.lastDeathFromSpawnRule.getOrDefault(spawnRule.getName(), 0L);
	}

	/**
	 * Clear the last time the boss spawned from the given spawn rule
	 *
	 * @param spawnRuleName
	 */
	public void setLastDeathFromSpawnRule(String spawnRuleName) {
		this.lastDeathFromSpawnRule.put(spawnRuleName, System.currentTimeMillis());

		this.save();
	}

	public void setNativeAttackGoalEnabled(boolean enabled) {
		this.nativeAttackGoalEnabled = enabled;

		this.save();

		if(GoalManagerCheck.isAvailable()) {
			for (SpawnedBoss spawned : findBossesAlive())
				if (spawned.getBoss().getName().equals(getName()) && spawned.getEntity() instanceof Mob)
					GoalManager.makeAggressive((Mob) spawned.getEntity(), enabled);
		}
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Cache */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Register pvp damage
	 *
	 * @param bossEntity
	 * @param damager
	 * @param amount
	 */
	public void registerDamage(Entity bossEntity, Player damager, double amount) {
		synchronized (this.damageCache) {
			final long now = System.currentTimeMillis();

			final Map<UUID, List<Tuple<Long, Double>>> bossCache = this.damageCache
					.getOrDefault(bossEntity.getUniqueId(), new HashMap<>());
			final List<Tuple<Long, Double>> playerCache = bossCache.getOrDefault(damager.getUniqueId(),
					new ArrayList<>());

			playerCache.add(new Tuple<>(now, amount));
			bossCache.put(damager.getUniqueId(), playerCache);

			this.damageCache.put(bossEntity.getUniqueId(), bossCache);

			// Register damage to player as a custom stat
			this.increaseStat(damager, "Damage", amount);
		}
	}

	/**
	 * Register a kill
	 *
	 * @param killer
	 */
	public void registerKill(Player killer) {
		this.increaseStat(killer, "Kills", 1);
	}

	/**
	 * Get kills for the player
	 *
	 * @param holder
	 * @return
	 */
	public int getKills(UUID holder) {
		return (int) this.getStat(holder, "Kills");
	}

	/**
	 * Get total damage dealt by the player to all Bosses of this type
	 *
	 * @param holder
	 * @return
	 */
	public double getTotalDamage(UUID holder) {
		return this.getStat(holder, "Damage");
	}

	/*
	 * Increase a stat for the player
	 */
	private void increaseStat(Player player, String key, double value) {
		final double oldValue = getStat(player.getUniqueId(), key);

		CompMetadata.setFileMetadata(player.getUniqueId(), "Boss_" + key + "_" + this.getName(),
				String.valueOf(oldValue + value));
		CompMetadata.setFileMetadata(player.getUniqueId(), "Player_Name", player.getName()); // Ugly way to save player
																								// name to avoid expensive
																								// lookups later
	}

	/*
	 * Get a stat for the player
	 */
	private double getStat(UUID holder, String key) {
		final String oldValueRaw = CompMetadata.getFileMetadata(holder, "Boss_" + key + "_" + this.getName());

		return oldValueRaw != null ? Double.parseDouble(oldValueRaw) : 0;
	}

	/**
	 * Get total damage for all Bosses of this type
	 *
	 * @return
	 */
	public Map<Double, String> getTopTotalDamage() {
		return this.getTopStats("Damage");
	}

	/**
	 * Get total kills for all Bosses of this type
	 *
	 * @return
	 */
	public Map<Double, String> getTopKills() {
		return this.getTopStats("Kills");
	}

	/**
	 * Get top stats for the player, the key is capitalized
	 *
	 * @param key
	 * @return
	 */
	public Map<Double, String> getTopStats(String key) {
		final Map<Double, String> topDamage = new TreeMap<>(Collections.reverseOrder());
		final Map<UUID, Map<String, String>> metadata = CompMetadata.MetadataFile.getInstance().getEntityMetadata();

		for (final Map.Entry<UUID, Map<String, String>> entry : metadata.entrySet()) {
			final Map<String, String> entityData = entry.getValue();

			if (entityData.containsKey("Player_Name")) {
				final String playerName = entityData.get("Player_Name");
				final double damage = Double.parseDouble(
						entityData.getOrDefault("Boss_" + ChatUtil.capitalizeFully(key) + "_" + this.getName(), "0"));

				if (damage > 0)
					topDamage.put(damage, playerName);
			}
		}

		return topDamage;
	}

	/**
	 * Calculate and return damage from most to least with damagers.
	 *
	 * @param bossEntity
	 * @param applyDropTimeLimit
	 * @return
	 */
	public TreeMap<Double, Player> getRecentDamagerPlayer(Entity bossEntity, boolean applyDropTimeLimit) {
		synchronized (this.damageCache) {
			final Map<UUID, List<Tuple<Long, Double>>> bossCache = this.damageCache
					.getOrDefault(bossEntity.getUniqueId(), new LinkedHashMap<>());

			if (bossCache.isEmpty())
				return new TreeMap<>();

			final Map<Player, Double> damagers = new LinkedHashMap<>();

			for (final Entry<UUID, List<Tuple<Long, Double>>> entry : bossCache.entrySet()) {
				final UUID playerId = entry.getKey();
				final Player player = Remain.getPlayerByUUID(playerId);

				if (player == null)
					continue;

				for (final Tuple<Long, Double> tuple : entry.getValue()) {
					final long time = tuple.getKey();
					final double damage = tuple.getValue();

					// Only count damage in time limit
					if (!applyDropTimeLimit || (applyDropTimeLimit && (System.currentTimeMillis() - time)
							/ 1000 < this.playerDropsTimeThreshold.getTimeSeconds()))
						damagers.put(player, damagers.getOrDefault(player, 0D) + damage);
					else
						Debugger.debug("drops",
								"Ignoring damage from " + player.getName() + " as it's too old: "
										+ (System.currentTimeMillis() - time) / 1000 + "s vs limit "
										+ this.playerDropsTimeThreshold.getTimeSeconds());
				}
			}

			// Sort by top damage
			final TreeMap<Double, Player> sorted = new TreeMap<>((first, second) -> Double.compare(second, first));

			for (final Map.Entry<Player, Double> entry : damagers.entrySet())
				sorted.put(entry.getValue(), entry.getKey());

			return sorted;
		}
	}

	/**
	 * Called when boss dies to clear damage cache
	 *
	 * @param bossEntity
	 */
	public void clearRecentDamagers(Entity bossEntity) {
		synchronized (this.damageCache) {
			this.damageCache.remove(bossEntity.getUniqueId());
		}
	}

	/**
	 * Return default health
	 *
	 * @return
	 */
	public double getDefaultHealth() {
		if (this.defaultHealth == null)
			this.defaultHealth = EntityUtil.getDefaultHealth(this.type);

		return this.defaultHealth;
	}

	/**
	 * Get the default base speed
	 *
	 * @return
	 */
	public float getDefaultBaseSpeed() {
		return this.type == CompEntityType.PLAYER ? 1F : 5F;
	}

	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */
	/* Misc */
	/*
	 * -----------------------------------------------------------------------------
	 * --
	 */

	public String getName() {
		return this.getFileName();
	}

	public String getTypeFormatted() {
		return ChatUtil.capitalizeFully(this.type);
	}

	@Override
	public String serialize() {
		return this.getName();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Boss && ((Boss) obj).getName().equalsIgnoreCase(this.getName());
	}

	@Override
	public String toString() {
		return "Boss{name=" + this.getName() + ", type=" + this.type + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get valid entity types we can create Bosses from
	 *
	 * @return
	 */
	public static Set<EntityType> getValidEntities() {
		final Set<EntityType> types = new TreeSet<>(Comparator.comparing(EntityType::name));

		types.addAll(CompEntityType.getAvailableSpawnable());

		types.remove(CompEntityType.ARMOR_STAND);

		if (HookManager.isCitizensLoaded())
			types.add(CompEntityType.PLAYER);

		return types;
	}

	/**
	 * @see ConfigItems#loadOrCreateItem(String)
	 *
	 * @param name
	 * @param type
	 *
	 * @return
	 */
	public static Boss createBoss(@NonNull final String name, @NonNull final EntityType type) {
		return loadedBosses.loadOrCreateItem(name, () -> new Boss(name, type));
	}

	/**
	 * @see ConfigItems#loadItems()
	 */
	public static void loadBosses() {
		loadedBosses.loadItems();
	}

	/**
	 * Remove boss by name
	 *
	 * @param bossName
	 */
	public static void removeBoss(String bossName) {
		final Boss boss = findBoss(bossName);
		Valid.checkNotNull(boss, "Cannot remove non existing Boss " + bossName);

		removeBoss(boss);
	}

	/**
	 * @param boss
	 */
	public static void removeBoss(final Boss boss) {
		loadedBosses.removeItem(boss);
	}

	/**
	 * @param name
	 * @return
	 * @see ConfigItems#isItemLoaded(String)
	 */
	public static boolean isBossLoaded(final String name) {
		return loadedBosses.isItemLoaded(name);
	}

	/**
	 * Find alive bosses on all worlds
	 *
	 * @return
	 */
	public static List<SpawnedBoss> findBossesAlive() {
		final List<SpawnedBoss> found = new ArrayList<>();

		for (final World world : Bukkit.getWorlds())
			found.addAll(findBossesAliveIn(world));

		return found;
	}

	/**
	 * Find alive bosses on in world
	 *
	 * @param world
	 * @return
	 */
	public static List<SpawnedBoss> findBossesAliveIn(World world) {
		final List<SpawnedBoss> found = new ArrayList<>();

		for (final Entity entity : world.getLivingEntities()) {
			final SpawnedBoss boss = findBoss(entity);

			if (boss != null)
				found.add(boss);
		}

		return found;
	}

	/**
	 * Find alive bosses on in world of the given type.
	 *
	 * @param world
	 * @param ofType
	 *
	 * @return
	 */
	public static List<SpawnedBoss> findBossesAliveIn(World world, Boss ofType) {
		final List<SpawnedBoss> found = new ArrayList<>();

		for (final Entity entity : world.getLivingEntities()) {
			final SpawnedBoss boss = findBoss(entity);

			if (boss != null && ofType.getName().equals(boss.getName()))
				found.add(boss);
		}

		return found;
	}

	/**
	 * Find alive bosses in range
	 *
	 * @param center
	 * @param radius
	 * @return
	 */
	public static SpawnedBoss findClosestBoss(Location center, double radius) {
		SpawnedBoss closestBoss = null;
		double closestDistance = -1;

		for (final Entity entity : Remain.getNearbyEntities(center, radius)) {
			final double distance = entity.getLocation().distance(center);

			if (closestDistance == -1 || distance < closestDistance) {
				final SpawnedBoss boss = findBoss(entity);

				if (boss != null) {
					closestDistance = distance;
					closestBoss = boss;
				}
			}
		}

		return closestBoss;
	}

	/**
	 * Find closest damaged boss in range
	 *
	 * @param center
	 * @param radius
	 * @return
	 */
	public static SpawnedBoss findClosestDamagedBoss(Location center, double radius) {
		SpawnedBoss closestBoss = null;
		double closestDistance = -1;

		for (final Entity entity : Remain.getNearbyEntities(center, radius)) {
			final double distance = entity.getLocation().distance(center);

			if (closestDistance == -1 || distance < closestDistance) {
				final SpawnedBoss boss = findBoss(entity);

				if (boss != null) {
					if(boss.getBoss().getRecentDamagerPlayer(entity, false).isEmpty())
						continue;
					closestDistance = distance;
					closestBoss = boss;
				}
			}
		}

		return closestBoss;
	}

	/**
	 * Find alive bosses in range
	 *
	 * @param center
	 * @param radius
	 * @return
	 */
	public static List<SpawnedBoss> findBossesInRange(Location center, double radius) {
		final List<SpawnedBoss> found = new ArrayList<>();

		for (final Entity entity : Remain.getNearbyEntities(center, radius)) {
			final SpawnedBoss boss = findBoss(entity);

			if (boss != null)
				found.add(boss);
		}

		return found;
	}

	/**
	 * @param bossName
	 * @return
	 */
	public static Boss fromString(final String bossName) {
		return findBoss(bossName);
	}

	/**
	 * @param bossName
	 * @return
	 */
	public static Boss findBoss(@NonNull final String bossName) {
		return loadedBosses.findItem(bossName);
	}

	/**
	 * Find Boss from entity
	 *
	 * @param entity
	 * @return
	 */
	public static SpawnedBoss findBoss(final Entity entity) {

		SpawnedBoss spawnedBoss = null;

		if (HookManager.isCitizensLoaded())
			spawnedBoss = CitizensHook.findNPC(entity);

		if (spawnedBoss == null) {
			final String bossName = findBossName(entity);
			final Boss boss = bossName != null ? findBoss(bossName) : null;

			if (boss != null)
				spawnedBoss = new SpawnedBoss(boss, (LivingEntity) entity);
		}

		return spawnedBoss;
	}

	/**
	 * Find Boss from itemstack
	 *
	 * @param itemStack
	 * @return
	 */
	public static Boss findBoss(final ItemStack itemStack) {
		final String bossName = findBossName(itemStack);

		return bossName != null ? findBoss(bossName) : null;
	}

	/**
	 * Find Boss name from itemstack, regardless if it is installed on server or not
	 *
	 * @param itemStack
	 * @return
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static String findBossName(ItemStack itemStack) {
		return CompMetadata.getMetadata(itemStack, NBT_TAG);
	}

	/**
	 * Find Boss name from entity, regardless if it is installed on server or not
	 *
	 * @param entity
	 * @return
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static String findBossName(Entity entity) {
		return CompMetadata.getMetadata(entity, NBT_TAG);
	}

	/**
	 * @return
	 * @see ConfigItems#getItems()
	 */
	public static List<Boss> getBosses() {
		return loadedBosses.getItems();
	}

	/**
	 * @return
	 * @see ConfigItems#getItemNames()
	 */
	public static List<String> getBossesNames() {
		return Common.convertList(getBosses(), Boss::getName);
	}

	/**
	 * Kill alive boss in all worlds, or all bosses if boss is null
	 *
	 * @param boss
	 * @return
	 */
	public static int killAliveBosses(@Nullable Boss boss) {
		final List<SpawnedBoss> nearby = findBossesAlive();

		return killAlive0(nearby, boss);
	}

	/**
	 * Kill alive boss in world, or all bosses if boss is null
	 *
	 * @param world
	 * @param boss
	 * @return
	 */
	public static int killAliveInWorld(World world, @Nullable Boss boss) {
		final List<SpawnedBoss> nearby = findBossesAliveIn(world);

		return killAlive0(nearby, boss);
	}

	/**
	 * Kill alive boss in range, or all bosses if boss is null
	 *
	 * @param center
	 * @param radius
	 * @param boss
	 * @return
	 */
	public static int killAliveInRange(Location center, int radius, @Nullable Boss boss) {
		final List<SpawnedBoss> nearby = findBossesInRange(center, radius);

		return killAlive0(nearby, boss);
	}

	/*
	 * Helper to kill spawned bosses
	 */
	private static int killAlive0(List<SpawnedBoss> bosses, @Nullable Boss bossToMatch) {
		int count = 0;

		for (final SpawnedBoss spawned : bosses) {
			if (bossToMatch != null && !spawned.getBoss().equals(bossToMatch))
				continue;

			Remain.removeEntityWithPassengersAndNPC(spawned.getEntity());
			count++;
		}

		return count;
	}

	/**
	 * Evaluates if the target can be affected by this skill.
	 *
	 * @param target the player
	 * @return if a skill can be applied
	 */
	public static boolean canTarget(final Entity target) {
		if (target == null)
			return false;

		if (HookManager.isNPC(target))
			return false;

		if (target.isDead() || !target.isValid())
			return false;

		if (Remain.isInvulnerable(target))
			return false;

		if (!WorldGuardHook.canTarget(target.getLocation()))
			return false;

		if (target instanceof Player) {
			final Player player = (Player) target;

			if (PlayerUtil.isVanished(player))
				return false;

			if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE)
				return false;
		}

		return true;
	}
}
