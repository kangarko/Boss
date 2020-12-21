package org.mineacademy.boss.menu;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossConversation.PromptName;
import org.mineacademy.boss.model.BossManager;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class MenuNewContainer extends MenuPagged<EntityType> {

	public MenuNewContainer() {
		super(new MenuMain(), BossManager.getValidTypes());

		setTitle("Select Boss Type");
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Select what kind of monster",
				"this Boss should represent."
		};
	}

	@Override
	protected ItemStack convertToItemStack(final EntityType entity) {
		return ItemCreator.of(CompMaterial.makeMonsterEgg(entity)).name("&r" + ItemUtil.bountifyCapitalized(entity)).build().make();
	}

	@Override
	protected void onPageClick(final Player pl, final EntityType clicked, final ClickType click) {
		PromptName.show(clicked, pl);
	}

	@Override
	protected boolean updateButtonOnClick() {
		return false;
	}
}
