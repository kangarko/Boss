package org.mineacademy.boss.custom;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.custom.EnderdragonPhaseSetting.CompPhase;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.MissingEnumException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.CompVillagerProfession;
import org.mineacademy.fo.remain.CompVillagerType;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTEntity;

import lombok.Getter;

/**
 * Custom settings will automatically be added to the Boss settings menu
 * and allow changing any additional values for your boss.
 *
 * @param <T>
 */
public abstract class CustomSetting<T> {

	/**
	 * The main setting register.
	 */
	private static final Map<String, CustomSetting<?>> byName = new HashMap<>();

	/**
	 * The custom settings that come preinstalled with the plugin.
	 */
	public static final CustomSetting<Boolean> BABY = new BabySetting();
	public static final CustomSetting<Boolean> CREEPER_POWERED = new PoweredCreeperSetting();
	public static final CustomSetting<Boolean> DESPAWN = new DespawnSetting();
	public static final CustomSetting<Boolean> ENDERDRAGON_GRIEF = new EnderdragonGriefSetting();
	public static final CustomSetting<Boolean> ENDERMAN_TELEPORT = new EndermanTeleportSetting();
	public static final CustomSetting<Boolean> GRAVITY = new GravitySetting();
	public static final CustomSetting<Boolean> INVULNERABLE = new InvulnerableSetting();
	public static final CustomSetting<Boolean> IRON_GOLEM_AGGRESSIVE = new IronGolemAggressiveSetting();
	public static final CustomSetting<Boolean> NO_AI = new NoAISetting();
	public static final CustomSetting<Boolean> PICKUP_ITEMS = new PickupItemsSetting();
	public static final CustomSetting<Boolean> PROJECTILE_IMMUNE = new ProjectileImmuneSetting();
	public static final CustomSetting<Boolean> SILENT = new SilentSetting();
	public static final CustomSetting<Boolean> SLIME_BABIES_ON_DEATH = new SlimeBabiesOnDeathSetting();
	public static final CustomSetting<Boolean> SNOWMAN_PUMPKIN = new SnowmanPumpkinSetting();
	public static final CustomSetting<Boolean> SUN_DAMAGE = new SunDamageSetting();
	public static final CustomSetting<Boolean> TARGETABLE = new TargetableSetting();
	public static final CustomSetting<Boolean> ZOMBIE_VILLAGER = new ZombieVillagerSetting();
	public static final CustomSetting<Integer> PHANTOM_SIZE = new PhantomSizeSetting();
	public static final CustomSetting<Integer> SLIME_SIZE = new SlimeSizeSetting();
	public static final CustomSetting<String> ENDER_DRAGON_PHASE = new EnderdragonPhaseSetting();
	public static final CustomSetting<String> RABBIT_TYPE = new RabbitTypeSetting();
	public static final CustomSetting<String> SKELETON_TYPE = new SkeletonTypeSetting();
	public static final CustomSetting<String> VILLAGER_PROFESSION = new VillagerProfessionSetting();
	public static final CustomSetting<String> VILLAGER_TYPE = new VillagerTypeSetting();
	public static final CustomSetting<Boolean> GLOWING = new GlowingSetting();
	public static final CustomSetting<Boolean> CUSTOM_NAME_VISIBLE = new CustomNameVisible();

	/* ------------------------------------------------------------------------------- */
	/* Main class methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * The string identifier, the name of this setting.
	 */
	@Getter
	private final String key;

	/*
	 * The temporary Boss instance we set when evaluating this setting.
	 */
	private Boss boss;

	/**
	 * Create a new custom setting by the given name.
	 *
	 * @param key
	 */
	protected CustomSetting(String key) {
		this.key = key;

		byName.put(key, this);
	}

	/* ------------------------------------------------------------------------------- */
	/* Overridable settings */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Can this setting be applied to the given entity type?
	 *
	 * @param type
	 * @return
	 */
	public boolean canApplyTo(EntityType type) {
		return true;
	}

	/**
	 * Apply this setting to the Boss and its entity on spawn
	 *
	 * @param boss
	 * @param entity
	 */
	public abstract void onSpawn(Boss boss, LivingEntity entity);

	/**
	 * Handle menu clicking for this setting.
	 *
	 * @param boss
	 * @param menu
	 * @param player
	 * @param clickType
	 */
	public abstract void onMenuClick(Boss boss, Menu menu, Player player, ClickType clickType);

	/**
	 * Get the default value for this setting.
	 *
	 * @return
	 */
	public abstract T getDefault();

	/**
	 * Get the icon for this setting.
	 *
	 * @return
	 */
	public abstract ItemCreator getIcon();

	/**
	 * Compiles the icon to a menu item, you can override this and call this method
	 * through super.toMenuItem() and append things to the icon before displaying.
	 *
	 * @return
	 */
	public ItemStack toMenuItem() {
		return this.getIcon().makeMenuTool();
	}

	/* ------------------------------------------------------------------------------- */
	/* Getters and final utilities */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return the value for this setting.
	 *
	 * @return
	 */
	public final T getValue() {
		return Common.getOrDefault(this.boss.getCustomSetting(this), this.getDefault());
	}

	/**
	 * Save a new value for this setting.
	 *
	 * @param boss
	 * @param value
	 */
	public final void save(Boss boss, T value) {
		boss.setCustomSetting(this, value);
	}

	/**
	 * Set the temporary instance of a Boss when evaluating {@link #getValue()} of this class.
	 *
	 * @param boss
	 */
	public final void setBoss(Boss boss) {
		this.boss = boss;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all registered custom settings.
	 *
	 * @return
	 */
	public static Collection<CustomSetting<?>> getRegisteredSettings() {
		return byName.values();
	}

	/**
	 * Return names of custom settings.
	 *
	 * @return
	 */
	public static Set<String> getRegisteredSettingsNames() {
		return byName.keySet();
	}

	/**
	 * Return a custom setting by name, if any.
	 *
	 * @param name
	 * @return
	 */
	public static CustomSetting<?> getByName(String name) {
		for (final Map.Entry<String, CustomSetting<?>> entry : byName.entrySet())
			if (entry.getKey().equalsIgnoreCase(name))
				return entry.getValue();

		return null;
	}
}

/* ------------------------------------------------------------------------------- */
/* Native settings */
/* ------------------------------------------------------------------------------- */

class GlowingSetting extends CustomBooleanSetting {

	public GlowingSetting() {
		super("Glowing");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return CompProperty.GLOWING.isAvailable(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		CompProperty.GLOWING.apply(entity, this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.BEACON,
				"Glowing",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Glowing entities have a",
				"white box around them.");
	}

	@Override
	public Boolean getDefault() {
		return true;
	}
}

class CustomNameVisible extends CustomBooleanSetting {

	public CustomNameVisible() {
		super("Custom_Name_Visible");
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		Remain.setCustomName(entity, null, this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.NAME_TAG,
				"Custom Name Visible",
				"",
				"Status: " + (this.getValue() ? "&aalways visible" : "&7visible on pointing"),
				"",
				"Set if the custom name is",
				"always visible or only when",
				"pointing at the Boss.",
				"",
				"NOTE: Older Minecraft versions",
				"such as 1.8.8 may only support",
				"showing the name while pointing",
				"at the entity.");
	}

}

class InvulnerableSetting extends CustomBooleanSetting {

	public InvulnerableSetting() {
		super("Invulnerable");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return CompProperty.INVULNERABLE.isAvailable(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		CompProperty.INVULNERABLE.apply(entity, this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.BLAZE_POWDER,
				"Invulnerable",
				"",
				"Status: " + (this.getValue() ? "&cinvulnerable" : "&7vulnerable"),
				"",
				"Invulnerable Bosses",
				"can only be damaged by",
				"players in creative mode.");
	}
}

class NoAISetting extends CustomBooleanSetting {

	public NoAISetting() {
		super("NoAI");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return CompProperty.AI.isAvailable(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (CompProperty.AI.isAvailable(Entity.class))
			CompProperty.AI.apply(entity, !this.getValue());

		else
			new NBTEntity(entity).setInteger("NoAI", this.getValue() ? 1 : 0);
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.REDSTONE,
				"No AI",
				"",
				"Status: " + (this.getValue() ? "&cwithout AI" : "&7has AI"),
				"",
				"Bosses lacking AI",
				"do not move and do",
				"not attack players.");
	}
}

class GravitySetting extends CustomBooleanSetting {

	public GravitySetting() {
		super("Gravity");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return CompProperty.GRAVITY.isAvailable(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		CompProperty.GRAVITY.apply(entity, this.getValue());
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.FEATHER,
				"Gravity",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Bosses lacking gravity do",
				"not fall to the ground",
				"nor follow players.");
	}
}

class SilentSetting extends CustomBooleanSetting {

	public SilentSetting() {
		super("Silent");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return CompProperty.SILENT.isAvailable(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		CompProperty.SILENT.apply(entity, this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.NOTE_BLOCK,
				"Silent",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Silent Bosses do not make",
				"any sounds when hit or die.");
	}
}

class PickupItemsSetting extends CustomBooleanSetting {

	public PickupItemsSetting() {
		super("Pickup_Items");
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		entity.setCanPickupItems(this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.CHEST,
				"Can Pickup Items",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Set if the Boss can",
				"pickup ground items.");
	}
}

class ProjectileImmuneSetting extends CustomBooleanSetting {

	public ProjectileImmuneSetting() {
		super("Projectile_Immune");
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ARROW,
				"Projectile Immunity",
				"",
				"Status: " + (this.getValue() ? "&aimmune" : "&7vulnerable"),
				"",
				"When enabled, the Boss is",
				"immune to projectile damage",
				"such as arrows or tridents.",
				"",
				"&7Forces players to engage",
				"&7the Boss in melee combat.");
	}
}

class TargetableSetting extends CustomBooleanSetting {

	public TargetableSetting() {
		super("Targetable");
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.IRON_SWORD,
				"Targettable",
				"",
				"Status: " + (this.getValue() ? "&cattacked" : "&7ignored"),
				"",
				"Set if Boss can be attacked",
				"by monsters or if monsters",
				"will ignore this Boss.");
	}
}

class DespawnSetting extends CustomBooleanSetting {

	public DespawnSetting() {
		super("Despawn");
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		entity.setRemoveWhenFarAway(this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				this.getValue() ? CompMaterial.LAVA_BUCKET : CompMaterial.BUCKET,
				"Despawn when far away?",
				"",
				"Status: " + (this.getValue() ? "&cdespawns" : "&7persistent"),
				"",
				"Set if Boss will be removed",
				"when far away from players.");
	}
}

class SunDamageSetting extends CustomBooleanSetting {

	public SunDamageSetting() {
		super("Sun_Damage");
	}

	@Override
	public boolean canApplyTo(EntityType type) {
		return type == CompEntityType.ZOMBIE || type == CompEntityType.SKELETON || type == CompEntityType.ZOMBIE_VILLAGER || type == CompEntityType.PHANTOM;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.FIRE_CHARGE,
				"Burns Under Sunlight",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Set if the Boss burns",
				"under the daylight.",
				"Default: enabled");
	}
}

class BabySetting extends CustomBooleanSetting {

	public BabySetting() {
		super("Baby");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		final Class<?> entityClass = entity.getEntityClass();

		return Zombie.class.isAssignableFrom(entityClass) || Ageable.class.isAssignableFrom(entityClass);
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (entity instanceof Zombie)
			((Zombie) entity).setBaby(this.getValue());

		else if (entity instanceof Ageable) {
			final Ageable ageable = (Ageable) entity;

			if (this.getValue())
				ageable.setBaby();
			else
				ageable.setAdult();

			ageable.setAgeLock(true);
		}
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ZOMBIE_HEAD,
				"Baby",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Baby Bosses are smaller",
				"and run faster.");
	}
}

class PoweredCreeperSetting extends CustomBooleanSetting {

	public PoweredCreeperSetting() {
		super("Powered_Creeper");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return Creeper.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		((Creeper) entity).setPowered(this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.NETHER_STAR,
				"Powered",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Powered Creepers have",
				"a special effect around,",
				"and explode stronger.");
	}
}

class SlimeBabiesOnDeathSetting extends CustomBooleanSetting {

	public SlimeBabiesOnDeathSetting() {
		super("Slime_Babies_On_Death");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return Slime.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(CompMaterial.SLIME_SPAWN_EGG,
				"&rSpawn Slime Babies?",
				"",
				"Status: " + (this.getValue() ? "&aenabled" : "&7disabled"),
				"",
				"Should this Boss spawn",
				"smaller slimes on death?");
	}
}

class SlimeSizeSetting extends CustomIntegerSetting {

	public SlimeSizeSetting() {
		super("Slime_Size");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return Slime.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		((Slime) entity).setSize(this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		final ItemCreator is = ItemCreator.from(
				CompMaterial.SLIME_BALL,
				"Slime Size",
				"",
				"Adjust the slime size.");

		if (this.getValue() == 50)
			is.lore(
					"",
					"&dBeware: Reached the",
					"&dmaximum slime size.");
		else if (this.getValue() > 24)
			is.lore(
					"",
					"&4Warning: Extremely oversized",
					"&4slimes are bugged and have",
					"&4inproper hit-box.");
		else if (this.getValue() > 15)
			is.lore(
					"",
					"&cWarning: Slime is oversized.",
					"&cYour server may not handle",
					"&cit correctly.");
		else if (this.getValue() > 10)
			is.lore(
					"",
					"&cNB: &7Too big slimes do not",
					"&7behave properly and may",
					"&7despawn randomly.");
		return is;
	}

	@Override
	public int getMinimum() {
		return 1;
	}

	@Override
	public int getMaximum() {
		return 40;
	}
}

class PhantomSizeSetting extends CustomIntegerSetting {

	private boolean hasClass;

	public PhantomSizeSetting() {
		super("Phantom_Size");

		try {
			Class.forName("org.bukkit.entity.Phantom");
			this.hasClass = true;

		} catch (final ReflectiveOperationException ex) {
			this.hasClass = false;
		}
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		if (!this.hasClass)
			return false;

		return Phantom.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		((Phantom) entity).setSize(this.getValue());
	}

	@Override
	public ItemCreator getIcon() {
		final ItemCreator is = ItemCreator.from(
				CompMaterial.ENDER_EYE,
				"Phantom Size",
				"",
				"Adjust the Phantom size.");
		return is;
	}

	@Override
	public int getMinimum() {
		return 1;
	}

	@Override
	public int getMaximum() {
		return 40;
	}
}

class RabbitTypeSetting extends CustomStringSetting<Rabbit.Type> {

	private boolean hasClass;

	public RabbitTypeSetting() {
		super("Rabbit_Type");

		try {
			Class.forName("org.bukkit.entity.Phantom");
			this.hasClass = true;

		} catch (final ReflectiveOperationException ex) {
			this.hasClass = false;
		}
	}

	@Override
	public boolean canApplyTo(EntityType entity) {

		if (!this.hasClass)
			return false;

		return Rabbit.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		final String typeName = this.getValue();

		if (typeName != null) {
			final Rabbit.Type rabbitType = ReflectionUtil.lookupEnumSilent(Rabbit.Type.class, typeName.toUpperCase());

			if (rabbitType != null)
				((Rabbit) entity).setRabbitType(rabbitType);
		}
	}

	@Override
	protected String getChangeQuestion() {
		return "Enter rabit type for this Boss. Current: '{current}'. Available: {available}. Type 'default' to apply vanilla rules (random).";
	}

	@Override
	protected List<Rabbit.Type> getValidTypes() {
		return Arrays.asList(ReflectionUtil.getEnumValues(Rabbit.Type.class));
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.RABBIT_FOOT,
				"Rabbit Type",
				"",
				"Current: &f" + Common.getOrDefault(this.getValue(), "random"),
				"",
				"Click to change the rabbit",
				"type (e.g. killer rabbit).");
	}
}

class EndermanTeleportSetting extends CustomBooleanSetting {

	public EndermanTeleportSetting() {
		super("Enderman_Teleport");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return Enderman.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ENDER_PEARL,
				"Enderman Teleport",
				"",
				this.getValue() ? "&fEnderman teleports normally." : "&cEnderman does not teleport.",
				"",
				"If false, it completely prevents",
				"the Enderman from teleporting.");
	}
}

class EnderdragonGriefSetting extends CustomBooleanSetting {

	public EnderdragonGriefSetting() {
		super("Enderdragon_Grief");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return EnderDragon.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ENDER_EYE,
				"Enderdragon Grief",
				"",
				this.getValue() ? "&cEnderdragon destroys blocks." : "&aEnderdragon does not destroy blocks.",
				"",
				"If false, this dragon will",
				"not destroy any blocks",
				"- hopefully.");
	}
}

class EnderdragonPhaseSetting extends CustomStringSetting<CompPhase> {

	private boolean hasClass;

	public EnderdragonPhaseSetting() {
		super("Enderdragon_Phase");

		try {
			Class.forName("org.bukkit.entity.EnderDragon$Phase");
			this.hasClass = true;

		} catch (final ReflectiveOperationException ex) {
			this.hasClass = false;
		}
	}

	@Override
	public boolean canApplyTo(EntityType entity) {

		if (!this.hasClass)
			return false;

		return EnderDragon.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		final String typeName = this.getValue();
		final EnderDragon dragon = (EnderDragon) entity;

		if (typeName != null && !typeName.isEmpty())
			dragon.setPhase(ReflectionUtil.lookupEnum(EnderDragon.Phase.class, typeName));
	}

	@Override
	protected String getChangeQuestion() {
		return "Enter dragon phase at spawn. Behavior is controled by Minecraft server, not by us. See https://mineacademy.org/dragon-phase for help. "
				+ "Current: '{current}'. Available: {available}. Type 'default' to reset to circling.";
	}

	@Override
	protected List<CompPhase> getValidTypes() {
		return Arrays.asList(CompPhase.values());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ELYTRA,
				"Phase At Startup",
				"",
				"Current: &f" + Common.getOrDefault(this.getValue(), "circling (default)"),
				"",
				"Click to change the dragon phase",
				"on spawn. See this link for more",
				"info: mineacademy.org/dragon-phase");
	}

	// For compatibility reasons mostly
	enum CompPhase {
		CIRCLING,
		STRAFING,
		FLY_TO_PORTAL,
		LAND_ON_PORTAL,
		LEAVE_PORTAL,
		BREATH_ATTACK,
		SEARCH_FOR_BREATH_ATTACK_TARGET,
		ROAR_BEFORE_ATTACK,
		CHARGE_PLAYER,
		DYING,
		HOVER
	}
}

class IronGolemAggressiveSetting extends CustomBooleanSetting {

	public IronGolemAggressiveSetting() {
		super("Iron_Golem_Aggressive");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return IronGolem.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public Boolean getDefault() {
		return true;
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.RED_DYE,
				"Iron Golem Attack",
				"",
				this.getValue() ? "&7The Golem attacks monsters." : "&cGolem ignores monsters.",
				"",
				"If false, your Iron Golem Boss",
				"will no longer attack monsters.");
	}
}

/**
 * @deprecated It is now possible to create Villager Zombies directly in later MC versions
 */
@Deprecated
class ZombieVillagerSetting extends CustomBooleanSetting {

	public ZombieVillagerSetting() {
		super("Zombie_Villager");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		if (CompEntityType.ZOMBIE_VILLAGER != null)
			return false;

		return Zombie.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (entity instanceof Zombie)
			((Zombie) entity).setVillager(this.getValue());

	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.ROTTEN_FLESH,
				"Zombie Villager",
				"",
				this.getValue() ? "&cBoss is a Zombie Villager." : "&7Boss is not a Zombie Villager.",
				"",
				"Should this Boss be a",
				"Zombie Villager?");
	}
}

/**
 * @deprecated It is now possible to create Skeleton types directly in later MC versions
 */
@Deprecated
class SkeletonTypeSetting extends CustomStringSetting<SkeletonType> {

	public SkeletonTypeSetting() {
		super("Skeleton_Type");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		if (CompEntityType.WITHER_SKELETON != null)
			return false;

		return Skeleton.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (this.getValue() != null) {
			final SkeletonType skeletonType = ReflectionUtil.lookupEnumSilent(SkeletonType.class, this.getValue().toUpperCase());

			if (skeletonType != null)
				((Skeleton) entity).setSkeletonType(skeletonType);
		}
	}

	@Override
	protected String getChangeQuestion() {
		return "Enter skeleton type for this Boss. Current: '{current}'. Available: {available}. Type 'default' to spawn normal skeleton.";
	}

	@Override
	protected List<SkeletonType> getValidTypes() {
		return Arrays.asList(ReflectionUtil.getEnumValues(SkeletonType.class));
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.BONE,
				"Skeleton Type",
				"",
				"Current: &f" + Common.getOrDefault(this.getValue(), "default"),
				"",
				"Click to change the",
				"Boss skeleton type.");
	}
}

class VillagerProfessionSetting extends CustomStringSetting<CompVillagerProfession> {

	private boolean hasClasses = false;
	private boolean isNewMinecraft = false;

	public VillagerProfessionSetting() {
		super("Villager_Profession");

		this.isNewMinecraft = MinecraftVersion.newerThan(V.v1_12);

		Class<?> professionClass;

		try {
			professionClass = Class.forName("org.bukkit.entity.Villager$Profession");

		} catch (final Throwable t) {
			return;
		}

		try {
			Zombie.class.getMethod("setVillagerProfession", professionClass);

			this.hasClasses = true;
		} catch (final Throwable t) {
		}
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		if (entity == CompEntityType.HUSK)
			return false;

		if (!this.hasClasses)
			return false;

		if (entity == CompEntityType.ZOMBIE && this.isNewMinecraft)
			return false;

		return Zombie.class.isAssignableFrom(entity.getEntityClass()) || Villager.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {

		if (this.getValue() == null)
			return;

		final Villager.Profession profession;

		try {
			profession = CompVillagerProfession.convertNameToBukkit(this.getValue().toUpperCase());
		} catch (final MissingEnumException ex) {
			return;
		}

		if (entity.getType().toString().equals("ZOMBIE_VILLAGER"))
			((ZombieVillager) entity).setVillagerProfession(profession);

		else if (entity instanceof Zombie)
			((Zombie) entity).setVillagerProfession(profession);

		else
			((Villager) entity).setProfession(profession);
	}

	@Override
	protected String getChangeQuestion() {
		return "Enter villager profession for this Boss. Current: '{current}'. Available: {available}. Type 'default' to apply vanilla rules (random).";
	}

	@Override
	protected List<CompVillagerProfession> getValidTypes() {
		return Arrays.asList(CompVillagerProfession.values());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.IRON_PICKAXE,
				"Villager Profession",
				"",
				"Current: &f" + Common.getOrDefault(this.getValue(), "random"),
				"",
				"Click to set the",
				"villager profession.");
	}
}

class VillagerTypeSetting extends CustomStringSetting<CompVillagerType> {

	private boolean hasClass = false;

	public VillagerTypeSetting() {
		super("Villager_Type");

		try {
			Class.forName("org.bukkit.entity.Villager$Type");
			this.hasClass = true;

		} catch (final Throwable t) {
		}
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		if (!this.hasClass)
			return false;

		return Villager.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (this.getValue() != null) {
			final Villager.Type villagerType;

			try {
				villagerType = CompVillagerType.convertNameToBukkit(this.getValue().toUpperCase());

			} catch (final MissingEnumException ex) {
				return;
			}

			((Villager) entity).setVillagerType(villagerType);
		}
	}

	@Override
	protected String getChangeQuestion() {
		return "Enter villager type for this Boss. Current: '{current}'. Available: {available}. Type 'default' to apply vanilla rules (biome-specific).";
	}

	@Override
	protected List<CompVillagerType> getValidTypes() {
		return Arrays.asList(CompVillagerType.values());
	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.VILLAGER_SPAWN_EGG,
				"Villager Type",
				"",
				"Current: &f" + Common.getOrDefault(this.getValue(), "random"),
				"",
				"Click to set the",
				"villager type.");
	}
}

class SnowmanPumpkinSetting extends CustomBooleanSetting {

	public SnowmanPumpkinSetting() {
		super("Snowman_Pumpkin");
	}

	@Override
	public boolean canApplyTo(EntityType entity) {
		return Snowman.class.isAssignableFrom(entity.getEntityClass());
	}

	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
		if (entity instanceof Snowman) {
			try {
				((Snowman) entity).setDerp(!this.getValue());

			} catch (final Throwable t) {
				final Object handle = Remain.getHandleEntity(entity);

				try {
					ReflectionUtil.invoke("setPumpkin", handle, this.getValue());

				} catch (final Throwable tt) {
					try {
						ReflectionUtil.invoke("setHasPumpkin", handle, this.getValue());
					} catch (final Throwable ttt) {
						// Unsupported MC version
					}
				}
			}
		}

	}

	@Override
	public ItemCreator getIcon() {
		return ItemCreator.from(
				CompMaterial.PUMPKIN,
				"Snowman Pumpkin",
				"",
				this.getValue() ? "&7Snowman has pumpkin." : "&6Snowman has no pumpkin.",
				"",
				"Should this snowman carry a",
				"pumpkin? Please note that",
				"this setting is not persistent",
				"and might reset when reloading",
				"the chunk (according to Spigot).",
				"",
				"&cThis setting might not work",
				"&con Minecraft 1.8.8.");
	}
}