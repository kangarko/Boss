package org.mineacademy.boss.api;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * Represents a Boss Spawner Egg.
 * <p>
 * Call {@link Boss#asEgg()} to get the {@link ItemStack}.
 */
public interface BossEggItem extends ConfigSerializable {

	/**
	 * Sets a new material.
	 *
	 * @param mat
	 */
	void setMaterial(CompMaterial mat);

	/**
	 * Gets the material
	 *
	 * @return
	 */
	CompMaterial getMaterial();

	/**
	 * Set a new name (will be colorized)
	 * {boss} is replaced with the Boss name
	 *
	 * @param title
	 */
	void setName(String name);

	/**
	 * Get the title
	 *
	 * @return
	 */
	String getName();

	/**
	 * Sets a new lore. Lines are colorized with {boss} replaced by Boss name.
	 *
	 * @param lore
	 */
	void setLore(List<String> lore);

	/**
	 * Get the lore lines.
	 *
	 * @return
	 */
	List<String> getLore();

	/**
	 * Gives an invisible enchant to make the item glow
	 *
	 * @param glow make it glow?
	 */
	void setGlowing(boolean glow);

	/**
	 * Does the item have invisible enchant for glowing effect?
	 *
	 * @return
	 */
	boolean isGlowing();
}
