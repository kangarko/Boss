package org.mineacademy.boss.api;

import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.RequiredArgsConstructor;

/**
 * Represents where on the {@link BossEquipment} an item should be.
 */
@RequiredArgsConstructor
public enum BossEquipmentSlot {

	/**
	 * The primary hand
	 */
	HAND(MinecraftVersion.olderThan(V.v1_9) ? "itemInHand" : "itemInMainHand"),

	/**
	 * The secondary hand
	 */
	OFF_HAND(MinecraftVersion.olderThan(V.v1_9) ? "itemInHand" : "itemInOffHand"),

	/**
	 * The helmet
	 */
	HELMET,

	/**
	 * The chestplate
	 */
	CHESTPLATE,

	/**
	 * The leggings
	 */
	LEGGINGS,

	/**
	 * The boots
	 */
	BOOTS;

	/**
	 * Represents the name of the method which sets this equipment slot in Bukkit's API.
	 */
	private final String methodName;

	BossEquipmentSlot() {
		this(null);
	}

	/**
	 * Get the name of the method which sets the equipment slot in Bukkit's API.
	 *
	 * @return the method name, or the enum name if the method name is not defined
	 */
	public String getMethodName() {
		return methodName != null ? methodName : name().toLowerCase();
	}
}
