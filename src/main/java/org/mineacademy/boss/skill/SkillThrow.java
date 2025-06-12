package org.mineacademy.boss.skill;

import org.bukkit.Location;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillThrow extends BossSkill {

	private int radius;
	private double power;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(LivingEntity entity) {
		final Location bossLocation = entity.getLocation();
		boolean success = false;

		for (final Entity nearby : entity.getNearbyEntities(this.radius, this.radius - 1, this.radius))
			if (nearby instanceof Player && Boss.canTarget(nearby)) {
				final Location loc = nearby.getLocation();
				final Vector vector = new Vector(loc.getX() - bossLocation.getX(), 0, loc.getZ() - bossLocation.getZ());

				vector.normalize().setY(this.power);

				if (!Valid.isFinite(vector))
					return false;

				try {
					BossSkillTag.IS_THROW_BY_BOSS.set(nearby, true);

					nearby.setVelocity(vector);

				} catch (final IllegalArgumentException ex) {
					continue;
				}

				this.sendSkillMessage((Player) nearby, entity);
				this.executeSkillCommands((Player) nearby, entity);

				Platform.runTask(2 * 20, () -> {
					if (nearby.isValid())
						BossSkillTag.IS_THROW_BY_BOSS.remove(nearby);
				});

				success = true;
			}

		return success;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-throw-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.PISTON,
				"Throw",
				"",
				"The player will be",
				"thrown up the skies")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.radius = map.getInteger("Radius", 5);
		this.power = map.getDouble("Power", 1.2D);
	}

	@Override
	public SerializedMap writeSettings() {
		return SerializedMap.fromArray(
				"Radius", this.radius,
				"Power", this.power);
	}

	@Override
	public Menu getMenu(Menu parent) {
		return new SkillSettingsMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class SkillSettingsMenu extends Menu {

		@Position(start = StartPosition.CENTER, value = -1)
		private final Button radiusButton;

		@Position(start = StartPosition.CENTER, value = +1)
		private final Button powerButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Throw Skill Settings");

			this.radiusButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDER_PEARL,
					"Radius",
					"",
					"Current: &f" + SkillThrow.this.radius,
					"",
					"How many blocks around",
					"should Boss look for",
					"players to throw?"),

					player -> {
						new SimpleDecimalPrompt("Enter how many blocks around the Boss will look for players. Current: '" + SkillThrow.this.radius + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 50) {
										SkillThrow.this.radius = newRadius;
										SkillThrow.this.save();

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

			this.powerButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.SLIME_BALL,
					"Power",
					"",
					"Current: &f" + MathUtil.formatTwoDigits(SkillThrow.this.power),
					"",
					"Click to edit",
					"throw power."),
					player -> {
						new SimpleDecimalPrompt("Enter the throw power (as a decimal number). Current: '" + MathUtil.formatTwoDigits(SkillThrow.this.power) + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									if (Valid.isDecimal(input) && Valid.isInRange(Float.parseFloat(input), 0.01, 10)) {
										SkillThrow.this.power = Float.parseFloat(input);
										SkillThrow.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid explosion power, enter a decimal number between 0.01 and 10 (high values may wreck the dude).";
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
