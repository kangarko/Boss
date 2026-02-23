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

		this.setTitle("Create Or Edit Bosses");

		this.spawnedButton = new ButtonMenu(
				() -> new SpawnedBossesMenu(SelectBossMenu.this),
				ItemCreator.from(CompMaterial.ENDER_EYE,
						"&dSpawned Bosses",
						"",
						"Currently alive: &f" + Boss.findBossesAlive().size(),
						"",
						"Click to view all",
						"alive Bosses and",
						"teleport or kill them."));

		this.createButton = new ButtonMenu(new CreateMenu(this), CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new Boss.");
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
				.lore("",
						"Click to open this Boss",
						"menu and customize it.")
				.makeMenuTool();
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Select a Boss to open its",
				"menu and customize it.",
				"",
				"Create a new Boss by",
				"clicking on the emerald."
		};
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

			this.setTitle("Select Boss Type");
		}

		@Override
		protected ItemStack convertToItemStack(EntityType type) {

			if (type == CompEntityType.PLAYER)
				return ItemCreator.from(
						CompMaterial.PLAYER_HEAD,
						"Player NPC",
						"",
						"Click to create a new",
						"NPC using Citizens.").make();

			return ItemCreator.fromMonsterEgg(type,
					ChatUtil.capitalizeFully(type),
					"",
					"Click to create a new",
					"Boss from this mob.").make();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"To create a new Boss, simply",
					"select what kind of mob the",
					"Boss will be created from.",
					"",
					"Minecraft server does not allow",
					"creation of completely new mob",
					"types without forcing all players",
					"to download a mod, that is why we",
					"create new Bosses from mobs that",
					"your client can render and customize",
					"them later in your Boss settings."
			};
		}

		@Override
		protected void onPageClick(Player player, EntityType type, ClickType click) {
			CreateBossPrompt.showTo(player, type);
		}
	}
}