package org.mineacademy.boss.model.region;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.region.Region;

import lombok.NonNull;

public interface Regionable {

	@NonNull
	Collection<String> getAll();

	List<String> getObject(Location loc);

	default boolean isPluginEnabled(String name) {
		return Common.doesPluginExist(name);
	}

	default boolean isWithin(Location loc, List<BossRegionSettings> evaluateAgainst) {
		final List<String> object = getObject(loc);

		if (object != null && !object.isEmpty())
			for (final BossRegionSettings regionable : evaluateAgainst)
				for (final String region : object)
					if (Valid.colorlessEquals(regionable.getRegionName(), region))
						return true;

		return false;
	}

	@NonNull
	Material getButtonMaterial();

	@NonNull
	String[] getButtonDescription();

	default Region getBoundingBox(String region) {
		return null;
	}

	default List<String> listOrNull(String element) {
		return element == null ? null : Collections.singletonList(Common.stripColors(element));
	}
}
