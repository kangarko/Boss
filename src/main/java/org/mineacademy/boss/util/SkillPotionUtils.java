package org.mineacademy.boss.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.FoException;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class SkillPotionUtils {

	public PotionEffect parseEffect(final String line) {
		final String[] split = line.split(", ");
		final Matcher matcher = Common.compileMatcher("([a-zA-Z_ ]{1,})(([1-9]{1,})|())", split[0]);

		if (matcher.find()) {
			final String name = matcher.group(1).replaceAll("\\s+$", "");
			final PotionEffectType type = SerializeUtil.deserialize(PotionEffectType.class, name);

			final int level = matcher.groupCount() > 2 && matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;
			final String durationRaw = split.length == 2 ? split[1] : "1 minute";
			final int durationTicks = (int) TimeUtil.toTicks(durationRaw);

			return new PotionEffect(type, durationTicks, level - 1);
		}

		throw new FoException("Invalid syntax for potion effect: " + line);
	}

	public StrictList<PotionEffect> readSettings(final Map<String, Object> map) {
		final StrictList<PotionEffect> potions = new StrictList<>();
		final Object obj = map.get("Effects");
		final List<String> effects = obj instanceof List ? (List<String>) obj : new ArrayList<>();

		for (final String line : effects) {
			final PotionEffect effect = SkillPotionUtils.parseEffect(line);

			potions.add(effect);
		}

		return potions;
	}

	public Map<String, Object> writeSettings(final Iterable<PotionEffect> potions) {
		final Map<String, Object> map = new HashMap<>();
		final List<String> wrapped = new ArrayList<>();

		for (final PotionEffect ef : potions)
			wrapped.add(ef.getType().toString() + " " + (ef.getAmplifier() + 1) + ", " + ef.getDuration() + " ticks");

		map.put("Effects", wrapped);

		return map;
	}

	public String[] getDefaultHeader() {
		return new String[] {
				" ",
				"For potion effects, use the following syntax",
				"<potion name> <level>, <duration>",
				" ",
				"Example:",
				" Effects:",
				"   - SLOWNESS 2, 10 seconds",
				"   - BLINDNESS, 4 seconds"
		};
	}
}
