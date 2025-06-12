package org.mineacademy.boss.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.mineacademy.boss.tool.LocationTool;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.visual.Visualizer;

import lombok.NonNull;

/**
 * The permanent server cache storing disk data.
 */
public final class BossLocation extends YamlConfig {

	/**
	 * The stored disk locations.
	 */
	private static final ConfigItems<BossLocation> loadedLocations = ConfigItems.fromFolder("locations", BossLocation.class, (Function<List<BossLocation>, List<BossLocation>>) list -> {
		Collections.sort(list, Comparator.comparing(loc -> loc.getFile() != null ? loc.getFileName() : "", String.CASE_INSENSITIVE_ORDER));

		return list;
	});

	/**
	 * The Bukkit location.
	 */
	private Location location;

	/**
	 * The task visualizing this location.
	 */
	private Task visualizedTask;

	/*
	 * Create a new location, used when loading from disk
	 */
	private BossLocation(String name) {
		this(name, null);
	}

	/*
	 * Create a new location, used when creating new
	 */
	private BossLocation(String name, @Nullable Location location) {
		this.location = location;

		this.setHeader(
				Common.configLine(),
				"This file stores a single location you can use in spawn rules.",
				"",
				"To create a location, right click a block holding Location Tool",
				"obtainable from /boss tools. To remove a location, right click",
				"the block again or stop your server and remove this file.",
				Common.configLine() + "\n");

		this.loadAndExtract(NO_DEFAULT, "locations/" + name + ".yml");
	}

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {

		// Only load if not created via command
		if (this.location != null) {
			this.save();

			return;
		}

		this.location = this.get("Position", Location.class);
	}

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#serialize()
	 */
	@Override
	protected void onSave() {
		this.set("Position", this.location);
	}

	/**
	 * Return clone of this location.
	 *
	 * @return the location
	 */
	@Nullable
	public Location getLocation() {
		return this.location.clone();
	}

	/**
	 * Visualize this location to the player.
	 *
	 * @param player
	 */
	public void visualize(Player player) {
		final Block block = this.location.getBlock();
		final LocationTool tool = LocationTool.getInstance();

		if (Visualizer.isVisualized(block))
			return;

		Visualizer.visualize(player, block, tool.getBlockMask(block, player), tool.getBlockName(block, player));

		this.visualizedTask = Platform.runTask(20 * 10, () -> {
			if (Visualizer.isVisualized(block))
				Visualizer.stopVisualizing(block);
		});
	}

	/**
	 * Stop visualizing this location.
	 */
	public void stopVisualizing() {
		final Block block = this.location.getBlock();

		if (Visualizer.isVisualized(block))
			Visualizer.stopVisualizing(block);

		if (this.visualizedTask != null) {
			this.visualizedTask.cancel();

			this.visualizedTask = null;
		}
	}

	public String getName() {
		return this.getFileName();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BossLocation && ((BossLocation) obj).location.equals(this.location);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @param name
	 * @param location
	 * @return
	 * @see ConfigItems#loadOrCreateItem(String)
	 */
	public static BossLocation createLocation(@NonNull final String name, @NonNull final Location location) {
		return loadedLocations.loadOrCreateItem(name, () -> new BossLocation(name, location));
	}

	/**
	 * @see ConfigItems#loadItems()
	 */
	public static void loadLocations() {
		loadedLocations.loadItems();
	}

	/**
	 * @param location
	 */
	public static void removeLocation(final BossLocation location) {
		location.stopVisualizing();

		loadedLocations.removeItem(location);
	}

	/**
	 * Return if the given Bukkit location is set by us.
	 *
	 * @param location
	 * @return
	 */
	public static boolean isLocationLoaded(final Location location) {
		return findLocation(location) != null;
	}

	/**
	 * @param name
	 * @return
	 * @see ConfigItems#isItemLoaded(String)
	 */
	public static boolean isLocationLoaded(final String name) {
		return loadedLocations.isItemLoaded(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public static BossLocation findLocation(@NonNull final String name) {
		return loadedLocations.findItem(name);
	}

	/**
	 * Return a boss location from the bukkit location
	 *
	 * @param location
	 * @return
	 */
	public static BossLocation findLocation(final Location location) {
		for (final BossLocation bossLocation : getLocations())
			if (Valid.locationEquals(bossLocation.getLocation(), location))
				return bossLocation;

		return null;
	}

	/**
	 * Return a list of Bukkit locations.
	 *
	 * @return
	 */
	public static List<Location> getBukkitLocations() {
		return Common.convertList(getLocations(), BossLocation::getLocation);
	}

	/**
	 * @return
	 * @see ConfigItems#getItems()
	 */
	public static Collection<BossLocation> getLocations() {
		return loadedLocations.getItems();
	}

	/**
	 * @return
	 * @see ConfigItems#getItemNames()
	 */
	public static List<String> getLocationsNames() {
		return loadedLocations.getItemNames();
	}
}
