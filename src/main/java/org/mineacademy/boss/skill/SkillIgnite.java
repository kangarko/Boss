package org.mineacademy.boss.skill;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillIgnite extends AbstractTargetSkill {

	private SimpleTime duration;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("20 seconds - 50 seconds");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		target.setFireTicks(this.duration.getTimeTicks());

		this.sendSkillMessage(target, entity);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-ignite-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.FLINT_AND_STEEL,
				"Ignite",
				"",
				"Set the player",
				"on fire.")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.duration = map.containsKey("Duration") ? map.get("Duration", SimpleTime.class) : SimpleTime.fromString("5 seconds");
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();
		map.put("Duration", this.duration);

		return map;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return new SkillSettingsMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class SkillSettingsMenu extends Menu {

		@Position(start = StartPosition.CENTER)
		private final Button durationButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Ignite Skill Settings");

			this.durationButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.FIRE_CHARGE,
					"Duration",
					"",
					"Current: &f" + TimeUtil.formatTimeGeneric(SkillIgnite.this.duration.getTimeSeconds()),
					"",
					"How long will player",
					"be set on fire?"),

					player -> {
						new SimpleStringPrompt("Enter how long the player will burn. Current: '" + SkillIgnite.this.duration + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final SimpleTime newValue = SimpleTime.fromString(input);

									if (newValue.getTimeTicks() > 0 && newValue.getTimeSeconds() <= 180) {
										SkillIgnite.this.duration = newValue;
										SkillIgnite.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid duration! Enter a humean readable format such as '30 seconds' from 1 second to 3 minutes.";
							}

						}.show(player);
					});
		}

		@Override
		public Menu newInstance() {
			return new SkillSettingsMenu(this.getParent());
		}
	}
}
