package org.mineacademy.boss.api;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a potion that the Boss can have.
 */
@RequiredArgsConstructor
@Getter
public final class BossPotion {

	/**
	 * The potion kind
	 */
	@NonNull
	private final PotionEffectType type;

	/**
	 * The level (amplifier - 1)
	 */
	private final int level;

	public static BossPotion parse(final String line) {
		final String[] parts = line.split(" ");
		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed value " + line);

		final PotionEffectType type = SerializeUtil.deserialize(PotionEffectType.class, parts[0]);
		final int level = parts.length == 2 && NumberUtils.isNumber(parts[1]) ? Integer.parseInt(parts[1]) : 1;

		return new BossPotion(type, level);
	}

	@Override
	public String toString() {
		return "BossPotion{" + (type != null ? type.getName() : "null") + " " + level + "}";
	}
}
