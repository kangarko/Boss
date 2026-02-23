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

		this.setTitle(Lang.legacy("menu-boss-title", "boss", boss.getName()));
		this.setSize(9 * 4);

		this.settingsButton = new ButtonMenu(new SettingsMenu(this, boss),
				CompMaterial.CHEST,
				Lang.legacy("menu-boss-button-settings"),
				Lang.legacy("menu-boss-button-settings-lore").split("\n"));

		this.skillsButton = new ButtonMenu(new SkillsMenu(this, boss),
				CompMaterial.ENDER_EYE,
				Lang.legacy("menu-boss-button-skills"),
				Lang.legacy("menu-boss-button-skills-lore").split("\n"));

		this.deathButton = new ButtonMenu(new DeathMenu(this, boss),
				CompMaterial.BONE,
				Lang.legacy("menu-boss-button-death"),
				Lang.legacy("menu-boss-button-death-lore").split("\n"));

		this.spawningButton = new ButtonMenu(new SpawningMenu(this, boss),
				CompMaterial.OAK_SAPLING,
				Lang.legacy("menu-boss-button-spawning"),
				Lang.legacy("menu-boss-button-spawning-lore").split("\n"));

		this.eggButton = Button.makeSimple(ItemCreator.fromItemStack(boss.getEgg()).clearLore()
				.name(Lang.legacy("menu-boss-button-egg"))
				.lore(Lang.legacy("menu-boss-button-egg-lore",
						"permission_egg", Permissions.Use.SPAWNER_EGG,
						"permission_spawn", Permissions.Spawn.BOSS.replace("{boss}", boss.getName())).split("\n")),
				click -> {
					this.getViewer().getInventory().addItem(boss.getEgg());
				});

		this.butcherButton = Button.makeSimple(ItemCreator.from(
				CompMaterial.RED_DYE,
				Lang.legacy("menu-boss-button-butcher"),
				Lang.legacy("menu-boss-button-butcher-lore", "label", SimpleSettings.MAIN_COMMAND_ALIASES.get(0)).split("\n")),
				player -> {
					final int count = Boss.killAliveBosses(boss);

					this.animateTitle(Lang.legacy("menu-boss-button-butcher-success", "amount", Lang.numberFormat("case-boss", count)));
				});

		final String bossBame = boss.getName();

		this.removeButton = new ButtonRemove(this, "boss", bossBame, () -> {
			if (Boss.isBossLoaded(bossBame)) {

				Boss.removeBoss(boss);

				final Menu nextMenu = Boss.getBosses().isEmpty() ? MainMenu.create() : SelectBossMenu.create();

				nextMenu.displayTo(this.getViewer());
				Platform.runTask(2, () -> nextMenu.animateTitle(Lang.legacy("menu-boss-removed", "boss", boss.getName())));

			} else
				this.animateTitle(Lang.legacy("menu-boss-not-loaded"));
		});
	}

	@Override
	public Menu newInstance() {
		return new BossMenu(this.getParent(), this.boss);
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-boss-info").split("\n");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static void showTo(Menu parent, Player player, Boss boss) {
		for (final Player online : Remain.getOnlinePlayers()) {
			if (online.equals(player))
				continue;

			Menu otherMenu = Menu.getMenu(online);

			if (otherMenu != null) {
				while (!(otherMenu instanceof BossMenu) && otherMenu != null)
					otherMenu = otherMenu.getParent();

				if (otherMenu instanceof BossMenu && ((BossMenu) otherMenu).boss.equals(boss)) {
					final Menu playerMenu = Menu.getMenu(player);

					if (playerMenu != null)
						playerMenu.animateTitle(Lang.legacy("menu-boss-browsing-warning", "player", online.getName()));
					else
						Messenger.error(player, Lang.legacy("menu-boss-browsing-error", "player", online.getName()));

					return;
				}
			}
		}

		new BossMenu(parent, boss).displayTo(player);
	}
}
