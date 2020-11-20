package org.mineacademy.boss.api;

import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.model.ConfigSerializable;

public interface BossEquipment extends ConfigSerializable, Iterable<BossDrop> {

	/**
	 * Get the item on Boss' primary hand (right hand)
	 *
	 * @param slot where on the equipment to get the item from
	 * @return the item, or null if not set
	 */
	BossDrop get(BossEquipmentSlot slot);

	/**
	 * Get unmodifiable all drops.
	 *
	 * @return an unmodifiable all drops
	 */
	Map<BossEquipmentSlot, BossDrop> getAllCopy();

	/**
	 * Set the item on an equipment slot that will never drop.
	 *
	 * @param slot where on the equipment to set the item
	 * @param item the item
	 */
	void set(BossEquipmentSlot slot, ItemStack item);

	/**
	 * Set the item on an equipment slot with a drop chance.
	 *
	 * @param slot       where on the equipment to set the item
	 * @param item       the item, set to null to remove
	 * @param dropChance the drop chance from 0.0 (never drops) to 1.0 (always drops)
	 */
	void set(BossEquipmentSlot slot, ItemStack item, float dropChance);

	/**
	 * Should we allow vanilla rules to give the mob random equipment?
	 * Example: Zombie can be given different leather / chainmail armor and swords
	 * even when you don't specify any equipment.
	 *
	 * @return if we should allow Minecraft vanilla rules to give the mob random equipment
	 */
	boolean allowRandom();

	/**
	 * Allow random equipment? See {@link #allowRandom()}
	 *
	 * @param flag, allow random?
	 */
	void setAllowRandom(boolean flag);
}
