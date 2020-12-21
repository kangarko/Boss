package org.mineacademy.boss.api;

import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.util.AutoUpdateList;
import org.mineacademy.boss.util.AutoUpdateMap;
import org.mineacademy.fo.model.RangedRandomValue;
import org.mineacademy.fo.model.SimpleSound;

/**
 * Represents various settings related to the boss
 */
public interface BossSettings {

	/**
	 * Get the custom name of the Boss displayed above him.
	 *
	 * @return the custom name, or null if hidden
	 */
	String getCustomName();

	/**
	 * Set the visible name of the Boss above him.
	 * By default this returns the Boss' name.
	 * <p>
	 * Setting it to null hides the name.
	 *
	 * @param customName the custom name
	 */
	void setCustomName(String customName);

	/**
	 * Get the message that is sent to the player when he cannot spawn
	 * the Boss due to lacking permission.
	 *
	 * @return the no permission message
	 */
	String getNoSpawnPermissionMessage();

	/**
	 * Set the message that is sent to the player when he cannot spawn
	 * the Boss due to lacking permission.
	 *
	 * @param noSpawnPermissionMessage, the message, or null for default
	 */
	void setNoSpawnPermissionMessage(String noSpawnPermissionMessage);

	/**
	 * Get the Boss' health as defined in the menu.
	 *
	 * @return the Boss' health
	 */
	int getHealth();

	/**
	 * Set the health of the Boss.
	 *
	 * @param health the new health
	 */
	void setHealth(int health);

	/**
	 * Gets the egg item.
	 *
	 * @return
	 */
	BossEggItem getEggItem();

	/**
	 * Get the dropped experience when the Boss dies
	 *
	 * @return dropped exp
	 */
	RangedRandomValue getDroppedExp();

	/**
	 * Set the dropped experience upon Boss' death
	 *
	 * @param value the new value
	 */
	void setDroppedExp(RangedRandomValue value);

	/**
	 * Get a list of (unmodifiable) potions that apply upon spawn.
	 *
	 * @return the active potions, null if not set
	 */
	AutoUpdateList<BossPotion> getPotions();

	/**
	 * Get a level of a certain potion type.
	 *
	 * @param type the potion type
	 * @return the level of it
	 */
	int getLevelOf(PotionEffectType type);

	/**
	 * Set a certain potion level to this Boss.
	 * Will override an existing potion.
	 *
	 * @param type  the potion type
	 * @param level the new level, or 0 to remove
	 */
	void setPotion(PotionEffectType type, int level);

	/**
	 * Get all specific settings as an UNMODIFIABLe set.
	 *
	 * @return all settings
	 */
	Set<BossSpecificSetting> getSpecificSettings();

	/**
	 * Set a new value to the setting.
	 * <p>
	 * PLEASE SEE {@link BossSpecificSetting} for what kind of values what settings accept.
	 *
	 * @param setting the setting
	 * @param value   the value
	 */
	void setSpecificSetting(BossSpecificSetting setting, Object value);

	/**
	 * Get the setting for this Boss, or default if not set.
	 *
	 * @param setting the setting
	 * @return the value, or default if not set
	 */
	Object getSpecificSetting(BossSpecificSetting setting);

	/**
	 * Get a a map of sounds that should be remapped
	 * Default entity sound -> our custom sound
	 * <p>
	 * Requires ProtocolLib.
	 *
	 * @return the remapped sounds
	 */
	AutoUpdateMap<Sound, SimpleSound> getRemappedSounds();

	/**
	 * If ProtocolLib is installed, all sounds will be printed to the console
	 * and to server operators from this Boss.
	 *
	 * @return if Boss' sounds shall be logged?
	 */
	boolean isDebuggingSounds();

	/**
	 * Set if sounds are debugged. See {@link #isDebuggingSounds()}
	 *
	 * @param flag true or false
	 */
	void setDebuggingSounds(boolean flag);

	/**
	 * Get the riding entity, null if none.
	 *
	 * @return the entity the Boss sits on
	 */
	EntityType getRidingVanilla();

	/**
	 * Set the riding entity.
	 *
	 * @param type the entity the Boss sits on
	 */
	void setRidingVanilla(EntityType type);

	/**
	 * Get whether the entity the boss sits on should die with the boss
	 *
	 * @return true/false
	 */
	boolean isRemovingRidingOnDeath();

	/**
	 * Set if the entity the boss sits on should die with the boss
	 *
	 * @param setting the setting
	 */
	void setRemoveRidingOnDeath(boolean setting);

	/**
	 * Should we only drop one item?
	 *
	 * @return if we should only drop one item
	 */
	boolean hasSingleDrops();

	/**
	 * Should we only drop one item?
	 *
	 * @param singleDrops new option
	 */
	void setSingleDrops(boolean singleDrops);

	/**
	 * Return if we should drop vanilla drops (false by default).
	 *
	 * @return whether we should drop vanilla drops (drop by default)
	 */
	boolean hasNaturalDrops();

	/**
	 * Set if we should drop vanilla drops (false by default)
	 *
	 * @param clearDrops
	 */
	void setNaturalDrops(boolean naturalDrops);

	/**
	 * Get whether the boss should drop rewards directly into player's inventories
	 *
	 * @return
	 */
	boolean hasInventoryDrops();

	/**
	 * Set whether the boss should drop rewards directly into player's inventories
	 *
	 * @param enabled the setting
	 */
	void setInventoryDrops(boolean enabled);

	/**
	 * Get the limit for the number of player that can receive rewards
	 *
	 * @return the player limit
	 */
	int getInventoryDropsPlayerLimit();

	/**
	 * Set the limit for the number of player that can receive rewards
	 *
	 * @param setting the setting
	 */
	void setInventoryDropsPlayerLimit(int setting);

	/**
	 * Get the time in seconds for how long a hit is counted
	 *
	 * @return the time limit, in seconds
	 */
	int getInventoryDropsTimeLimit();

	/**
	 * Set the time in seconds for how long a hit is counted
	 *
	 * @param timeLimit the time limit, in seconds
	 */
	void setInventoryDropsTimeLimit(int timeLimit);

	/**
	 * Get riding boss
	 *
	 * @return the riding boss
	 */
	String getRidingBoss();

	/**
	 * Set the riding entity (boss).
	 *
	 * @param the boss to ride
	 */
	void setRidingBoss(String boss);

	/**
	 * Get the damage modifier for this boss
	 *
	 * @return
	 */
	double getDamageMultiplier();

	/**
	 * Set a new damage multiplier for this boss.
	 * Example: 1.5 for 150% damage instead of 100%,
	 * or 0.25 for 25% of his damage
	 *
	 * @param damageMultiplier
	 */
	void setDamageMultiplier(double damageMultiplier);

	int getConvertingChance();

	void setConvertingChance(int chance);
}
