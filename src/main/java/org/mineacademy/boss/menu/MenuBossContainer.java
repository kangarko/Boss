package org.mineacademy.boss.menu;

import java.util.Collections;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.model.ItemCreator;

public final class MenuBossContainer extends MenuPagged<Boss> {

	MenuBossContainer() {
		super(new MenuMain(), BossPlugin.getBossManager().getBosses());

		setTitle("Select a Boss");
	}

	@Override
	protected ItemStack convertToItemStack(final Boss boss) {
		Valid.checkNotNull(boss);

		return ItemCreator
				.of(boss.asEgg())
				.name("&r" + boss.getName())
				.lores(Collections.singletonList(
						"&7" + ItemUtil.bountifyCapitalized(boss.getType())))
				.build().make();
	}

	@Override
	protected void onPageClick(final Player pl, final Boss boss, final ClickType click) {
		new MenuBossIndividual(boss).displayTo(pl);
	}

	@Override
	protected String[] getInfo() {
		return null;
	}
}
