package org.mineacademy.boss.model.specific;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator.ItemCreatorBuilder;

public abstract class IntegerSpecificKey extends SpecificKey<Integer> {

	@Override
	protected final void onIconClick(Player pl, Menu menu, ClickType click, Integer oldLevel) {
		final int newLevel = MathUtil.range(oldLevel + (click == ClickType.LEFT ? -1 : 1), getMinimum(), getMaximum());

		update(newLevel);
	}

	@Override
	protected ItemCreatorBuilder postProcessIcon(ItemCreatorBuilder icon) {
		final int quantity = get();
		final boolean longName = getBoss().getName().length() > 15;

		return icon.lores(Arrays.asList(
				"",
				"&7Value: &f" + quantity,
				"",
				(longName ? "  " : "") + " &8(Mouse click)",
				(longName ? " " : "") + " &7&l< &4-1    &2+1 &7&l>"));
	}

	@Override
	public Integer getDefault() {
		return 1;
	}

	public int getMinimum() {
		return 0;
	}

	public int getMaximum() {
		return 50;
	}
}
