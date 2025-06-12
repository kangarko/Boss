package org.mineacademy.boss.custom;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.settings.Lang;

/**
 * A simple custom setting taking a boolean value,
 * which automatically makes the value toggleable in the menu.
 */
public abstract class CustomBooleanSetting extends CustomSetting<Boolean> {

	/**
	 * Created a new custom setting.
	 *
	 * @param key
	 */
	protected CustomBooleanSetting(String key) {
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
		final boolean enabled = this.getValue();

		this.save(boss, !enabled);
		menu.restartMenu(Lang.legacy(enabled ? "part-disabled" : "part-enabled") + " " + ChatUtil.capitalizeFully(this.getKey()));
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#toMenuItem()
	 */
	@Override
	public ItemStack toMenuItem() {
		return this.getIcon().glow(this.getValue()).makeMenuTool();
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#getDefault()
	 */
	@Override
	public Boolean getDefault() {
		return false;
	}
}
