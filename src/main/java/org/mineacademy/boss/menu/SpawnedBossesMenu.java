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

/**
 * Menu showing all currently alive Bosses with teleport and kill actions.
 */
public final class SpawnedBossesMenu extends MenuPaged<SpawnedBoss> {

	protected SpawnedBossesMenu(Menu parent) {
		super(parent, Boss.findBossesAlive());

		this.setTitle("&0Spawned Bosses");
	}

	@Override
	protected ItemStack convertToItemStack(SpawnedBoss spawned) {
		final LivingEntity entity = spawned.getEntity();
		final Boss boss = spawned.getBoss();

		if (entity.isDead() || !entity.isValid())
			return ItemCreator.from(CompMaterial.BARRIER, "&c" + boss.getName(),
					"&4Boss is no longer alive!")
					.makeMenuTool();

		final int health    = Remain.getHealth(entity);
		final int maxHealth = Remain.getMaxHealth(entity);
		final int percent   = maxHealth > 0 ? (int) ((double) health / maxHealth * 100) : 0;

		return ItemCreator.fromItemStack(boss.getEgg())
				.name(boss.getName())
				.clearLore()
				.lore("",
						"&7Health: &f" + health + "&7/&f" + maxHealth + " &8(" + percent + "%)",
						"",
						"&7World: &f" + entity.getWorld().getName(),
						"&7Location: &f" + entity.getLocation().getBlockX() + ", " + entity.getLocation().getBlockY() + ", " + entity.getLocation().getBlockZ(),
						"",
						"&eLeft-click: &7teleport to Boss",
						"&cRight-click: &7kill Boss")
				.makeMenuTool();
	}

	@Override
	protected void onPageClick(Player player, SpawnedBoss spawned, ClickType click) {
		final LivingEntity entity = spawned.getEntity();

		if (entity.isDead() || !entity.isValid()) {
			this.restartMenu("&4Boss is no longer alive!");

			return;
		}

		if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
			player.teleport(entity);

			this.restartMenu("&7Teleported to Boss &6" + spawned.getName() + "&7.");

		} else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
			Remain.removeEntityWithPassengersAndNPC(entity);

			this.restartMenu("&2Killed " + spawned.getName());
		}
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"This menu lists all currently",
				"spawned Bosses in the server.",
				"",
				"&8Bosses from 'Replace Vanilla'",
				"&8spawn rules are excluded."
		};
	}

	@Override
	public Menu newInstance() {
		return new SpawnedBossesMenu(this.getParent());
	}
}
