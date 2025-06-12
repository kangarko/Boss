package org.mineacademy.boss.skill;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillLightning extends BossSkill {

	private int radius;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(LivingEntity entity) {

		for (final Entity nearby : entity.getNearbyEntities(this.radius, this.radius - 1, this.radius))
			if (nearby instanceof Player && Boss.canTarget(nearby)) {
				final LightningStrike lightning = nearby.getWorld().spawn(nearby.getLocation(), LightningStrike.class);

				BossSkillTag.IS_CANCELLING_COMBUSTION.set(lightning, true);
				BossSkillTag.IS_CANCELLING_DAMAGE_TO_NON_PLAYERS.set(lightning, true);

				this.sendSkillMessage((Player) nearby, entity);
				this.executeSkillCommands((Player) nearby, entity);

				return true;
			}

		return false;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-lightning-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.ENDER_EYE,
				"Lightning",
				"",
				"The player will be",
				"striked by lightning.")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.radius = map.getInteger("Radius", 5);
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();

		map.put("Radius", this.radius);

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
		private final Button radiusButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Lightning Skill Settings");

			this.radiusButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDER_PEARL,
					"Radius",
					"",
					"Current: &f" + SkillLightning.this.radius,
					"",
					"How many blocks around",
					"Boss looks for players",
					"to strike?"),

					player -> {
						new SimpleDecimalPrompt("Enter how many blocks around the Boss will look for players. Current: '" + SkillLightning.this.radius + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 50) {
										SkillLightning.this.radius = newRadius;
										SkillLightning.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid radius, enter a whole number between 1-50";
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
