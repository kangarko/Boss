package org.mineacademy.boss.model.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.mineacademy.boss.storage.SimpleRegionData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualizedRegion;

public final class RegionBoss implements Regionable {

	@Override
	public Collection<String> getAll() {
		return Common.convert(getData().getRegions(), VisualizedRegion::getName);
	}

	@Override
	public List<String> getObject(Location loc) {
		final List<VisualizedRegion> region = getData().getRegions(loc);
		if (region.isEmpty())
			return null;

		final List<String> converted = new ArrayList<>();

		for (final VisualizedRegion rg : region)
			converted.add(rg.getName());

		return converted;
	}

	@Override
	public Region getBoundingBox(String region) {
		return getData().getRegion(region);
	}

	private SimpleRegionData getData() {
		return SimpleRegionData.$();
	}

	@Override
	public Material getButtonMaterial() {
		return CompMaterial.SKELETON_SKULL.getMaterial();
	}

	@Override
	public String[] getButtonDescription() {
		return new String[] {
				"Regions created",
				"via &6/boss region"
		};
	}
}
