package org.mineacademy.boss.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossManager;

/**
 * The main API class for the Boss plugin.
 */
public final class BossAPI {

	/**
	 * The instance of the Boss manager.
	 */
	private static final BossManager manager = BossPlugin.getBossManager();

	/**
	 * Create a new Boss
	 *
	 * @param name the file name for the Boss without extension
	 * @return a Boss, or null if not found
	 */
	public static Boss createBoss(EntityType type, String name) {
		return manager.createBoss(type, name);
	}

	/**
	 * Remove an existing boss by name
	 *
	 * @param name the Boss' name
	 */
	public static void removeBoss(String name) {
		manager.removeBoss(name);
	}

	/**
	 * Determines whether or not this item holds a valid Boss spawner egg registered
	 * within the Boss plugin.
	 *
	 * @param item the {@link ItemStack}
	 * @return if this item holds a Boss
	 */
	public static boolean isBoss(ItemStack item) {
		return manager.findBoss(item) != null;
	}

	/**
	 * Determines if the entity is registered within the Boss plugin.
	 *
	 * @param entity the {@link Entity}
	 * @return if this entity is a Boss
	 */
	public static boolean isBoss(Entity entity) {
		return manager.findBoss(entity) != null;
	}

	/**
	 * Attempts to find a registered Boss from an entity.
	 *
	 * @param entity the entity to search for
	 * @return a Boss, or null if not found
	 */
	public static Boss getBoss(Entity entity) {
		return manager.findBoss(entity);
	}

	/**
	 * Attempts to find a registered Boss from an item.
	 *
	 * @param item the item to evaluate
	 * @return a Boss, or null if not found
	 */
	public static Boss getBoss(ItemStack item) {
		return manager.findBoss(item);
	}

	/**
	 * Attempts to find a registered Boss directly from its file name.
	 *
	 * @param fileName the file name in bosses/ folder
	 * @return a Boss, or null if not found
	 */
	public static Boss getBoss(String fileName) {
		return manager.findBoss(fileName);
	}

	/**
	 * Get all active Bosses in the specific world.
	 *
	 * @param world the world to look in
	 * @return the active Bosses in the world
	 */
	public static Collection<SpawnedBoss> getBosses(World world) {
		return manager.findBosses(world);
	}

	/**
	 * Get all active Bosses in the specific chunk.
	 *
	 * @param chunk the chunk to look in
	 * @return the active Bosses there
	 */
	public static Collection<SpawnedBoss> getBosses(Chunk chunk) {
		return manager.findBosses(chunk);
	}

	/**
	 * Get all installed Bosses as unmodifiable list
	 *
	 * @return all available bosses
	 */
	public static Collection<Boss> getBosses() {
		return manager.getBosses();
	}

	/**
	 * Get the list of all {@link EntityType} that the the Boss may be of.
	 *
	 * @return a list of all valid Boss' entity types
	 */
	public static List<EntityType> getValidTypes() {
		return Collections.unmodifiableList(BossManager.getValidTypes().getSource());
	}
}
