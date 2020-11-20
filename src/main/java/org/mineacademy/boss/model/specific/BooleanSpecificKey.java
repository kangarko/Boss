package org.mineacademy.boss.model.specific;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.menu.Menu;

public abstract class BooleanSpecificKey extends SpecificKey<Boolean> {

	@Override
	protected final void onIconClick(Player pl, Menu menu, ClickType click, Boolean oldValue) {
		update(!oldValue);
	}

	@Override
	public Boolean getDefault() {
		return false;
	}
}
