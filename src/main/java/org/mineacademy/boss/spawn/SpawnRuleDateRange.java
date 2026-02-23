package org.mineacademy.boss.spawn;

import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationContext;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Between minutes and hours to spawn Boss.
 */
@Getter
abstract class SpawnRuleDateRange extends SpawnRule {

	/**
	 * What minutes to spawn Boss?
	 */
	private Tuple<Integer, Integer> whichMinutes;

	/**
	 * What hours to spawn Boss?
	 */
	private Tuple<Integer, Integer> whichHours;

	/**
	 * Create new spawn rule
	 *
	 * @param name
	 * @param type
	 */
	public SpawnRuleDateRange(String name, SpawnRuleType type) {
		super(name, type);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	@Override
	protected void onLoad() {
		super.onLoad();

		this.whichMinutes = this.getTuple("Minutes", Integer.class, Integer.class);
		this.whichHours = this.getTuple("Hours", Integer.class, Integer.class);
	}

	@Override
	public void onSave() {
		this.set("Minutes", this.whichMinutes);
		this.set("Hours", this.whichHours);

		super.onSave();
	}

	/* ------------------------------------------------------------------------------- */
	/* Spawning */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#canRun()
	 */
	@Override
	protected boolean canRun() {

		final LocalTime time = TimeUtil.getCurrentTime();

		if (!this.isWithin(time.getHour(), this.getWhichHours())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life hour (" + time.getHour() + ") out of range: " + this.getWhichHours());

			return false;
		}

		if (!this.isWithin(time.getMinute(), this.getWhichMinutes())) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life minute (" + time.getMinute() + ") out of range: " + this.getWhichMinutes());

			return false;
		}

		return super.canRun();
	}

	private boolean isWithin(int value, Tuple<Integer, Integer> tuple) {
		return tuple == null || value >= tuple.getKey() && value <= tuple.getValue();
	}

	/* ------------------------------------------------------------------------------- */
	/* Menu system */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#getButtons(org.mineacademy.fo.menu.Menu)
	 */
	@Override
	public List<Button> getButtons(Menu parent) {
		final List<Button> buttons = super.getButtons(parent);

		buttons.add(Button.makeSimple(ItemCreator.from(
				CompMaterial.TORCH,
				"Hour Range",
				"",
				"Current: &f" + (this.whichHours == null ? "&aany hour" : this.whichHours.toLine()),
				"",
				"Click to change what",
				"hours of real-life day",
				"this rule will execute."),
				player -> {
					new SimpleStringPrompt("Enter what hours of real-life day this rule will execute such as '9 - 5' for 9am to 5pm, or set to -1 for any hour. "
							+ "Current: " + (this.whichHours == null ? "any" : this.whichHours) + ".") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {

							if ("-1".equals(input))
								return true;

							try {
								final String[] split = input.split("\\-");

								if (split.length == 1 || split.length == 2) {
									final int from = Integer.parseInt(split[0].trim());
									final int to = split.length == 2 ? Integer.parseInt(split[1].trim()) : from;

									if (from >= 0 && to <= 23)
										return true;
								}
							} catch (final Throwable t) {
							}

							return false;
						}

						@Override
						protected void onValidatedInput(ConversationContext context, String input) {
							if ("-1".equals(input))
								SpawnRuleDateRange.this.setWhichHours(null);
							else {
								final String[] split = input.split("\\-");
								final int from = Integer.parseInt(split[0].trim());
								final int to = split.length == 2 ? Integer.parseInt(split[1].trim()) : from;

								SpawnRuleDateRange.this.setWhichHours(new Tuple<>(from, to));
							}
						}

						@Override
						protected String getFailedValidationText(ConversationContext context, String invalidInput) {
							return "Invalid hour range '" + invalidInput + "'! Enter -1 for any hour, or type the hour from 0-23.";
						}

						@Override
						protected String getMenuAnimatedTitle() {
							return "Set hours to " + (SpawnRuleDateRange.this.whichHours == null ? "any" : SpawnRuleDateRange.this.whichHours);
						}
					}.show(player);
				}));

		buttons.add(Button.makeSimple(ItemCreator.from(
				CompMaterial.REDSTONE_TORCH,
				"Minute Range",
				"",
				"Current: &f" + (this.whichMinutes == null ? "&aany minute" : this.whichMinutes),
				"",
				"Click to change what",
				"minutes of real-life hours",
				"you select in the next icon",
				"this rule will execute"),
				player -> {
					new SimpleStringPrompt("Enter what minutes of real-life day this rule will execute such as '0 - 30' for 0 to 30th minute of selected hours, or set to -1 for any minute. "
							+ "Current: " + (this.whichMinutes == null ? "any" : this.whichMinutes) + ".") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {

							if ("-1".equals(input))
								return true;

							try {
								final String[] split = input.split("\\-");

								if (split.length == 1 || split.length == 2) {
									final int from = Integer.parseInt(split[0].trim());
									final int to = split.length == 2 ? Integer.parseInt(split[1].trim()) : from;

									if (from >= 0 && to <= 59)
										return true;
								}
							} catch (final Throwable t) {
							}

							return false;
						}

						@Override
						protected void onValidatedInput(ConversationContext context, String input) {
							if ("-1".equals(input))
								SpawnRuleDateRange.this.setWhichMinutes(null);
							else {
								final String[] split = input.split("\\-");
								final int from = Integer.parseInt(split[0].trim());
								final int to = split.length == 2 ? Integer.parseInt(split[1].trim()) : from;

								SpawnRuleDateRange.this.setWhichMinutes(new Tuple<>(from, to));
							}
						}

						@Override
						protected String getFailedValidationText(ConversationContext context, String invalidInput) {
							return "Invalid minute range '" + invalidInput + "'! Enter -1 for any minute, or type the minutes from 0-59.";
						}

						@Override
						protected String getMenuAnimatedTitle() {
							return "Set minutes to " + (SpawnRuleDateRange.this.whichMinutes == null ? "any" : SpawnRuleDateRange.this.whichMinutes.toLine());
						}
					}.show(player);
				}));

		return buttons;
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Set the minutes
	 *
	 * @param whichMinutes the whichMinutes to set
	 */
	public final void setWhichMinutes(@Nullable Tuple<Integer, Integer> whichMinutes) {
		this.whichMinutes = whichMinutes;

		this.save();
	}

	/**
	 * Set the hours
	 *
	 * @param whichHours the whichHours to set
	 */
	public final void setWhichHours(@Nullable Tuple<Integer, Integer> whichHours) {
		this.whichHours = whichHours;

		this.save();
	}
}
