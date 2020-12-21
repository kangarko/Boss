package org.mineacademy.boss.model.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.region.Region;

public final class RegionWorldGuard implements Regionable {

	@Override
	public Collection<String> getAll() {
		final List<String> regs = HookManager.getRegions();
		final List<String> copy = new ArrayList<>();

		for (final String rg : regs)
			if (!copy.contains(rg))
				copy.add(rg);

		return copy;
	}

	@Override
	public List<String> getObject(Location loc) {
		try {
			final List<String> f = HookManager.getRegions(loc);
			final List<String> copy = new ArrayList<>();

			for (final String rg : f)
				if (!copy.contains(rg))
					copy.add(rg);

			return copy.isEmpty() ? null : copy;
		} catch (final Throwable t) {
			return null;
		}
	}

	@Override
	public Material getButtonMaterial() {
		return Material.WATER_BUCKET;
	}

	@Override
	public boolean isWithin(Location loc, List<BossRegionSettings> evaluateAgainst) {
		for (final String object : HookManager.getRegions(loc)) {
			final String evaluated = Common.stripColors(object);

			return evaluateAgainst.stream().filter(s -> s.getRegionName().equalsIgnoreCase(evaluated)).findFirst().isPresent();
		}

		return false;
	}

	@Override
	public String[] getButtonDescription() {
		return new String[] {
				"Regions from the",
				"WorldGuard plugin."
		};
	}

	@Override
	public Region getBoundingBox(String region) {
		return HookManager.getRegion(region);
	}
}
