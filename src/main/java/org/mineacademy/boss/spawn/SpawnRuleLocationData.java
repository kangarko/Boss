package org.mineacademy.boss.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.menu.util.SpawnRuleLocationsMenu;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Rule to spawn mobs at a given time on certain blocks.
 */
@Getter
public abstract class SpawnRuleLocationData extends SpawnRuleDateExact {

	/**
	 * Where to spawn Bosses
	 */
	private Set<String> locations;

	/**
	 * Should we randomize the location a bit?
	 */
	private boolean offset;

	protected SpawnRuleLocationData(String name, SpawnRuleType type) {
		super(name, type);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRuleDateExact#onLoad()
	 */
	@Override
	protected void onLoad() {
		super.onLoad();

		this.locations = this.getSet("Locations", String.class);
		this.offset = this.getBoolean("Offset", false);
	}

	@Override
	public void onSave() {
		this.set("Locations", this.locations);
		this.set("Offset", this.offset);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Logic */
	/* ------------------------------------------------------------------------------- */

	@Override
	public void onTick(SpawnData data) {

		if (!this.canRun())
			return;

		final List<String> locations = new ArrayList<>(this.locations);

		// Randomize
		Collections.shuffle(locations);

		for (final String locationName : locations) {
			final BossLocation bossLocation = BossLocation.findLocation(locationName);

			// Probably removed
			if (bossLocation == null)
				continue;

			Location location;

			if (this.offset)
				location = this.findSuitableOffsetLocation(bossLocation.getLocation());
			else
				location = bossLocation.getLocation().add(0.5 + Math.random() * (RandomUtil.nextBoolean() ? -1 : 1) / 10, 1, 0.5 + Math.random() * (RandomUtil.nextBoolean() ? -1 : 1) / 2);

			final int nearbyBlocksThreshold = Settings.Spawning.LOCATION_SPAWN_NEARBY_PLAYER_RADIUS;

			if (nearbyBlocksThreshold != -1 && !(this instanceof SpawnRuleRespawn)) { // Always spawn if we are starting delay after death
				boolean playersNearby = false;

				for (final Player online : location.getWorld().getPlayers())
					if (online.getWorld().equals(location.getWorld()) && online.getLocation().distance(location) <= nearbyBlocksThreshold)
						playersNearby = true;

				if (!playersNearby) {
					Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to players not in nearby radius (" + nearbyBlocksThreshold + " blocks)");

					continue;
				}
			}

			if (this.canRun(location))
				this.spawn(location, data);
		}
	}

	private Location findSuitableOffsetLocation(Location clone) {

		for (int i = 0; i < 15; i++) {
			final int randomXoffset = RandomUtil.nextIntBetween(-8, +8);
			final int randomYoffset = RandomUtil.nextIntBetween(-2, +2);
			final int randomZoffset = RandomUtil.nextIntBetween(-8, +8);

			final Location newLocation = clone.clone().add(randomXoffset, randomYoffset, randomZoffset);
			final Block newBlock = newLocation.getBlock();
			final Block above = newBlock.getRelative(BlockFace.UP);

			if (CompMaterial.isAir(newBlock) && CompMaterial.isAir(above))
				return newLocation;
		}

		return clone;
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		buttons.add(SpawnRuleLocationsMenu.createButton(parent, this.locations, this::setLocations, this instanceof SpawnRuleRespawn));

		buttons.add(new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = SpawnRuleLocationData.this.offset;
				SpawnRuleLocationData.this.setOffset(!has);

				menu.restartMenu(has ? "&4Location no longer randomized" : "&2Location is now randomized");
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.from(
						CompMaterial.SLIME_BLOCK,
						"&6Location Offset",
						"",
						"Status: " + (SpawnRuleLocationData.this.offset ? "&aEnabled" : "&cDisabled"),
						"",
						"&aEnabled&7 = We randomize spawning",
						"location each time. ",
						"",
						"&cDisabled&7 = We spawn your Boss",
						"exactly at the location point.",
						"",
						"&cWarning: Can pose a performance",
						"&cpenalty and show in spark profiler.")
						.glow(SpawnRuleLocationData.this.offset)
						.make();
			}
		});

		return buttons;
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Set locations
	 *
	 * @param locations the locations to set
	 */
	public void setLocations(Set<String> locations) {
		this.locations = locations;

		this.save();
	}

	/**
	 * Set random offset to spawn location
	 *
	 * @param offset
	 */
	public void setOffset(boolean offset) {
		this.offset = offset;

		this.save();
	}
}
