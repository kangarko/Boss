package org.mineacademy.boss.api;

import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * The delay between each skill execution.
 */
@Getter
public final class BossSkillDelay {

	/**
	 * The minimum time in ticks
	 */
	private final int minimum;

	/**
	 * The maximum time in ticks
	 */
	private final int maximum;

	/**
	 * The raw format.
	 */
	private String raw;

	/**
	 * Make a new random delay from values such as:
	 * 1 second
	 * 10 ticks
	 * 20 minutes
	 * etc.
	 *
	 * @param minimumTime
	 * @param maximumTime
	 */
	public BossSkillDelay(String minimumTime, String maximumTime) {
		this((int) TimeUtil.toTicks(minimumTime), (int) TimeUtil.toTicks(maximumTime));

		this.raw = minimumTime + (isRandom() ? " - " + maximumTime : "");
	}

	/**
	 * Make a new fixed delay.
	 *
	 * @param delayTicks the fixed delay
	 */
	public BossSkillDelay(int delayTicks) {
		this(delayTicks, delayTicks);
	}

	/**
	 * MAke a new random delay.
	 *
	 * @param minimumTicks
	 * @param maximumTicks
	 */
	public BossSkillDelay(int minimumTicks, int maximumTicks) {
		Valid.checkBoolean(minimumTicks > 0 && maximumTicks > 0, "Minimum and maximum must be > 0");
		Valid.checkBoolean(minimumTicks <= maximumTicks, "Minimum must be less or equals maximum (" + minimumTicks + " vs " + maximumTicks + ")");

		this.minimum = minimumTicks;
		this.maximum = maximumTicks;
		this.raw = isRandom() ? minimum + " ticks - " + maximum + " ticks" : minimum + " ticks";
	}

	/**
	 * Generates a new random value, in ticks.
	 *
	 * @return the random value
	 */
	public int getDelay() {
		return RandomUtil.nextBetween(minimum, maximum);
	}

	/**
	 * Random delay means that the minimum
	 * value is different from the maximum
	 * and we should pick up randomly in between.
	 *
	 * @return true if the delay is random
	 */
	public boolean isRandom() {
		return minimum != maximum;
	}
}
