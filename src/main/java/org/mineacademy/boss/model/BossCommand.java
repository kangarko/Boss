package org.mineacademy.boss.model;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a Boss command
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossCommand implements ConfigSerializable {

	/**
	 * For whom
	 */
	private final Boss boss;

	/**
	 * Type when this cmd is triggered
	 */
	@Getter
	private final BossCommandType type;

	/**
	 * The command
	 */
	@Getter
	private String command;

	/**
	 * Run as console?
	 */
	@Getter
	private boolean console;

	/**
	 * Do not run this command if the Boss is dead
	 */
	@Getter
	private boolean ignoreIfDead;

	/**
	 * Run chance
	 */
	@Getter
	private double chance;

	/**
	 * Trigger, requires type == {@link BossCommandType#HEALTH_TRIGGER}
	 */
	@Nullable
	private Double healthTrigger;

	/**
	 * Format the command, used in menus
	 *
	 * @return
	 */
	public String getCommandFormatted() {
		final String label = this.command.split(" ")[0];
		final boolean isSpecial = Arrays.asList("broadcast", "broadcast-damagers-list", "tell-damagers-list", "tell", "tell-damagers", "discord").contains(label);

		return (isSpecial ? "&d[" + label + "]&f" : "/" + label) + " " + Common.joinRange(1, this.command.split(" "));
	}

	/**
	 * @param command the command to set
	 */
	public void setCommand(String command) {
		this.command = command;

		this.boss.save();
	}

	/**
	 * @param console the console to set
	 */
	public void setConsole(boolean console) {
		this.console = console;

		this.boss.save();
	}

	/**
	 * @param ignoreIfDead the ignoreIfDead to set
	 */
	public void setIgnoreIfDead(boolean ignoreIfDead) {
		Valid.checkBoolean(this.type == BossCommandType.HEALTH_TRIGGER, "Cannot set ignore if dead for boss command of type " + this.type);
		this.ignoreIfDead = ignoreIfDead;

		this.boss.save();
	}

	/**
	 * @param chance the chance to set
	 */
	public void setChance(double chance) {
		this.chance = chance;

		this.boss.save();
	}

	/**
	 * @return the healthTrigger
	 */
	public Double getHealthTrigger() {
		Valid.checkBoolean(this.type == BossCommandType.HEALTH_TRIGGER, "Cannot get health trigger for boss command of type " + this.type);

		return this.healthTrigger;
	}

	/**
	 * @param healthTrigger the healthTrigger to set
	 */
	public void setHealthTrigger(double healthTrigger) {
		Valid.checkBoolean(this.type == BossCommandType.HEALTH_TRIGGER, "Cannot apply health trigger for boss command of type " + this.type);

		this.healthTrigger = healthTrigger;
		this.boss.save();
	}

	/**
	 *
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Type", this.type);
		map.put("Command", this.command);
		map.put("Console", this.console);
		map.put("Chance", this.chance);
		map.putIfExists("Health_Trigger", this.healthTrigger);

		return map;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BossCommand) {
			final BossCommand other = (BossCommand) obj;

			return other.type == this.type && other.command.equals(this.command) && other.console == this.isConsole() && other.chance == this.chance && Objects.equals(other.healthTrigger, this.healthTrigger);
		}

		return false;
	}

	@Override
	public String toString() {
		return this.command;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Load from file
	 *
	 * @param map
	 * @param boss
	 * @return
	 */
	public static BossCommand deserialize(SerializedMap map, Boss boss) {
		final BossCommand command = new BossCommand(boss, map.get("Type", BossCommandType.class));

		command.command = map.getString("Command");
		command.console = map.getBoolean("Console");
		command.chance = map.getDouble("Chance");
		command.healthTrigger = map.getDouble("Health_Trigger", null);

		return command;
	}

	/**
	 * Create a new boss command
	 *
	 * @param boss
	 * @param type
	 * @param commandLine
	 * @return
	 */
	public static BossCommand create(Boss boss, BossCommandType type, String commandLine) {
		final BossCommand command = new BossCommand(boss, type);

		command.command = commandLine;
		command.chance = 1D;
		command.console = true;

		return command;
	}
}