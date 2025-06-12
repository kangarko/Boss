package org.mineacademy.boss.custom;

import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.Variables;

/**
 * A simple custom setting taking a boolean value,
 * which automatically makes the value editable in the menu.
 * @param <T>
 */
public abstract class CustomStringSetting<T> extends CustomSetting<String> {

	/**
	 * Created a new custom setting.
	 *
	 * @param key
	 */
	protected CustomStringSetting(String key) {
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

		new SimpleStringPrompt(Variables.builder().placeholderArray(
				"current", Common.getOrDefault(this.getValue(), "default"),
				"available", Common.join(this.getValidTypes())).replaceLegacy(this.getChangeQuestion())) {

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				for (final T valid : CustomStringSetting.this.getValidTypes())
					if (valid.toString().equalsIgnoreCase(input))
						return true;

				return false;
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return CustomStringSetting.this.getFailedValidationText(invalidInput);
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				CustomStringSetting.this.save(boss, input.toString());
			}

			@Override
			protected String getMenuAnimatedTitle() {
				return "&9" + CustomStringSetting.this.getKey().replace("_", " ") + " Updated!";
			}

		}.show(player);
	}

	/**
	 * Return the question shown in the menu when wanting to set the value of setting.
	 *
	 * @return
	 */
	protected abstract String getChangeQuestion();

	/**
	 * Return all valid options the player can type in chat when editing this setting.
	 *
	 * @return
	 */
	protected abstract List<T> getValidTypes();

	/**
	 * Return the error message when {@link #getValidTypes()} doesn't contain player's input.
	 *
	 * @param invalidInput
	 * @return
	 */
	protected String getFailedValidationText(String invalidInput) {
		return "Invalid input '" + invalidInput + "', available: " + Common.join(this.getValidTypes()) + ".";
	}

	/**
	 * @see org.mineacademy.boss.custom.CustomSetting#getDefault()
	 */
	@Override
	public String getDefault() {
		return null;
	}
}
