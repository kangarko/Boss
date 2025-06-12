package org.mineacademy.boss.menu.util;

import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * Utility menu used for some spawn rules to select locations.
 */
public final class SpawnRuleLocationsMenu extends MenuPaged<String> {

	private final Set<String> oldLocations;
	private final Consumer<Set<String>> setter;

	private SpawnRuleLocationsMenu(Menu parent, Set<String> oldLocations, Consumer<Set<String>> setter) {
		super(parent, BossLocation.getLocationsNames(), true);

		this.setTitle("Select Spawn Locations");

		this.oldLocations = oldLocations;
		this.setter = setter;
	}

	@Override
	protected ItemStack convertToItemStack(String locationName) {
		final boolean has = this.oldLocations.contains(locationName);

		return ItemCreator.from(
				CompMaterial.PAPER,
				locationName + " Location",
				"",
				"Status: " + (has ? "&aBoss spawns here" : "&cBoss doesn't spawn here"),
				"",
				"Click to toggle if",
				"the Boss will spawn",
				"in this location.")
				.glow(has)
				.make();
	}

	@Override
	protected void onPageClick(Player player, String locationName, ClickType click) {
		final boolean has = this.oldLocations.contains(locationName);

		if (has)
			this.oldLocations.remove(locationName);

		else
			this.oldLocations.add(locationName);

		this.setter.accept(this.oldLocations);
		this.restartMenu(has ? "&4Boss no longer spawns in " + locationName : "&2Boss now spawns in " + locationName);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"Select blocks where this rule",
				"will spawn each Boss. You can",
				"create new locations using tool",
				"via '&6/boss tools&7'.",
				"",
				"&6Tip: &7To save time, you can stop",
				"the server and write locations",
				"manually into your spawnrule yml",
				"file in the spawnrules/ folder."
		};
	}

	public static Button createButton(Menu parent, Set<String> locations, Consumer<Set<String>> setter, boolean respawn) {
		return new ButtonMenu(new SpawnRuleLocationsMenu(parent, locations, setter),
				CompMaterial.MAP,
				"Locations",
				"",
				"Selected: &f" + locations.size() + "&8/&f" + BossLocation.getLocations().size(),
				"",
				"Click to choose locations",
				"where to spawn Bosses",
				"you selected in this rule.",
				"",
				(respawn ? "&cWe choose 1 location randomly" : ""),
				"",
				"You can create locations",
				"using '/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " tools' command.");
	}
}