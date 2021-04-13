package org.mineacademy.boss.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.api.BossAttributes;
import org.mineacademy.boss.api.BossDrop;
import org.mineacademy.boss.api.BossEggItem;
import org.mineacademy.boss.api.BossEquipment;
import org.mineacademy.boss.api.BossPotion;
import org.mineacademy.boss.api.BossSettings;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.boss.api.BossSkillRegistry;
import org.mineacademy.boss.api.BossSpawning;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.boss.util.AutoUpdateList;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.RangedRandomValue;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;

/**
 * Class covering the data section + capable of setting inventory content
 */
@Getter
public final class SimpleSettings extends YamlConfig implements BossSettings {

	private final SimpleBoss boss;

	private EntityType type;
	private String customName;
	private String noSpawnPermissionMessage;
	private Integer health;
	private boolean deathLightning;

	private EntityType ridingVanilla;
	private String ridingBoss;
	private boolean ridingRemoveOnDeath;
	private boolean inventoryDrops;
	private boolean singleDrops;
	private int inventoryDropsPlayerLimit;
	private int inventoryDropsTimeLimit;
	private double damageMultiplier;

	private int convertingChance;

	private BossEggItem eggItem;
	private RangedRandomValue droppedExp;

	private StrictMap<BossSpecificSetting, Object> specificSettings = new StrictMap<>();

	private AutoUpdateList<BossPotion> potions;
	private AutoUpdateList<BossSkill> skills;
	private AutoUpdateMap<String, Double> deathCommands;
	private AutoUpdateMap<String, Double> deathByPlayerCommands;
	private AutoUpdateMap<String, Double> spawnCommands;

	private final AutoUpdateMap<Integer, BossDrop> drops;
	private boolean naturalDrops;

	private AutoUpdateMap<String, Integer> reinforcementsBoss;
	private AutoUpdateMap<String, Integer> reinforcementsVanilla;

	private AutoUpdateMap<Sound, SimpleSound> remappedSounds;
	private boolean debuggingSounds;

	private BossAttributes attributes;
	private BossSpawning spawning;
	private BossEquipment equipment;

	// Private flag to prevent stack overflow
	private boolean loadingDefaults = false;

	protected SimpleSettings(EntityType type, String name, SimpleBoss boss) {
		this.type = type;
		this.boss = boss;
		this.drops = new AutoUpdateMap<>(this::updateDrops_);
	}

	public void loadConfig() {
		loadConfiguration("prototype/boss.yml", "bosses/" + boss.getName() + ".yml");
	}

	@Override
	public void onLoadFinish() {
		if (type == null) {
			Valid.checkBoolean(isSet("Type"), "Boss " + boss.getName() + " lacks 'Type' (EntityType) key in its .yml file! Please create that key.");

			type = ReflectionUtil.lookupEnum(EntityType.class, getString("Type"));
		}

		{ // Load defaults
			if (loadingDefaults)
				return;

			loadingDefaults = true;

			setDefaults();
			loadingDefaults = false;
		}

		customName = getString("Custom_Name");
		noSpawnPermissionMessage = getString("No_Spawn_Permission_Message");
		health = getInteger("Health");
		droppedExp = loadRanged("Dropped_Exp");
		potions = loadPotions("Potions");
		specificSettings = loadSpecificSettings();
		attributes = SimpleAttributes.deserialize(getMap("Attributes"), boss);
		skills = loadSkills();
		spawning = SimpleSpawning.deserialize(getMap("Spawning"), this);
		deathCommands = loadCommands("Death.Commands", () -> updateCommands("Death.Commands", deathCommands));
		deathByPlayerCommands = loadCommands("Death.Commands_For_Players", () -> updateCommands("Death.Commands_For_Players", deathByPlayerCommands));
		spawnCommands = loadCommands("Spawn.Commands", () -> updateCommands("Spawn.Commands", spawnCommands));
		reinforcementsBoss = loadReinforcements("Death.Reinforcements.Boss");
		reinforcementsVanilla = loadReinforcements("Death.Reinforcements.Vanilla");
		deathLightning = getBoolean("Death.Lightning");
		equipment = SimpleEquipment.deserialize(isSet("Equipment") ? getMap("Equipment") : new SerializedMap(), this);
		remappedSounds = loadSounds("Sounds.Remapped");
		debuggingSounds = getBoolean("Sounds.Debug");
		ridingVanilla = !getString("Riding").isEmpty() ? get("Riding", EntityType.class) : null;
		ridingRemoveOnDeath = getBoolean("Death.Kill_Riding_Entity");
		eggItem = SimpleEggItem.deserialize(getMap("Spawner_Egg"), this);
		ridingBoss = getString("Riding_Boss");
		naturalDrops = getBoolean("Natural_Drops");
		singleDrops = getBoolean("Single_Drops");
		damageMultiplier = getDouble("Damage_Multiplier");

		inventoryDrops = getBoolean("Death.Inventory_Drops.Enabled");
		inventoryDropsPlayerLimit = getInteger("Death.Inventory_Drops.Player_Limit");
		inventoryDropsTimeLimit = getInteger("Death.Inventory_Drops.Time_Limit_Seconds");

		convertingChance = getInteger("Converting_Chance");

		loadDrops();
		migrateHealth();
	}

	private AutoUpdateMap<String, Double> loadCommands(String path, Runnable updater) {
		final AutoUpdateMap<String, Double> commands = new AutoUpdateMap<>(updater);
		final StrictMap<String, Double> silentMap = commands.getSource();

		final List<?> commandList = getList(path);

		if (commandList != null)
			commandList.forEach(key -> {
				if (key instanceof Map) {
					final Map<?, ?> map = (Map<?, ?>) key;
					final Object command = map.get("Command");
					final Object chance = map.get("Chance");

					if (command != null && chance == null)
						silentMap.put(command.toString(), 1.0);
					else if (command != null && chance != null)
						try {
							silentMap.put(command.toString(), Double.parseDouble(chance.toString()));
						} catch (final NumberFormatException ex) {
							silentMap.put(command.toString(), 0.0);
						}
				} else
					// assume it's an old format command (List of strings)
					silentMap.put(key.toString(), 1.0);
			});
		return commands;
	}

	private void loadDrops() {
		drops.getSource().clear();

		if (isSet("Drops")) {
			final SerializedMap map = getMap("Drops");

			for (final Entry<?, Object> e : map.asMap().entrySet()) {
				final Integer slot = Integer.parseInt(e.getKey().toString());
				final BossDrop drop = BossDrop.deserialize(SerializedMap.of(e.getValue()), this);

				drops.getSource().put(slot, drop);
			}
		}
	}

	private void migrateHealth() {
		if (isSet("Attributes.Max Health")) {
			final int healthAttr = getDouble("Attributes.Max Health").intValue();

			if (healthAttr != health && healthAttr > health)
				setNoSave("Health", healthAttr);

			setNoSave("Attributes.Max Health", null);
		}
	}

	private void setDefaults() {
		Valid.checkNotNull(type, "Type not yet set!");

		if (!isSet("Type"))
			setNoSave("Type", type.toString());

		if (!isSet("Custom_Name"))
			setCustomName(boss.getName());

		if (!isSet("No_Spawn_Permission_Message"))
			setNoSpawnPermissionMessage("");

		if (!isSet("Health"))
			setHealth(getHealth());

		if (!isSet("Dropped_Exp"))
			setDroppedExp(null);

		if (!isSet("Potions"))
			setNoSave("Potions", "[]");

		for (final BossSpecificSetting key : BossSpecificSetting.values())
			if (key.getFor(null).matches(type) && !isSet("Specific_Settings." + key.getConfigKey()))
				setSpecificSetting(key, key.getFor(boss).getDefault());

		if (!isSet("Attributes")) {
			attributes = SimpleAttributes.deserialize(null, boss);

			for (final BossAttribute attr : attributes.getVanilla())
				attributes.set(attr, attributes.getDefaultBase(attr));
		}

		if (!isSet("Skills"))
			setNoSave("Skills", "[]");

		if (!isSet("Death.Commands"))
			setNoSave("Death.Commands", "[]");

		if (!isSet("Death.Commands_For_Players"))
			setNoSave("Death.Commands_For_Players", "[]");

		if (!isSet("Death.Reinforcements.Vanilla"))
			setNoSave("Death.Reinforcements.Vanilla", "[]");

		if (!isSet("Death.Reinforcements.Boss"))
			setNoSave("Death.Reinforcements.Boss", "[]");

		if (!isSet("Death.Lightning"))
			setNoSave("Death.Lightning", true);

		if (!isSet("Spawn.Commands"))
			setNoSave("Spawn.Commands", "[]");

		if (!isSet("Spawning"))
			setNoSave("Spawning", SimpleSpawning.deserialize(new SerializedMap(), this));

		if (!isSet("Sounds.Debug"))
			setNoSave("Sounds.Debug", false);

		if (!isSet("Sounds.Remapped"))
			setNoSave("Sounds.Remapped", new HashMap<>());

		if (!isSet("Equipment"))
			setNoSave("Equipment", new HashMap<>());

		if (!isSet("Riding"))
			setNoSave("Riding", "");

		if (!isSet("Riding_Boss"))
			setNoSave("Riding_Boss", "");

		if (!isSet("Death.Kill_Riding_Entity"))
			setNoSave("Death.Kill_Riding_Entity", true);

		if (!isSet("Spawner_Egg"))
			setNoSave("Spawner_Egg", SimpleEggItem.getDefault());

		if (!isSet("Natural_Drops"))
			setNoSave("Natural_Drops", false);

		if (!isSet("Single_Drops"))
			setNoSave("Single_Drops", false);

		if (!isSet("Death.Inventory_Drops.Enabled"))
			setNoSave("Death.Inventory_Drops.Enabled", false);

		if (!isSet("Death.Inventory_Drops.Player_Limit"))
			setNoSave("Death.Inventory_Drops.Player_Limit", 5);

		if (!isSet("Death.Inventory_Drops.Time_Limit_Seconds"))
			setNoSave("Death.Inventory_Drops.Time_Limit_Seconds", 10);

		if (!isSet("Damage_Multiplier"))
			setNoSave("Damage_Multiplier", 1.0D);

		if (!isSet("Converting_Chance"))
			setNoSave("Converting_Chance", 100);
	}

	private RangedRandomValue loadRanged(String path) {
		final String raw = getString(path);

		return raw != null && !"default".equals(raw) && !"def".equals(raw) ? RangedRandomValue.parse(raw) : null;
	}

	private AutoUpdateList<BossPotion> loadPotions(String string) {
		final AutoUpdateList<BossPotion> potions = new AutoUpdateList<>(this::updatePotions);

		for (final String raw : getStringList("Potions"))
			potions.getSource().add(BossPotion.parse(raw));

		return potions;
	}

	private StrictMap<BossSpecificSetting, Object> loadSpecificSettings() {
		final StrictMap<BossSpecificSetting, Object> specificSettings = new StrictMap<>();

		for (final BossSpecificSetting setting : BossSpecificSetting.values())
			if (setting.getFor(boss).matches(type)) {
				final Object value = getObject("Specific_Settings." + setting.getConfigKey());

				if (value != null)
					specificSettings.put(setting, value);
			}

		return specificSettings;
	}

	private AutoUpdateMap<String, Integer> loadReinforcements(String path) {
		final AutoUpdateMap<String, Integer> map = new AutoUpdateMap<>(null);
		map.setUpdater(() -> updateReinforcements_(path, map));

		final List<String> unparsed = getStringList(path);

		for (final String raw : unparsed) {
			final String[] line = raw.split(", ");

			final String name = line[0];
			final int count = line.length == 2 ? Integer.parseInt(line[1]) : 1;

			map.getSource().put(name, count);
		}

		return map;
	}

	private AutoUpdateList<BossSkill> loadSkills() {
		final AutoUpdateList<BossSkill> skills = new AutoUpdateList<>(this::updateSkills);
		final List<String> unparsed = getStringList("Skills");

		if (unparsed != null)
			for (final String name : unparsed) {
				final BossSkill skill = BossSkillRegistry.getByName(name);

				if (skill != null)
					skills.getSource().add(skill);

				else
					Common.log("Ignoring unknown skill '" + name + "' for Boss " + boss.getName());
			}

		return skills;
	}

	private AutoUpdateMap<Sound, SimpleSound> loadSounds(String path) {
		final AutoUpdateMap<Sound, SimpleSound> map = new AutoUpdateMap<>(null);
		map.setUpdater(this::updateSounds_);

		final SerializedMap unparsed = getMap(path);

		for (final Entry<String, Object> raw : unparsed.asMap().entrySet()) {
			final String fromRaw = raw.getKey();

			final Sound from = ReflectionUtil.lookupEnum(Sound.class, fromRaw);
			final SimpleSound to = new SimpleSound(raw.getValue().toString());

			map.getSource().put(from, to);
		}

		return map;
	}

	/**
	 * @param convertingChance the convertingChance to set
	 */
	@Override
	public void setConvertingChance(int convertingChance) {
		Valid.checkBoolean(convertingChance >= 0 && convertingChance <= 100, "Converting chance for " + this + " must be between 0-100, got: " + convertingChance);

		this.convertingChance = convertingChance;
		save("Converting_Chance", this.convertingChance);
	}

	@Override
	public void setCustomName(String customName) {
		this.customName = customName != null ? customName : "none";

		save("Custom_Name", this.customName);
	}

	@Override
	public void setNoSpawnPermissionMessage(String noSpawnPermissionMessage) {
		this.noSpawnPermissionMessage = noSpawnPermissionMessage == null ? "" : noSpawnPermissionMessage;

		save("No_Spawn_Permission_Message", this.noSpawnPermissionMessage);
	}

	@Override
	public void setHealth(int health) {
		this.health = health;

		save("Health", health);
	}

	@Override
	public int getHealth() {
		return Common.getOrDefault(health, getDefaultHealth());
	}

	private int getDefaultHealth() {
		final LivingEntity entity = boss.spawnDummy(false);
		final int health = Remain.getHealth(entity);

		entity.remove();

		return health;
	}

	@Override
	public void setDroppedExp(RangedRandomValue value) {
		this.droppedExp = value;

		save("Dropped_Exp", value != null ? value.isStatic() ? value.toLine() : value.getMin() + " - " + value.getMax() : "default");
	}

	@Override
	public AutoUpdateList<BossPotion> getPotions() {
		return potions;
	}

	@Override
	public int getLevelOf(PotionEffectType type) {
		for (final BossPotion potion : potions)
			if (type.equals(potion.getType()))
				return potion.getLevel();

		return 0;
	}

	@Override
	public void setPotion(PotionEffectType type, int level) {
		final BossPotion potion = new BossPotion(type, level);

		boolean found = false;

		// Case 1: Already exists
		for (int i = 0; i < potions.size(); i++) {
			final BossPotion p = potions.get(i);

			if (p != null && p.getType() == type) {
				if (level > 0)
					potions.setAndUpdate(i, potion);
				else
					potions.removeAndUpdate(i);

				found = true;
				break;
			}
		}

		// Case 2: Does not exist
		if (!found && level > 0)
			potions.addAndUpdate(potion);
	}

	@Override
	public Set<BossSpecificSetting> getSpecificSettings() {
		return Collections.unmodifiableSet(specificSettings.keySet());
	}

	@Override
	public Object getSpecificSetting(BossSpecificSetting setting) {
		return Common.getOrDefault(specificSettings.get(setting), setting.getFor(boss).getDefault());
	}

	@Override
	public void setSpecificSetting(BossSpecificSetting key, Object value) {
		this.specificSettings.override(key, value);

		save("Specific_Settings." + key.getConfigKey(), value);
	}

	public void setDeathLightning(boolean deathLightning) {
		this.deathLightning = deathLightning;

		save("Death.Lightning", deathLightning);
	}

	@Override
	public void setDebuggingSounds(boolean flag) {
		this.debuggingSounds = flag;

		save("Sounds.Debug", flag);
	}

	@Override
	public void setNaturalDrops(boolean naturalDrops) {
		this.naturalDrops = naturalDrops;

		save("Natural_Drops", naturalDrops);
	}

	@Override
	public void setSingleDrops(boolean singleDrops) {
		this.singleDrops = singleDrops;

		save("Single_Drops", singleDrops);
	}

	@Override
	public boolean hasNaturalDrops() {
		return naturalDrops;
	}

	@Override
	public boolean hasSingleDrops() {
		return singleDrops;
	}

	//

	private void updatePotions() {
		final StrictList<String> list = Common.convertStrict(potions, p -> {
			Valid.checkNotNull(p, "Potion = null");
			Valid.checkNotNull(p.getType(), "Potion type null at " + p);

			return p.getType().getName() + " " + p.getLevel();
		});

		save("Potions", list);
	}

	public void updateAttributes(BossAttributes attributes) {
		this.attributes = attributes;

		save("Attributes", attributes);
	}

	private void updateSkills() {
		final StrictList<String> list = Common.convertStrict(skills, s -> {
			Valid.checkNotNull(s, "Skill = null");

			return s.getName();
		});

		save("Skills", list);
	}

	private void updateReinforcements_(String path, AutoUpdateMap<String, Integer> map) {
		final StrictList<String> converted = Common.convertToList(map.getSource().getSource(), (boss, quantity) -> boss + ", " + quantity);

		save(path, converted);
	}

	public void updateSpawning(SimpleSpawning spawning) {
		this.spawning = spawning;

		save("Spawning", spawning);
	}

	private void updateCommands(String path, AutoUpdateMap<String, Double> cmds) {

		// Bukkit's yaml implementation doesn't escape key strings
		final LinkedList<Map<String, Object>> temp = new LinkedList<>();

		for (final Map.Entry<String, Double> e : cmds.entrySet()) {
			final HashMap<String, Object> v = new HashMap<>();

			v.put("Command", e.getKey());
			v.put("Chance", e.getValue());

			temp.add(v);
		}

		save(path, temp);
	}

	public void saveEquipment_() {
		save("Equipment", equipment);
	}

	public void updateDrops_() {
		save("Drops", drops);
	}

	public void updateEggItem(SimpleEggItem eggItem) {
		this.eggItem = eggItem;

		save("Spawner_Egg", eggItem);
	}

	@Override
	public void setRidingVanilla(EntityType type) {
		this.ridingVanilla = type;

		save("Riding", type);
	}

	@Override
	public void setRidingBoss(String boss) {
		this.ridingBoss = boss;

		save("Riding_Boss", boss == null ? "" : boss);
	}

	@Override
	public boolean isRemovingRidingOnDeath() {
		return ridingRemoveOnDeath;
	}

	@Override
	public void setRemoveRidingOnDeath(boolean setting) {
		this.ridingRemoveOnDeath = setting;

		save("Death.Kill_Riding_Entity", setting);
	}

	@Override
	public boolean hasInventoryDrops() {
		return inventoryDrops;
	}

	@Override
	public void setInventoryDrops(boolean setting) {
		this.inventoryDrops = setting;

		save("Death.Inventory_Drops.Enabled", setting);
	}

	@Override
	public int getInventoryDropsPlayerLimit() {
		return inventoryDropsPlayerLimit;
	}

	@Override
	public void setInventoryDropsPlayerLimit(int setting) {
		this.inventoryDropsPlayerLimit = setting;

		save("Death.Inventory_Drops.Player_Limit", setting);
	}

	@Override
	public int getInventoryDropsTimeLimit() {
		return inventoryDropsTimeLimit;
	}

	@Override
	public void setInventoryDropsTimeLimit(int setting) {
		this.inventoryDropsTimeLimit = setting;

		save("Death.Inventory_Drops.Time_Limit_Seconds", setting);
	}

	public void updateSounds_() {
		save("Sounds.Remapped", Common.convertStrict(remappedSounds.getSource().getSource(), new Common.MapToMapConverter<Sound, SimpleSound, String, String>() {

			@Override
			public String convertKey(Sound key) {
				return key.toString();
			}

			@Override
			public String convertValue(SimpleSound value) {
				return value.toString();
			}
		}));
	}

	@Override
	public void setDamageMultiplier(double damageMultiplier) {
		this.damageMultiplier = damageMultiplier;

		save("Damage_Multiplier", damageMultiplier);
	}
}