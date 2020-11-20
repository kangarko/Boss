package org.mineacademy.boss.api;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.util.AutoUpdateList;
import org.mineacademy.boss.util.AutoUpdateMap;

/**
 * Represents a simple Boss.
 */
public interface Boss {

	// ------------------------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------------------------

	/**
	 * Get the Bukkit type of this Boss.
	 *
	 * @return the {@link EntityType} that this Boss represents
	 */
	EntityType getType();

	/**
	 * Get the exact name of the settings file.
	 * <p>
	 * This name is used internally in spawner eggs.
	 *
	 * @return the name of the Boss' settings file
	 */
	String getName();

	/**
	 * Gets the Boss name that is lower case, and has spaces and diacritics
	 * stripped.
	 *
	 * @return a name compatible with permission
	 */
	String getSpawnPermission();

	/**
	 * Get user alterable settings for this Boss.
	 */
	BossSettings getSettings();

	/**
	 * Get the Boss' equipment, including hands and armor content.
	 *
	 * @return Boss' equipment
	 */
	BossEquipment getEquipment();

	/**
	 * Get the Boss' drops, items that are dropped with a specific chance when the
	 * Boss dies.
	 *
	 * @return Boss' drops, as slots in inventory and actual drops
	 */
	AutoUpdateMap<Integer, BossDrop> getDrops();

	/**
	 * Get other Bosses spawned upon this Boss' own death. The map contains of other
	 * Boss' names and how many of them to summon.
	 *
	 * @return the death Boss' reinforcements
	 */
	AutoUpdateMap<String, Integer> getReinforcementsBoss();

	/**
	 * Get vanilla monsters spawned upon this Boss' own death. The map contains of
	 * {@link EntityType} names and how many of them to summon.
	 *
	 * @return the death native reinforcements
	 */
	AutoUpdateMap<String, Integer> getReinforcementsVanilla();

	/**
	 * Get the list of what commands to execute (as the console) when this Boss
	 * spawns.
	 *
	 * @return the spawn commands
	 */
	AutoUpdateMap<String, Double> getSpawnCommands();

	/**
	 * Get the list of what commands to execute (as the console) when this Boss
	 * dies.
	 *
	 * @return the death commands
	 */
	AutoUpdateMap<String, Double> getDeathCommands();

	/**
	 * Get the list of what commands to execute (as the player)
	 * if direct inventory drops are enabled.
	 * <p>
	 * Typically this means that we run these commands for the last X amount of
	 * players who attacked the boss in a certain amount of time before he died.
	 *
	 * @return
	 */
	AutoUpdateMap<String, Double> getDeathByPlayerCommands();

	/**
	 * Get if the Boss strikes lightning bolt on death
	 *
	 * @return if the Boss strikes lightning bolt on death
	 */
	boolean hasLightningOnDeath();

	/**
	 * Set if the Boss strikes lightning bolt on death
	 *
	 * @param flag if the Boss strikes lightning bolt on death
	 */
	void setLightningOnDeath(boolean flag);

	/**
	 * Get the Boss' attributes, which are generic properties as described per
	 * http://minecraft.gamepedia.com/Attribute
	 *
	 * @return Boss' attributes
	 */
	BossAttributes getAttributes();

	/**
	 * Get the Boss' skills, which are special abilities the Bosses dispose, such as
	 * throw the player into the air or teleport to the player.
	 *
	 * @return Boss' skills
	 */
	AutoUpdateList<BossSkill> getSkills();

	/**
	 * Get the Boss' natural occurence on the server
	 *
	 * @return the Boss spawning
	 */
	BossSpawning getSpawning();

	/**
	 * If {@link BossSettings#hasInventoryDrops()} is enabled, we give item rewards
	 * directly to players' inventories instead of dropping on the ground.
	 * <p>
	 * Return the class that manages the last attacker players as well as times of
	 * their attack.
	 *
	 * @return the damage tracker for this boss
	 */
	BossDropsManager getDropsManager();

	// ------------------------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Summons this Boss at a certain {@link Location}.
	 *
	 * @param loc where to spawn
	 * @return the {@link SpawnedBoss} or null if event was cancelled
	 */
	SpawnedBoss spawn(Location loc, BossSpawnReason reason);

	/**
	 * Creates a Boss' spawner egg.
	 * <p>
	 * Warning: This egg only holds the Boss' name (not the entire Boss instance)
	 * and the plugin will identify it via {@link PlayerInteractEvent} to spawn the
	 * proper Boss.
	 *
	 * @return the Boss' spawner egg
	 */
	ItemStack asEgg();
}
