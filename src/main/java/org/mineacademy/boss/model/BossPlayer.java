package org.mineacademy.boss.model;

import org.bukkit.Location;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;

public final class BossPlayer {

	@Getter
	private VisualizedRegion createdRegion;

	public void reset() {
		this.createdRegion = null;
	}

	public void setCreatedRegionPoint(Location location, boolean primary) {
		if (createdRegion == null)
			createdRegion = new VisualizedRegion(null, null);

		if (primary)
			createdRegion.setPrimary(location);
		else
			createdRegion.setSecondary(location);
	}

}
