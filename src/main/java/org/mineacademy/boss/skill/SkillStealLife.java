package org.mineacademy.boss.skill;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillStealLife extends AbstractTargetSkill {

	private double percent;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("2 minute - 4 minutes");
	}

	@Override
	public boolean execute(Player target, LivingEntity boss) {
		final double take = Remain.getHealth(target) * this.percent;
		final double rest = Remain.getHealth(target) - take;

		// Player
		target.setHealth(MathUtil.range(rest, 0, Remain.getHealth(target)));
		CompSound.ENTITY_PLAYER_HURT.play(target, 1F, 1);
		CompParticle.CRIT_MAGIC.spawn(target.getEyeLocation());

		// Boss
		final double bossHealh = Remain.getHealth(boss);
		boss.setHealth(MathUtil.range(bossHealh + take, bossHealh, Remain.getMaxHealth(boss)));

		CompParticle.SPELL_WITCH.spawn(boss.getEyeLocation().clone().add(boss.getVelocity().normalize()));
		CompParticle.SPELL_WITCH.spawn(boss.getEyeLocation().clone().add(0, 0.5, 0));

		this.sendSkillMessage(target, boss);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-steal-life-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.FERMENTED_SPIDER_EYE,
				"Steal Life",
				"",
				"Boss will take a portion of",
				"player's health to his own!")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.percent = map.getDouble("Percent", 0.20D);
	}

	@Override
	public SerializedMap writeSettings() {
		return SerializedMap.fromArray("Percent", this.percent);
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
		private final Button percentButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Steal Life Skill Settings");

			this.percentButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.REDSTONE_BLOCK,
					"Percent To Take",
					"",
					"Current: &f" + (SkillStealLife.this.percent * 100) + "%",
					"",
					"How much percent of",
					"player's health will",
					"the Boss steal?"),

					player -> {
						new SimpleDecimalPrompt("Enter how much percent of player's health will the Boss steal (from 0.00 to 100.00). Current: '" + (SkillStealLife.this.percent * 100) + "'%.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final double newRadius = Double.parseDouble(input);

									if (newRadius > 0 && newRadius <= 100) {
										SkillStealLife.this.percent = newRadius / 100;
										SkillStealLife.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid amount, enter a decimal number between 0.00 and 100.00 (such as 25 for 25%)";
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
