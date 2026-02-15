package org.mineacademy.boss.menu;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * Menu showing all currently alive Bosses with teleport and kill actions.
 */
public final class SpawnedBossesMenu extends MenuPaged<SpawnedBoss> {

	protected SpawnedBossesMenu(Menu parent) {
		super(parent, Boss.findBossesAlive());

		this.setTitle(Lang.legacy("menu-spawned-title"));
	}

	@Override
	protected ItemStack convertToItemStack(SpawnedBoss spawned) {
		final LivingEntity entity = spawned.getEntity();
		final Boss boss = spawned.getBoss();

		if (entity.isDead() || !entity.isValid())
			return ItemCreator.from(CompMaterial.BARRIER)
					.name("&c" + boss.getName())
					.lore("", "&7This boss is no longer alive.")
					.makeMenuTool();

		return ItemCreator.fromItemStack(boss.getEgg())
				.name(boss.getName())
				.clearLore()
				.lore("",
						"&7Type: &f" + boss.getTypeFormatted(),
						"&7Health: &f" + Remain.getHealth(entity) + "&8/&f" + Remain.getMaxHealth(entity),
						"&7World: &f" + entity.getWorld().getName(),
						"&7Location: &f" + SerializeUtil.serializeLocation(entity.getLocation()),
						"",
						"&2Left-click &7to teleport.",
						"&4Right-click &7to kill.")
				.makeMenuTool();
	}

	@Override
	protected void onPageClick(Player player, SpawnedBoss spawned, ClickType click) {
		final LivingEntity entity = spawned.getEntity();

		if (entity.isDead() || !entity.isValid()) {
			this.restartMenu(Lang.legacy("command-invalid-boss-dead"));

			return;
		}

		if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
			player.teleport(entity);

			this.restartMenu("&aTeleported to " + spawned.getName());

		} else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
			Remain.removeEntityWithPassengersAndNPC(entity);

			this.restartMenu("&cKilled " + spawned.getName());
		}
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-spawned-info").split("\n");
	}

	@Override
	public Menu newInstance() {
		return new SpawnedBossesMenu(this.getParent());
	}
}
