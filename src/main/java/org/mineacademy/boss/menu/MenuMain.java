package org.mineacademy.boss.menu;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

public final class MenuMain extends Menu {

	private final Button bossesButton;
	private final Button newButton;
	private final Button toolsButton;
	private final Button reloadButton;

	public MenuMain() {
		super(null);

		setSize(9 * 5);
		setTitle("Boss Menu");

		// This is a wrapper to easily send the player to another menu
		bossesButton = new ButtonMenu(MenuBossContainer::new,
				ItemCreator
						.of(CompMaterial.CHEST,
								"&6Bosses",
								"",
								"Browse created Bosses.")
						.glow(true));

		// This also sends the player to another menu but you can do additional stuff on clicking the button
		newButton = new Button() {

			@Override
			public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
				new MenuNewContainer().displayTo(pl);
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator
						.of(CompMaterial.ORANGE_DYE,
								"&6Create New",
								"",
								"Make a new Boss.")
						.build().make();
			}
		};

		toolsButton = new ButtonMenu(new MenuToolsBoss(this) {

			{
				setSize(9 * 3);
			}

			@Override
			protected int getInfoButtonPosition() {
				return getSize() - 9;
			}
		},
				ItemCreator
						.of(CompMaterial.DIAMOND_AXE,
								"&3Tools",
								"",
								"Open the tools menu",
								"that includes region",
								"selection and Boss",
								"Spawner tool.")
						.glow(true));

		reloadButton = new Button() {

			@Override
			public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
				try {
					SimplePlugin.getInstance().reload();
					animateTitle(SimplePlugin.getNamed() + " has been reloaded");

				} catch (final Throwable t) {
					animateTitle("&4Error reloading, see console");
				}
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator
						.of(CompMaterial.COMPARATOR,
								"Reload the Plugin",
								"",
								"Reload the configuration",
								"and loaded bosses.",
								"",
								"For the best stability, we",
								"advice that you restart",
								"your server instead.")
						.build().make();
			}
		};
	}

	@Override
	public ItemStack getItemAt(final int slot) {

		if (Arrays.asList(0, 9, 18, 27, 36, 8, 17, 26, 35, 44, 1, 7, 37, 43).contains(slot))
			return ItemCreator.of(CompMaterial.ORANGE_STAINED_GLASS_PANE, "").build().make();

		if (slot == 9 * 2 + 2)
			return bossesButton.getItem();

		if (slot == 9 + 4)
			return newButton.getItem();

		if (slot == 9 * 2 + 6)
			return toolsButton.getItem();

		if (slot == 9 * 3 + 4)
			return reloadButton.getItem();

		return null;
	}

	@Override
	public void onMenuClick(final Player pl, final int slot, final ItemStack clicked) {
	}

	@Override
	protected String[] getInfo() {
		return null;
	}

	@Override
	protected boolean addReturnButton() {
		return false;
	}
}
