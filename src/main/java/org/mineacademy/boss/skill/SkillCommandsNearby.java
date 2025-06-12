package org.mineacademy.boss.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillCommandsNearby extends BossSkill {

	private int radius;
	private int maxPlayers;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(LivingEntity entity) {
		boolean success = false;
		int executedTimes = 0;

		final List<Player> targets = new ArrayList<>();

		for (final Entity nearby : entity.getNearbyEntities(this.radius, this.radius - 1, this.radius))
			if (nearby instanceof Player && Boss.canTarget(nearby))
				targets.add((Player) nearby);

		Collections.sort(targets, new Comparator<Player>() {
			@Override
			public int compare(Player first, Player second) {
				return (int) (entity.getLocation().distance(first.getLocation()) - entity.getLocation().distance(second.getLocation()));
			}
		});

		for (final Player nearby : targets) {
			if (this.maxPlayers != -1 && ++executedTimes > this.maxPlayers)
				break;

			this.sendSkillMessage(nearby, entity);
			this.executeSkillCommands(nearby, entity);

			success = true;
		}

		return success;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[0];
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.REPEATING_COMMAND_BLOCK,
				"Commands For Nearby",
				"",
				"This is an empty skill you",
				"can program to run custom",
				"commands for a random nearby",
				"player in the given radius.")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.radius = map.getInteger("Radius", 5);
		this.maxPlayers = map.getInteger("Max_Players", -1);
	}

	@Override
	public SerializedMap writeSettings() {
		return SerializedMap.fromArray(
				"Radius", this.radius,
				"Max_Players", -1);
	}

	@Override
	public Menu getMenu(Menu parent) {
		return new SkillCommandsNearbyMenu(parent);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private class SkillCommandsNearbyMenu extends Menu {

		@Position(start = StartPosition.CENTER, value = -1)
		private final Button radiusButton;

		@Position(start = StartPosition.CENTER, value = +1)
		private final Button maxPlayersButton;

		SkillCommandsNearbyMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Throw Skill Settings");

			this.radiusButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.ENDER_PEARL,
					"Radius",
					"",
					"Current: &f" + SkillCommandsNearby.this.radius,
					"",
					"How many blocks around",
					"should Boss look for",
					"players to run commands for?"),

					player -> {
						new SimpleDecimalPrompt("Enter how many blocks around the Boss will look for players. Current: '" + SkillCommandsNearby.this.radius + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final int newRadius = Integer.parseInt(input);

									if (newRadius > 0 && newRadius <= 50) {
										SkillCommandsNearby.this.radius = newRadius;
										SkillCommandsNearby.this.save();

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

			this.maxPlayersButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.SLIME_BALL,
					"Max Players",
					"",
					"Current: &f" + SkillCommandsNearby.this.maxPlayers,
					"",
					"Click to edit max players",
					"to run commands for."),
					player -> {
						new SimpleDecimalPrompt("Enter the max players to run commands for, or -1 for all nearby players. We start by the closest player. Current: '" + SkillCommandsNearby.this.maxPlayers + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {
								try {
									if (Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), -1, 1000)) {
										SkillCommandsNearby.this.maxPlayers = Integer.parseInt(input);
										SkillCommandsNearby.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid max players, enter a whole number or -1 to run for all nearby players.";
							}
						}.show(player);
					});
		}

		@Override
		public Menu newInstance() {
			return new SkillCommandsNearbyMenu(this.getParent());
		}
	}
}
