package org.mineacademy.boss.spawn;

import java.time.LocalTime;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Represents a rule that spawns Bosses at an exact date.
 */
@Getter
abstract class SpawnRuleDateExact extends SpawnRule {

	/**
	 * When which minute should the boss be spawned?
	 */
	private int whichMinute;

	/**
	 * Which hour should the boss be spawned?
	 */
	private int whichHour;

	/**
	 * Create a new spawn rule with dates set
	 *
	 * @param name
	 */
	protected SpawnRuleDateExact(String name, SpawnRuleType type) {
		super(name, type);
	}

	/* ------------------------------------------------------------------------------- */
	/* Data */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.boss.spawn.SpawnRule#onLoad()
	 */
	@Override
	protected void onLoad() {
		super.onLoad();

		this.whichMinute = this.getInteger("Minute", -1);
		this.whichHour = this.getInteger("Hour", -1);
	}

	@Override
	public void onSave() {
		this.set("Minute", this.whichMinute);
		this.set("Hour", this.whichHour);

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

		if (this.getWhichHour() != -1 && this.getWhichHour() != time.getHour()) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life hour (" + time.getHour() + ") out of range: " + this.getWhichHour());

			return false;
		}

		if (this.getWhichMinute() != -1 && this.getWhichMinute() != time.getMinute()) {
			Debugger.debug("spawning", "[SpawnRule=" + this.getName() + "] Not running due to real-life minute (" + time.getMinute() + ") out of range: " + this.getWhichMinute());

			return false;
		}

		return super.canRun();
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
				CompMaterial.RAIL,
				"Hour",
				"",
				"Current: &f" + (this.whichHour == -1 ? "&aany hour" : this.whichHour),
				"",
				"Click to change what",
				"hour of real-life day",
				"this rule will execute."),
				player -> {
					new SimpleDecimalPrompt("Enter what hour of real-life day this rule will execute, or set to -1 for any hour. Current: " + (this.whichHour == -1 ? "any" : this.whichHour) + ".") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {
							return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), -1, 23);
						}

						@Override
						protected String getFailedValidationText(ConversationContext context, String invalidInput) {
							return "Invalid hour '" + invalidInput + "'! Enter -1 for any hour, or type the hour from 0-23.";
						}

						@Override
						protected String getMenuAnimatedTitle() {
							return "Set the hour to " + (SpawnRuleDateExact.this.whichHour == -1 ? "any" : SpawnRuleDateExact.this.whichHour);
						}

						@Override
						protected void onValidatedInput(ConversationContext context, double input) {
							SpawnRuleDateExact.this.setWhichHour((int) input);
						}
					}.show(player);
				}));

		buttons.add(Button.makeSimple(ItemCreator.from(
				CompMaterial.POWERED_RAIL,
				"Minute",
				"",
				"Current: &f" + (this.whichMinute == -1 ? "&aany minute" : this.whichMinute),
				"",
				"Click to change what",
				"minutes of real-life hours",
				"you select in the next icon",
				"this rule will execute"),
				player -> {
					new SimpleDecimalPrompt("Enter what minute of real-life hour this rule will execute, or set to -1 for any minute. Current: " + (this.whichMinute == -1 ? "any" : this.whichMinute) + ".") {

						@Override
						protected boolean isInputValid(ConversationContext context, String input) {
							return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), -1, 59);
						}

						@Override
						protected String getFailedValidationText(ConversationContext context, String invalidInput) {
							return "Invalid minute '" + invalidInput + "'! Enter -1 for any minute, or type the minute from 0-59.";
						}

						@Override
						protected String getMenuAnimatedTitle() {
							return "Set the minute to " + (SpawnRuleDateExact.this.whichMinute == -1 ? "any" : SpawnRuleDateExact.this.whichMinute);
						}

						@Override
						protected void onValidatedInput(ConversationContext context, double input) {
							SpawnRuleDateExact.this.setWhichMinute((int) input);
						}
					}.show(player);
				}));

		return buttons;
	}

	/* ------------------------------------------------------------------------------- */
	/* Setters */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Which minute to spawn
	 *
	 * @param whichMinute the whichMinute to set
	 */
	public final void setWhichMinute(int whichMinute) {
		this.whichMinute = whichMinute;

		this.save();
	}

	/**
	 * Which hour to spawn
	 *
	 * @param whichHour the whichHour to set
	 */
	public final void setWhichHour(int whichHour) {
		this.whichHour = whichHour;

		this.save();
	}
}
