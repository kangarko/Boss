package org.mineacademy.boss.menu.boss;

import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.MainMenu;
import org.mineacademy.boss.menu.SelectBossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.Permissions;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * The main Boss menu.
 */
public final class BossMenu extends Menu {

	private final Boss boss;

	@Position(9 + 1)
	private final Button settingsButton;

	@Position(9 + 3)
	private final Button skillsButton;

	@Position(9 + 5)
	private final Button deathButton;

	@Position(9 + 7)
	private final Button spawningButton;

	@Position(start = StartPosition.BOTTOM_CENTER, value = -1)
	private final Button eggButton;

	@Position(start = StartPosition.BOTTOM_CENTER)
	private final Button butcherButton;

	@Position(start = StartPosition.BOTTOM_CENTER, value = +1)
	private final Button removeButton;

	private BossMenu(Menu parent, Boss boss) {
		super(parent);

		this.boss = boss;

		this.setTitle("Boss " + boss.getName());
		this.setSize(9 * 4);

		this.settingsButton = new ButtonMenu(new SettingsMenu(this, boss),
				CompMaterial.CHEST,
				"&6Settings",
				"",
				"Edit how this Boss behaves",
				"or looks like, when spawned.");

		this.skillsButton = new ButtonMenu(new SkillsMenu(this, boss),
				CompMaterial.ENDER_EYE,
				"&bSkills",
				"",
				"Give your Boss special",
				"functions or abilities.");

		this.deathButton = new ButtonMenu(new DeathMenu(this, boss),
				CompMaterial.BONE,
				"Death",
				"",
				"Edit what happens when",
				"your Boss dies.");

		this.spawningButton = new ButtonMenu(new SpawningMenu(this, boss),
				CompMaterial.OAK_SAPLING,
				"&aSpawning",
				"",
				"Select how and when",
				"to spawn this Boss.");

		this.eggButton = Button.makeSimple(ItemCreator.fromItemStack(boss.getEgg()).clearLore()
				.name("Get Spawner Egg")
				.lore(
						"",
						"Receive a Boss egg you",
						"can use to spawn the Boss.",
						"",
						"&cTip: &7Give players permissions",
						"below to use this spawner egg:",
						"&c&o" + Permissions.Use.SPAWNER_EGG,
						"&c&o" + Permissions.Spawn.BOSS.replace("{boss}", boss.getName())),
				click -> {
					this.getViewer().getInventory().addItem(boss.getEgg());
				});

		this.butcherButton = Button.makeSimple(ItemCreator.from(
				CompMaterial.RED_DYE,
				"&cKill Bosses",
				"Remove alive Bosses in all",
				"worlds in loaded chunks.",
				"",
				"&cTip: &7Use /" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " butcher\ncommand for more options."),
				player -> {
					final int count = Boss.killAliveBosses(boss);

					this.animateTitle("&4Killed " + Lang.numberFormat("case-boss", count));
				});

		final String bossBame = boss.getName();

		this.removeButton = new ButtonRemove(this, "boss", bossBame, () -> {
			if (Boss.isBossLoaded(bossBame)) {

				// Remove the boss from disk
				Boss.removeBoss(boss);

				// Show parent menu for convenience
				final Menu nextMenu = Boss.getBosses().isEmpty() ? MainMenu.create() : SelectBossMenu.create();

				nextMenu.displayTo(this.getViewer());
				Platform.runTask(2, () -> nextMenu.animateTitle("&4Removed Boss " + boss.getName()));

			} else
				this.animateTitle("&4Boss not loaded!");
		});
	}

	@Override
	public Menu newInstance() {
		return new BossMenu(this.getParent(), this.boss);
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"This the main Boss' menu.",
				"Edit how the Boss looks",
				"like or behaves here.",
				"",
				"Your Boss on your worlds",
				"will update automatically."
		};
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static void showTo(Menu parent, Player player, Boss boss) {
		for (final Player online : Remain.getOnlinePlayers()) {
			Menu otherMenu = Menu.getMenu(online);

			if (otherMenu != null) {
				while (!(otherMenu instanceof BossMenu) && otherMenu != null)
					otherMenu = otherMenu.getParent();

				if (otherMenu instanceof BossMenu && ((BossMenu) otherMenu).boss.equals(boss)) {
					final Menu playerMenu = Menu.getMenu(player);

					if (playerMenu != null)
						playerMenu.animateTitle("&4" + online.getName() + " is browsing this!");
					else
						Messenger.error(player, "Cannot open Boss menu while " + online.getName() + " is browsing it!");

					return;
				}
			}
		}

		new BossMenu(parent, boss).displayTo(player);
	}
}
