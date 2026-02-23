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
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * Utility menu used for some spawn rules to select locations.
 */
public final class SpawnRuleLocationsMenu extends MenuPaged<String> {

	private final Set<String> oldLocations;
	private final Consumer<Set<String>> setter;

	private SpawnRuleLocationsMenu(Menu parent, Set<String> oldLocations, Consumer<Set<String>> setter) {
		super(parent, BossLocation.getLocationsNames(), true);

		this.setTitle(Lang.legacy("menu-spawn-rule-locations-title"));

		this.oldLocations = oldLocations;
		this.setter = setter;
	}

	@Override
	protected ItemStack convertToItemStack(String locationName) {
		final boolean has = this.oldLocations.contains(locationName);

		return ItemCreator.from(
				CompMaterial.PAPER,
				Lang.legacy("menu-spawn-rule-locations-item", "location", locationName),
				Lang.legacy("menu-spawn-rule-locations-item-lore", "status", has ? Lang.legacy("menu-spawn-rule-locations-spawns") : Lang.legacy("menu-spawn-rule-locations-no-spawns")).split("\n"))
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
		this.restartMenu(has ? Lang.legacy("menu-spawn-rule-locations-disabled", "location", locationName) : Lang.legacy("menu-spawn-rule-locations-enabled", "location", locationName));
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-spawn-rule-locations-info").split("\n");
	}

	public static Button createButton(Menu parent, Set<String> locations, Consumer<Set<String>> setter, boolean respawn) {
		return new ButtonMenu(new SpawnRuleLocationsMenu(parent, locations, setter),
				CompMaterial.MAP,
				Lang.legacy("menu-spawn-rule-locations-button"),
				Lang.legacy("menu-spawn-rule-locations-button-lore",
						"selected", locations.size(),
						"total", BossLocation.getLocations().size(),
						"respawn_notice", respawn ? Lang.legacy("menu-spawn-rule-locations-button-respawn") : "",
						"label", SimpleSettings.MAIN_COMMAND_ALIASES.get(0)).split("\n"));
	}
}