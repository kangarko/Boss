package org.mineacademy.boss.menu;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.SelectRegionMenu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * The main Boss plugin menu.
 */
public final class MainMenu extends Menu {

	// Buttons are dynamically positioned based on if regions are enabled
	private final Button regionsButton;
	private final Button bossesButton;
	private final Button toolsButton;
	private final Button spawningButton;

	@Override
	public ItemStack getItemAt(int slot) {

		if (Settings.REGISTER_TOOLS && Settings.REGISTER_REGIONS) {
			if (Arrays.asList(0, 9, 18, 27, 36, 8, 17, 26, 35, 44, 1, 7, 37, 43).contains(slot))
				return ItemCreator.from(CompMaterial.ORANGE_STAINED_GLASS_PANE, " ").make();

			if (slot == 9 * 1 + 4)
				return this.regionsButton.getItem();

			else if (slot == 9 * 2 + 2)
				return this.bossesButton.getItem();

			else if (slot == 9 * 2 + 6)
				return this.toolsButton.getItem();

			else if (slot == 9 * 3 + 4)
				return this.spawningButton.getItem();

		} else if (Settings.REGISTER_TOOLS) {

			if (slot == 9 + 2)
				return this.bossesButton.getItem();

			else if (slot == 9 + 4)
				return this.spawningButton.getItem();

			else if (slot == 9 + 6)
				return this.toolsButton.getItem();

			else
				return ItemCreator.from(CompMaterial.RED_STAINED_GLASS_PANE, " ").make();

		} else if (Settings.REGISTER_REGIONS) {

			if (slot == 9 + 2)
				return this.bossesButton.getItem();

			else if (slot == 9 + 4)
				return this.spawningButton.getItem();

			else if (slot == 9 + 6)
				return this.regionsButton.getItem();

			else
				return ItemCreator.from(CompMaterial.PURPLE_STAINED_GLASS_PANE, " ").make();

		} else {

			if (slot == 9 + 3)
				return this.bossesButton.getItem();

			else if (slot == 9 + 5)
				return this.spawningButton.getItem();

			else
				return ItemCreator.from(CompMaterial.MAGENTA_STAINED_GLASS_PANE, " ").make();
		}

		return NO_ITEM;
	}

	protected MainMenu() {

		this.setTitle("Boss™ Main Menu");
		this.setSize(Settings.REGISTER_TOOLS && Settings.REGISTER_REGIONS ? 9 * 5 : 9 * 3);

		this.bossesButton = new ButtonMenu(new SelectBossMenu(this), CompMaterial.CHEST,
				"&6Bosses",
				"",
				"Click to edit",
				"loaded Bosses",
				"or create them.");

		this.regionsButton = new ButtonMenu(SelectRegionMenu.create(this), CompMaterial.ORANGE_DYE,
				"&eRegions",
				"",
				"Click to manage",
				"Boss regions.");

		this.toolsButton = Settings.REGISTER_TOOLS ? new ButtonMenu(new ToolsMenu(this), ItemCreator.from(CompMaterial.DIAMOND_AXE,
				"&bTools",
				"",
				"Click to list useful",
				"Boss tools for regions",
				"and mob editing.").glow(true))
				: Button.makeDummy(
						CompMaterial.DIAMOND_AXE,
						"&cTools Disabled!",
						"",
						"To enable tools, set",
						"&6Register_Tools &7key in",
						"settings.yml to true.");

		this.spawningButton = new ButtonMenu(new SelectSpawnRuleMenu(this), CompMaterial.REDSTONE,
				"&cSpawning",
				"",
				"Click to manage",
				"Boss spawning.");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static Menu create() {
		return new MainMenu();
	}

	public static void showTo(Player player) {
		new MainMenu().displayTo(player);
	}
}
