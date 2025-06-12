package org.mineacademy.boss.skill;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.task.TaskFrozenPlayers;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillFreeze extends AbstractTargetSkill {

	/**
	 * How long shall the player be frozen in ticks?
	 */
	private SimpleTime duration;

	/**
	 * Create cobweb effect?
	 */
	private boolean cobweb;

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("20 seconds - 40 seconds");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		final TaskFrozenPlayers task = TaskFrozenPlayers.getInstance();

		if (task.isFrozen(target))
			return true;

		task.freeze(target, this.duration, this.cobweb);
		this.sendSkillMessage(target, entity);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-freeze-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.ICE,
				"Freeze",
				"",
				"Freeze the player",
				"completely.")
				.make();
	}

	@Override
	public void readSettings(SerializedMap map) {
		this.duration = map.containsKey("Duration") ? map.get("Duration", SimpleTime.class) : SimpleTime.fromString("3 seconds");
		this.cobweb = map.getBoolean("Cobweb", true);
	}

	@Override
	public SerializedMap writeSettings() {
		final SerializedMap map = new SerializedMap();

		map.put("Duration", this.duration);
		map.put("Cobweb", this.cobweb);

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

		@Position(9 * 1 + 3)
		private final Button destroyBlocksButton;

		@Position(9 * 1 + 5)
		private final Button durationButton;

		SkillSettingsMenu(Menu parent) {
			super(parent);

			this.setSize(9 * 3);
			this.setTitle("Freeze Skill Settings");

			this.destroyBlocksButton = new Button() {

				@Override
				public void onClickedInMenu(Player player, Menu menu, ClickType click) {
					SkillFreeze.this.cobweb = !SkillFreeze.this.cobweb;
					SkillFreeze.this.save();

					SkillSettingsMenu.this.restartMenu(SkillFreeze.this.cobweb ? "&2Cobweb Will Be Spawned" : "&4Cobweb Won't Be Spawned");
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.from(
							CompMaterial.COBWEB,
							"Cobweb",
							"",
							"Status: " + (SkillFreeze.this.cobweb ? "&aenabled" : "&cdisabled"),
							"",
							"Should we spawn cobweb",
							"at player's location,",
							"if it's air? We remove",
							"it after the skill ends.")
							.glow(SkillFreeze.this.cobweb)
							.make();
				}
			};

			this.durationButton = Button.makeSimple(ItemCreator.from(
					CompMaterial.CLOCK,
					"Duration",
					"",
					"Current: &f" + TimeUtil.formatTimeGeneric(SkillFreeze.this.duration.getTimeSeconds()),
					"",
					"How long will player",
					"be frozen?"),

					player -> {
						new SimpleStringPrompt("Enter how long the player will freeze. Current: '" + SkillFreeze.this.duration + "'.") {

							@Override
							protected boolean isInputValid(ConversationContext context, String input) {

								try {
									final SimpleTime newValue = SimpleTime.fromString(input);

									if (newValue.getTimeTicks() > 0 && newValue.getTimeSeconds() <= 60 * 10) {
										SkillFreeze.this.duration = newValue;
										SkillFreeze.this.save();

										return true;
									}

								} catch (final Throwable t) {
									// see getFailedValiationText
								}

								return false;
							}

							@Override
							protected String getFailedValidationText(ConversationContext context, String invalidInput) {
								return "Invalid duration! Enter a humean readable format such as '30 seconds' from 1 second to 10 minutes.";
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
