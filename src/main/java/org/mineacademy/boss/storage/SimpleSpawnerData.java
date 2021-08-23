package org.mineacademy.boss.storage;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.tool.SpawnerTool;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlSectionConfig;

import lombok.Getter;

@Getter
public final class SimpleSpawnerData extends YamlSectionConfig {

	private final static SimpleSpawnerData instance = new SimpleSpawnerData();

	public static SimpleSpawnerData $() {
		return instance;
	}

	// Location of the spawner, Boss name
	private final StrictMap<Location, String> spawners = new StrictMap<>();

	public SimpleSpawnerData() {
		super("Spawners");

		loadConfiguration(FoConstants.File.DATA);
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return false;
	}

	@Override
	protected void onLoadFinish() {
		Debugger.debug("spawner", "Loading spawner data.. ");

		spawners.clear();

		if (isSet("Converted"))
			for (final String spawnerLine : getStringList("Stored")) {
				final String[] spawnerData = spawnerLine.split("\\: ");
				Valid.checkBoolean(spawnerData.length == 2, "Spawner data must be 2 of length when loading from cache: " + spawnerLine);

				final Location location = SerializeUtil.deserializeLocation(spawnerData[0]);
				final String bossName = spawnerData[1];

				Valid.checkNotNull(location, "Location cannot be null when loading from cache: " + spawnerLine);
				Valid.checkNotNull(bossName, "Boss name cannot be null when loading from cache: " + spawnerLine);

				try {
					final Block block = location.getBlock();
					final Boss boss = BossPlugin.getBossManager().findBoss(bossName);

					if ((bossName.equals("unspecified") || boss != null) && block != null && block.getType() == CompMaterial.SPAWNER.getMaterial()) {
						Debugger.debug("spawner", "\tLoaded Boss spawner at " + Common.shortLocation(location) + " spawning " + bossName);

						block.setMetadata(SpawnerTool.METADATA, new FixedMetadataValue(SimplePlugin.getInstance(), bossName));

						spawners.put(location, bossName);
					} else
						Debugger.debug("spawner", "\tRemoving obsolete Boss spawner data at " + Common.shortLocation(location));

				} catch (final Throwable t) {
					Common.error(t, "Problem loading Boss spawner from line: " + spawnerLine);
				}
			}
		else
			setNoSave("Converted", true);

		setNoSave("Stored", Common.convertToList(spawners.getSource(), (key, value) -> SerializeUtil.serializeLoc(key) + ": " + value));
	}

	public boolean hasSpawner(Location loc) {
		return spawners.contains(loc);
	}

	public String getSpawner(Location loc) {
		return spawners.get(loc);
	}

	public void addSpawner(Location loc, String bossName) {
		Debugger.debug("spawner", "Adding spawner data at " + Common.shortLocation(loc) + " for Boss '" + bossName + "'");
		spawners.override(loc, bossName);

		update();
	}

	public void removeSpawner(Location loc) {
		Debugger.debug("spawner", "Removing spawner data at " + Common.shortLocation(loc));
		spawners.remove(loc);

		update();
	}

	private void update() {
		save("Stored", Common.convertToList(spawners.getSource(), (key, value) -> SerializeUtil.serializeLoc(key) + ": " + value));

		onLoadFinish();
	}
}
