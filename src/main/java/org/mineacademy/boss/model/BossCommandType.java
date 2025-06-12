package org.mineacademy.boss.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the type of boss command
 */
@RequiredArgsConstructor
public enum BossCommandType {

	/**
	 * Run on Boss spawn
	 */
	SPAWN("spawn"),

	/**
	 * Run on Boss death
	 */
	DEATH("death"),

	/**
	 * Run when Boss' health lowers to certain threshold
	 */
	HEALTH_TRIGGER("health trigger"),

	/**
	 * Run when Boss' runs his skill ability.
	 */
	SKILL("skill");

	/**
	 * The menu label
	 */
	@Getter
	private final String menuLabel;
}