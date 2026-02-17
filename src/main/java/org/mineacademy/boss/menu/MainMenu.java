package org.mineacademy.boss.menu;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.SelectRegionMenu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

/**
 * The main Boss plugin menu.
 */
public final class MainMenu extends Menu {

	private final Button regionsButton;
	private final Button bossesButton;
	private final Button toolsButton;
	private final Button spawningButton;
	private final Button spawnedBossesButton;

	@Override
	public ItemStack getItemAt(int slot) {

		if (Settings.REGISTER_TOOLS && Settings.REGISTER_REGIONS) {
			if (Arrays.asList(0, 9, 18, 27, 36, 45, 8, 17, 26, 35, 44, 53, 1, 7, 46, 52).contains(slot))
				return ItemCreator.from(CompMaterial.ORANGE_STAINED_GLASS_PANE, " ").make();

			if (slot == 9 * 1 + 4)
				return this.regionsButton.getItem();

			else if (slot == 9 * 2 + 2)
				return this.bossesButton.getItem();

			else if (slot == 9 * 2 + 6)
				return this.toolsButton.getItem();

			else if (slot == 9 * 3 + 4)
				return this.spawningButton.getItem();

			else if (slot == 9 * 4 + 4)
				return this.spawnedBossesButton.getItem();

		} else if (Settings.REGISTER_TOOLS) {

			if (slot == 9 + 1)
				return this.bossesButton.getItem();

			else if (slot == 9 + 3)
				return this.spawningButton.getItem();

			else if (slot == 9 + 5)
				return this.toolsButton.getItem();

			else if (slot == 9 + 7)
				return this.spawnedBossesButton.getItem();

			else
				return ItemCreator.from(CompMaterial.RED_STAINED_GLASS_PANE, " ").make();

		} else if (Settings.REGISTER_REGIONS) {

			if (slot == 9 + 1)
				return this.bossesButton.getItem();

			else if (slot == 9 + 3)
				return this.spawningButton.getItem();

			else if (slot == 9 + 5)
				return this.regionsButton.getItem();

			else if (slot == 9 + 7)
				return this.spawnedBossesButton.getItem();

			else
				return ItemCreator.from(CompMaterial.PURPLE_STAINED_GLASS_PANE, " ").make();

		} else {

			if (slot == 9 + 2)
				return this.bossesButton.getItem();

			else if (slot == 9 + 4)
				return this.spawningButton.getItem();

			else if (slot == 9 + 6)
				return this.spawnedBossesButton.getItem();

			else
				return ItemCreator.from(CompMaterial.MAGENTA_STAINED_GLASS_PANE, " ").make();
		}

		return NO_ITEM;
	}

	protected MainMenu() {

		this.setTitle(Lang.legacy("menu-main-title"));
		this.setSize(Settings.REGISTER_TOOLS && Settings.REGISTER_REGIONS ? 9 * 6 : 9 * 3);

		this.bossesButton = new ButtonMenu(new SelectBossMenu(this), CompMaterial.CHEST,
				Lang.legacy("menu-main-button-bosses"),
				Lang.legacy("menu-main-button-bosses-lore").split("\n"));

		this.regionsButton = new ButtonMenu(SelectRegionMenu.create(this), CompMaterial.ORANGE_DYE,
				Lang.legacy("menu-main-button-regions"),
				Lang.legacy("menu-main-button-regions-lore").split("\n"));

		this.toolsButton = Settings.REGISTER_TOOLS ? new ButtonMenu(new ToolsMenu(this), ItemCreator.from(CompMaterial.DIAMOND_AXE,
				Lang.legacy("menu-main-button-tools"),
				Lang.legacy("menu-main-button-tools-lore").split("\n")).glow(true))
				: Button.makeDummy(
						CompMaterial.DIAMOND_AXE,
						Lang.legacy("menu-main-button-tools-disabled"),
						Lang.legacy("menu-main-button-tools-disabled-lore").split("\n"));

		this.spawningButton = new ButtonMenu(new SelectSpawnRuleMenu(this), CompMaterial.REDSTONE,
				Lang.legacy("menu-main-button-spawning"),
				Lang.legacy("menu-main-button-spawning-lore").split("\n"));

		final int spawnedCount = Boss.findBossesAlive().size();

		this.spawnedBossesButton = new ButtonMenu(new SpawnedBossesMenu(this), CompMaterial.ENDER_EYE,
				Lang.legacy("menu-main-button-spawned-bosses"),
				Lang.legacy("menu-main-button-spawned-bosses-lore", "count", spawnedCount).split("\n"));
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
