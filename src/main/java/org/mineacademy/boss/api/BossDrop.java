package org.mineacademy.boss.api;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.impl.SimpleSettings;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.Getter;

public class BossDrop implements ConfigSerializable {

	private final SimpleSettings settings;

	/**
	 * Get the item represented by this drop
	 */
	@Getter
	private final ItemStack item;

	/**
	 * Gets the chance of the item being dropped upon this Boss's death.
	 *
	 * <ul>
	 * <li>A drop chance of 0.0F will never drop
	 * <li>A drop chance of 1.0F will always drop
	 * </ul>
	 */
	@Getter
	private float dropChance = 0;

	public BossDrop(final ItemStack item, final float dropChance) {
		this(null, item, dropChance);
	}

	private BossDrop(final SimpleSettings settings, final ItemStack item, final float dropChance) {
		this.settings = settings;
		this.item = item;
		this.dropChance = dropChance;
	}

	/**
	 * Sets the chance of the item being dropped upon this Boss' death.
	 *
	 * <ul>
	 * <li>A drop chance of 0.0F will never drop
	 * <li>A drop chance of 1.0F will always drop
	 * </ul>
	 *
	 * @param chance of the item being dropped
	 */
	public final void setDropChance(final float chance) {
		this.dropChance = chance;

		if (settings != null)
			settings.updateDrops();
	}

	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("item", item);
		map.putIfExist("dropChance", dropChance);

		return map;
	}

	public static final BossDrop deserialize(final SerializedMap map, final SimpleSettings settings) {
		if (map.containsKey("item") && map.containsKey("dropChance")) {
			final ItemStack item = map.getItem("item");
			final float dropChance = map.getFloat("dropChance");

			return new BossDrop(settings, item, dropChance);
		}

		return null;
	}

	@Override
	public final String toString() {
		return "Drop{" + item.getType() + " " + dropChance * 100 + "%}";
	}
}
