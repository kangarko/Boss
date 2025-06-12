package org.mineacademy.boss.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillEnderman extends BossSkill {

	/**
	 * How many blocks away from the Boss to look for?
	 */
	private int radius;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("45 seconds - 2 minutes");
	}

	@Override
	public boolean execute(LivingEntity boss) {
		final List<Player> found = new ArrayList<>();

		for (final Entity nearby : boss.getNearbyEntities(this.radius, this.radius - 1, this.radius))
			if (nearby instanceof Player)
				found.add((Player) nearby);

		if (!found.isEmpty()) {
			Collections.shuffle(found);

			final Player target = found.get(0);

			if (Boss.canTarget(target)) {
				boss.teleport(target.getLocation());
				CompSound.ENTITY_ENDERMAN_TELEPORT.play(target);

				this.sendSkillMessage(target, boss);
				this.executeSkillCommands(target, boss);

				return true;
			}
		}

		return false;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-enderman-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.ENDER_PEARL,
				"Enderman",
				"",
				"Boss will teleport itself",
				"to a random player.")
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
			this.setTitle("Enderman Skill Settings");

			this.radiusButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDER_PEARL,
					"Radius",
					"",
					"Current: &f" + SkillEnderman.this.radius,
					"",
					"How many blocks around",
					"should Boss look for players",
					"to teleport itself to?"),

					player -> {
						new SimpleDecimalPrompt("Enter how many blocks around the Boss will look for players. Current: '" + SkillEnderman.this.radius + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 50) {
										SkillEnderman.this.radius = newRadius;
										SkillEnderman.this.save();

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
