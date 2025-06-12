package org.mineacademy.boss.custom;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.settings.Lang;

/**
 * A simple custom setting taking a boolean value,
 * which automatically makes the value editable in the menu.
 */
public abstract class CustomIntegerSetting extends CustomSetting<Integer> {

	/**
	 * Created a new custom setting.
	 *
	 * @param key
	 */
	protected CustomIntegerSetting(String key) {
		super(key);
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#onSpawn(org.mineacademy.boss.model.Boss, org.bukkit.entity.LivingEntity)
	 */
	@Override
	public void onSpawn(Boss boss, LivingEntity entity) {
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#onMenuClick(org.mineacademy.boss.model.Boss, org.mineacademy.fo.menu.Menu, org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	public final void onMenuClick(Boss boss, Menu menu, Player player, ClickType clickType) {
		final int oldValue = this.getValue();
		final int newValue = MathUtil.range(oldValue + (clickType == ClickType.LEFT ? -1 : 1), this.getMinimum(), this.getMaximum());

		this.save(boss, newValue);
		menu.restartMenu(Lang.legacy("menu-set-to", "key", this.getKey(), "value", newValue));
	}

	/**
	 * Return the minimum value allowed to be set through menu.
	 *
	 * @return
	 */
	public abstract int getMinimum();

	/**
	 * Return the maximum value allowed to be set through menu.
	 *
	 * @return
	 */
	public abstract int getMaximum();

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#getDefault()
	 */
	@Override
	public Integer getDefault() {
		return 1;
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#toMenuItem()
	 */
	@Override
	public ItemStack toMenuItem() {
		return this.getIcon().lore(Lang.legacy("menu-button-integer", "value", this.getValue())).make();
	}
}
