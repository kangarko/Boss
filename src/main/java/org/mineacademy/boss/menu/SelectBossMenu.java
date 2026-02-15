package org.mineacademy.boss.menu;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.menu.boss.BossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.prompt.CreateBossPrompt;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

/**
 * The menu where players can select a Boss or create one.
 */
public final class SelectBossMenu extends MenuPaged<Boss> {

	@Position(start = StartPosition.BOTTOM_LEFT)
	private final Button spawnedButton;

	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	protected SelectBossMenu(Menu parent) {
		super(parent, Boss.getBosses());

		this.setTitle(Lang.legacy("menu-boss-select-title"));

		this.spawnedButton = new ButtonMenu(
				() -> new SpawnedBossesMenu(SelectBossMenu.this),
				ItemCreator.from(CompMaterial.ENDER_EYE,
						Lang.legacy("menu-spawned-button"),
						Lang.legacy("menu-spawned-button-lore").split("\n")));

		this.createButton = new ButtonMenu(new CreateMenu(this), CompMaterial.EMERALD,
				Lang.legacy("menu-boss-select-button-create"),
				Lang.legacy("menu-boss-select-button-create-lore").split("\n"));
	}

	@Override
	public Menu newInstance() {
		return new SelectBossMenu(this.getParent());
	}

	@Override
	protected ItemStack convertToItemStack(Boss boss) {
		return ItemCreator.fromItemStack(boss.getEgg())
				.name(boss.getName())
				.clearLore()
				.lore(Lang.legacy("menu-boss-select-item-lore").split("\n"))
				.makeMenuTool();
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-boss-select-info").split("\n");
	}

	@Override
	protected void onPageClick(Player player, Boss boss, ClickType click) {
		BossMenu.showTo(this, player, boss);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static Menu create() {
		return new SelectBossMenu(new MainMenu());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	final class CreateMenu extends MenuPaged<EntityType> {

		protected CreateMenu(Menu parent) {
			super(parent, Boss.getValidEntities());

			this.setTitle(Lang.legacy("menu-boss-create-title"));
		}

		@Override
		protected ItemStack convertToItemStack(EntityType type) {

			if (type == CompEntityType.PLAYER)
				return ItemCreator.from(
						CompMaterial.PLAYER_HEAD,
						Lang.legacy("menu-boss-create-npc-name"),
						Lang.legacy("menu-boss-create-npc-lore").split("\n")).make();

			return ItemCreator.fromMonsterEgg(type,
					ChatUtil.capitalizeFully(type),
					Lang.legacy("menu-boss-create-entity-lore").split("\n")).make();
		}

		@Override
		protected String[] getInfo() {
			return Lang.legacy("menu-boss-create-info").split("\n");
		}

		@Override
		protected void onPageClick(Player player, EntityType type, ClickType click) {
			CreateBossPrompt.showTo(player, type);
		}
	}
}