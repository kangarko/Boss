package org.mineacademy.boss.menu;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.SpawnedBoss;
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

		this.setTitle(Lang.legacy("menu-spawned-bosses-title"));
	}

	@Override
	protected ItemStack convertToItemStack(SpawnedBoss spawned) {
		final LivingEntity entity = spawned.getEntity();
		final Boss boss = spawned.getBoss();

		if (entity.isDead() || !entity.isValid())
			return ItemCreator.from(CompMaterial.BARRIER, "&c" + boss.getName(),
					Lang.legacy("menu-spawned-bosses-not-alive"))
					.makeMenuTool();

		final int health    = Remain.getHealth(entity);
		final int maxHealth = Remain.getMaxHealth(entity);
		final int percent   = maxHealth > 0 ? (int) ((double) health / maxHealth * 100) : 0;

		return ItemCreator.fromItemStack(boss.getEgg())
				.name(boss.getName())
				.clearLore()
				.lore(Lang.legacy("menu-spawned-bosses-item-lore",
						"health", health,
						"max_health", maxHealth,
						"health_percent", percent,
						"world", entity.getWorld().getName(),
						"x", entity.getLocation().getBlockX(),
						"y", entity.getLocation().getBlockY(),
						"z", entity.getLocation().getBlockZ()).split("\n"))
				.makeMenuTool();
	}

	@Override
	protected void onPageClick(Player player, SpawnedBoss spawned, ClickType click) {
		final LivingEntity entity = spawned.getEntity();

		if (entity.isDead() || !entity.isValid()) {
			this.restartMenu(Lang.legacy("menu-spawned-bosses-not-alive"));

			return;
		}

		if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
			player.teleport(entity);

			this.restartMenu(Lang.legacy("menu-spawned-bosses-teleported", "boss", spawned.getName()));

		} else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
			Remain.removeEntityWithPassengersAndNPC(entity);

			this.restartMenu(Lang.legacy("menu-spawned-bosses-killed", "boss", spawned.getName()));
		}
	}

	@Override
	protected String[] getInfo() {
		return Lang.legacy("menu-spawned-bosses-info").split("\n");
	}

	@Override
	public Menu newInstance() {
		return new SpawnedBossesMenu(this.getParent());
	}
}
