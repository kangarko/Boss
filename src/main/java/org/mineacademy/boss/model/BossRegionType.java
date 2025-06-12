package org.mineacademy.boss.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.mineacademy.boss.hook.LandsHook;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Regions from other plugins
 */
public enum BossRegionType {

	/**
	 * Our native region accessible with /boss region command.
	 */
	BOSS("Boss", CompMaterial.SKELETON_SKULL, Arrays.asList("Regions created", "via '/boss region' menu.")) {

		@Override
		protected Collection<String> collectAllRegions() {
			return DiskRegion.getRegionNames();
		}

		@Override
		public List<String> findRegions(Location location) {
			return DiskRegion.findRegionNames(location);
		}
	},

	/**
	 * Residence plugin.
	 */
	RESIDENCE("Residence", CompMaterial.WOODEN_AXE, Arrays.asList("Residences created in", "the Residence plugin.")) {

		@Override
		public Collection<String> collectAllRegions() {
			return HookManager.getResidences();
		}

		@Override
		public List<String> findRegions(Location location) {
			return toList(HookManager.getResidence(location));
		}
	},

	/**
	 * Factions UUID, MCore Factions, Factions 1.6 and LegacyFactions
	 */
	FACTIONS("Factions", CompMaterial.DIAMOND_SWORD, Arrays.asList("Regions from the", "Factions plugin.")) {

		@Override
		public boolean isEnabled() {
			return HookManager.isFactionsLoaded();
		}

		@Override
		public Collection<String> collectAllRegions() {
			return HookManager.getFactions();
		}

		@Override
		public List<String> findRegions(Location location) {
			return toList(HookManager.getFaction(location));
		}
	},

	/**
	 * Towny plugin.
	 */
	TOWNY("Towny", CompMaterial.BRICK, Arrays.asList("Town regions from the", "Towny plugin.")) {

		@Override
		public Collection<String> collectAllRegions() {
			return HookManager.getTowns();
		}

		@Override
		public List<String> findRegions(Location loc) {
			return toList(HookManager.getTown(loc));
		}
	},

	/**
	 * Lands plugin.
	 */
	LANDS("Lands", CompMaterial.GRASS_BLOCK, Arrays.asList("Lands from the", "Lands plugin.")) {

		@Override
		public Collection<String> collectAllRegions() {
			return LandsHook.getLands();
		}

		@Override
		public List<String> findRegions(Location location) {
			return Arrays.asList(LandsHook.findLand(location));
		}
	},

	/**
	 * WorldGuard plugin.
	 */
	WORLDGUARD("WorldGuard", CompMaterial.WATER_BUCKET, Arrays.asList("Regions from the", "WorldGuard plugin.")) {

		@Override
		public Collection<String> collectAllRegions() {
			final List<String> regions = HookManager.getRegions();
			final List<String> copy = new ArrayList<>();

			for (final String region : regions)
				if (!copy.contains(region))
					copy.add(region);

			return copy;
		}

		@Override
		public List<String> findRegions(Location location) {
			try {
				final List<String> regions = HookManager.getRegions(location);
				final List<String> copy = new ArrayList<>();

				for (final String region : regions)
					if (!copy.contains(region))
						copy.add(region);

				return copy;

			} catch (final Throwable t) {
				return null;
			}
		}
	};

	/**
	 * The plugin name
	 */
	@Getter
	private final String plugin;

	/**
	 * If the plugin is enabled
	 */
	@Getter
	private final boolean enabled;

	/**
	 * The menu icon
	 */
	@Getter
	private final CompMaterial icon;

	/**
	 * The menu description
	 */
	@Getter
	private final List<String> description;

	/**
	 * Initialize this class (can't use lombok due to some eclipse error)
	 *
	 * @param plugin
	 * @param icon
	 * @param description
	 */
	BossRegionType(String plugin, CompMaterial icon, List<String> description) {
		this.plugin = plugin;
		this.enabled = Platform.isPluginInstalled(plugin);
		this.icon = icon;
		this.description = description;
	}

	/**
	 * Get all known regions
	 *
	 * @return all known regions
	 */
	public final Collection<String> getRegionNames() {
		final List<String> sorted = new ArrayList<>(this.collectAllRegions());

		java.util.Collections.sort(sorted, (o1, o2) -> Collator.getInstance().compare(o1.toLowerCase(), o2.toLowerCase()));

		return sorted;
	}

	/**
	 * Get if the location is within a list
	 *
	 * @param regionName
	 * @param location the location
	 *
	 * @return true if the location is in the list
	 */
	public final boolean isWithin(String regionName, Location location) {
		final List<String> regions = this.findRegions(location);

		if (regions != null && !regions.isEmpty())
			for (final String otherRegion : regions)
				if (otherRegion != null && otherRegion.equals(regionName))
					return true;

		return false;
	}

	/*
	 * Helper to get all region names
	 */
	protected abstract Collection<String> collectAllRegions();

	/*
	 * Helper to find regions at the given location
	 */
	protected abstract List<String> findRegions(Location location);

	/*
	 * Helper to convert a single element to a list
	 */
	protected final List<String> toList(String element) {
		return element == null ? new ArrayList<>() : Collections.singletonList(element);
	}
}
