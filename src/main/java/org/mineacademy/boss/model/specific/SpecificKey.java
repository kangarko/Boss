package org.mineacademy.boss.model.specific;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossSpecificSetting;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.ItemCreator.ItemCreatorBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

public abstract class SpecificKey<T> {

	@NonNull
	private BossSpecificSetting setting;

	@NonNull
	@Getter(value = AccessLevel.PROTECTED)
	private Boss boss;

	// --------------------------------------------------------------------------------
	// Implementation
	// --------------------------------------------------------------------------------

	public void onSpawn(LivingEntity en) {
	}

	protected abstract void onIconClick(Player pl, Menu menu, ClickType click, T oldValue);

	protected Object getApplicable() {
		return LivingEntity.class;
	}

	protected abstract ItemCreator.ItemCreatorBuilder getIcon();

	protected ItemCreatorBuilder postProcessIcon(ItemCreatorBuilder icon) {
		if (get() != getDefault())
			icon.glow(true);

		return icon;
	}

	public abstract T getDefault();

	public boolean isEnabled() {
		return true;
	}

	// --------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------

	protected final void update(Object newValue) {
		boss.getSettings().setSpecificSetting(setting, newValue);
	}

	protected final T get() {
		return (T) boss.getSettings().getSpecificSetting(setting);
	}

	// --------------------------------------------------------------------------------
	// Outside-from methods
	// --------------------------------------------------------------------------------

	public final void setData(BossSpecificSetting setting, Boss boss) {
		this.setting = setting;
		this.boss = boss;
	}

	public final Button getButton() {
		return new Button() {

			@Override
			public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
				onIconClick(pl, menu, click, (T) boss.getSettings().getSpecificSetting(setting));

				menu.restartMenu();
			}

			@Override
			public ItemStack getItem() {
				final ItemCreator.ItemCreatorBuilder icon = getIcon();

				return postProcessIcon(icon).build().make();
			}
		};
	}

	public final boolean matches(EntityType type) {
		Valid.checkNotNull(type, "type = null");

		return isEnabled() && matches0(type, getApplicable());
	}

	private final boolean matches0(EntityType type, Object target) {
		if (target instanceof EntityType)
			return (EntityType) target == type;

		if (target instanceof Class<?>) {
			Valid.checkNotNull(type.getEntityClass(), "EntityType." + type + " lacks entity class! (this fault is from Bukkit!)");

			return ((Class<?>) target).isAssignableFrom(type.getEntityClass());
		}

		if (target instanceof List) {
			for (final Object item : (List<?>) target)
				if (matches0(type, item))
					return true;

			return false;
		}

		throw new FoException("Dead end matching " + type + " (matches " + target + ")");
	}
}
