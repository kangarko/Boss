package org.mineacademy.boss.menu;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.InventoryDrawer;

abstract class IncrementalListMenu extends Menu {

	private final StrictList<String> list;

	protected IncrementalListMenu(final Menu parent, final StrictList<String> list) {
		super(parent);

		setSize(18 + 9 * (list.size() / 9));
		this.list = list;
	}

	@Override
	protected final List<Button> getButtonsToAutoRegister() {
		final List<Button> items = new ArrayList<>(getSize());

		for (int i = 0; i < list.size(); i++)
			items.add(getListButton(list.get(i), i));

		return items;
	}

	protected abstract Button getListButton(String listName, int listIndex);

	@Override
	protected final void onDisplay(final InventoryDrawer inv) {
		for (final Button item : getButtonsToAutoRegister())
			inv.pushItem(item.getItem());
	}

	@Override
	protected final String[] getInfo() {
		return null;
	}
}
