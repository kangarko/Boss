package org.mineacademy.boss.api;

import java.util.Collection;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;

/**
 * The main API class for the Boss plugin.
 */
public final class BossAPI {

	/**
	 * Create a new Boss
	 *
	 * @param type
	 * @param name the file name for the Boss without extension
	 * @return a Boss, or null if not found
	 */
	public static Boss createBoss(EntityType type, String name) {
		return Boss.createBoss(name, type);
	}

	/**
	 * Remove an existing boss by name
	 *
	 * @param name the Boss' name
	 */
	public static void removeBoss(String name) {
		Boss.removeBoss(name);
	}

	/**
	 * Determines whether or not this item holds a valid Boss spawner egg registered
	 * within the Boss plugin.
	 *
	 * @param item the {@link ItemStack}
	 * @return if this item holds a Boss
	 */
	public static boolean isBoss(ItemStack item) {
		return Boss.findBoss(item) != null;
	}

	/**
	 * Determines if the entity is registered within the Boss plugin.
	 *
	 * @param entity the {@link Entity}
	 * @return if this entity is a Boss
	 */
	public static boolean isBoss(Entity entity) {
		return Boss.findBoss(entity) != null;
	}

	/**
	 * Attempts to find a registered Boss from an entity.
	 *
	 * @param entity the entity to search for
	 * @return a Boss, or null if not found
	 */
	public static SpawnedBoss getBoss(Entity entity) {
		return Boss.findBoss(entity);
	}

	/**
	 * Attempts to find a registered Boss from an item.
	 *
	 * @param item the item to evaluate
	 * @return a Boss, or null if not found
	 */
	public static Boss getBoss(ItemStack item) {
		return Boss.findBoss(item);
	}

	/**
	 * Attempts to find a registered Boss directly from its file name.
	 *
	 * @param name the file name in bosses/ folder
	 * @return a Boss, or null if not found
	 */
	public static Boss getBoss(String name) {
		return Boss.findBoss(name);
	}

	/**
	 * Get all active Bosses in the specific world.
	 *
	 * @param world the world to look in
	 * @return the active Bosses in the world
	 */
	public static Collection<SpawnedBoss> getBosses(World world) {
		return Boss.findBossesAliveIn(world);
	}

	/**
	 * Get all installed Bosses as unmodifiable list
	 *
	 * @return all available bosses
	 */
	public static Collection<Boss> getBosses() {
		return Boss.getBosses();
	}

	/**
	 * Get the list of all {@link EntityType} that the the Boss may be of.
	 *
	 * @return a list of all valid Boss' entity types
	 */
	public static Set<EntityType> getValidTypes() {
		return Boss.getValidEntities();
	}
}
