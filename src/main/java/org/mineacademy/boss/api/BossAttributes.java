package org.mineacademy.boss.api;

import java.util.Set;

import org.mineacademy.boss.model.BossAttribute;
import org.mineacademy.fo.model.ConfigSerializable;

/**
 * Attributes are generic properties for all entities in game,
 * as described per http://minecraft.gamepedia.com/Attribute
 * <p>
 * Each Boss is capable of bearing all attributes valid to its entity type.
 */
public interface BossAttributes extends ConfigSerializable {

	/**
	 * Get an attribute of the Boss, regardless of whether or not
	 * this has been set via the user menu (if not, default is provided).
	 *
	 * @param attr the attribute
	 * @return the base value, or Minecraft's default if not set explicitly
	 */
	double get(BossAttribute attr);

	/**
	 * Get the default base attribute (from Minecraft, before modifiers
	 * are applied) of the Boss.
	 *
	 * @param attr the attribute
	 * @return the default base value
	 */
	double getDefaultBase(BossAttribute attr);

	/**
	 * Set an attribute with a base value.
	 * <p>
	 * The modifier cannot be altered via this API.
	 *
	 * @param attr  the attribute
	 * @param value the base value
	 */
	void set(BossAttribute attr, double value);

	/**
	 * Get all attributes of the Boss saved in the data file.
	 * Used internally when spawning the Boss.
	 * <p>
	 * Cannot be modified!
	 *
	 * @return the user-changed attributes
	 */
	Set<BossAttribute> getConfigured();

	/**
	 * Get all available attributes from Spigot / Craftbukkit
	 * for this entity type.
	 * Cannot be modified!
	 *
	 * @return attributes from the Minecraft server
	 */
	Set<BossAttribute> getVanilla();
}