package org.mineacademy.boss.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mineacademy.boss.model.skills.SkillArrow;
import org.mineacademy.boss.model.skills.SkillBomb;
import org.mineacademy.boss.model.skills.SkillConfuse;
import org.mineacademy.boss.model.skills.SkillDisarm;
import org.mineacademy.boss.model.skills.SkillEnderman;
import org.mineacademy.boss.model.skills.SkillFire;
import org.mineacademy.boss.model.skills.SkillFireball;
import org.mineacademy.boss.model.skills.SkillFreeze;
import org.mineacademy.boss.model.skills.SkillLightning;
import org.mineacademy.boss.model.skills.SkillMinions;
import org.mineacademy.boss.model.skills.SkillPotions;
import org.mineacademy.boss.model.skills.SkillStealLife;
import org.mineacademy.boss.model.skills.SkillTeleport;
import org.mineacademy.boss.model.skills.SkillThrow;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.settings.YamlConfig;

/**
 * The registry of all known skills the boss accept
 */
public final class BossSkillRegistry {

	/**
	 * The registered skills.
	 */
	private static final StrictList<BossSkill> skills = new StrictList<>();

	// Native skills
	private static final StrictList<BossSkill> natives = new StrictList<>();

	/**
	 * An internal method to register our default skills.
	 * <p>
	 * Since Boss supports reload, all old skills will be removed.
	 */
	public static void registerDefaults() {
		natives.clear();

		registerNative(new SkillThrow());
		registerNative(new SkillLightning());
		registerNative(new SkillConfuse());
		registerNative(new SkillBomb());
		registerNative(new SkillTeleport());
		registerNative(new SkillFreeze());
		registerNative(new SkillFireball());
		registerNative(new SkillArrow());
		registerNative(new SkillFire());
		registerNative(new SkillDisarm());
		registerNative(new SkillEnderman());
		registerNative(new SkillStealLife());
		registerNative(new SkillMinions());
		registerNative(new SkillPotions());
	}

	/**
	 * Register a new skill, or throw an error if exists.
	 *
	 * @param skill the skill to register
	 */
	public static void register(BossSkill skill) {
		register(skill, skills);
	}

	public static void registerNative(BossSkill skill) {
		register(skill, natives);
	}

	private static void register(BossSkill skill, StrictList<BossSkill> list) {
		if (isRegistered(skill))
			throw new FoException("Boss skill already registered: " + skill.getName());

		Common.registerEvents(skill);

		try {
			new SkillConfig(skill); // Load settings automatically

			list.add(skill);

		} catch (final Throwable t) {
			Common.error(t,
					"Error loading skill " + skill.getName(),
					"Error: %error",
					"The skill has not been loaded.");
		}
	}

	/**
	 * A helper class for loading skill settings
	 */
	private static class SkillConfig extends YamlConfig {

		private final BossSkill skill;

		private SkillConfig(BossSkill skill) {
			this.skill = skill;

			setHeader(makeHeader(skill));
			loadConfiguration(null, "skills/" + skill.getName() + ".yml");
		}

		@Override
		protected void onLoadFinish() {
			SerializedMap content = getMap("");

			{ // Load skill specific settings
				skill.readSettings(content.asMap());
			}

			{ // Save in case of change
				final Map<String, Object> map = skill.writeSettings();

				for (final Map.Entry<String, Object> e : map.entrySet()) {
					final String path = e.getKey();

					if (!isSet(path))
						setNoSave(path, e.getValue());
				}
			}

			{ // Delay
				if (!content.containsKey("Delay")) {
					setNoSave("Delay", skill.getDefaultDelay().getRaw());

					content = getMap("");
				}

				final String[] line = content.getString("Delay").split(" - ");

				skill.setDelay(new BossSkillDelay(line[0], line.length == 2 ? line[1] : line[0]));
			}

			{ // Message

				// Set defaults if does not exist
				if (!content.containsKey("Message")) {
					setNoSave("Message", Arrays.asList(skill.getDefaultMessage()));

					content = getMap("");
				}

				final List<String> list = new ArrayList<>();

				if (content.getObject("Message") instanceof String)
					list.add(content.getString("Message"));

				else
					list.addAll(content.getStringList("Message"));

				skill.setMessages(list.toArray(new String[list.size()]));
			}
		}
	}

	private static String[] makeHeader(BossSkill skill) {
		final List<String> lines = new ArrayList<>(Arrays.asList(
				"------------------------------------------------------------------------",
				" This is the configuration for the skill '" + skill.getName() + "'",
				"------------------------------------------------------------------------",
				" ",
				"Documentation:",
				"  Delay - The duration of the pause between the skill is run again.",
				"          You can specify a range that will be choosen randomly.",
				"  Message - The message to send to the player when the skill is run.",
				"            If you have multiple lines, one is choosen randomly."));

		if (skill.getDefaultHeader() != null)
			lines.addAll(Arrays.asList(skill.getDefaultHeader()));

		lines.add("\n------------------------------------------------------------------------\n");

		return lines.toArray(new String[lines.size()]);
	}

	/**
	 * Get if the skill has been registered.
	 *
	 * @param skill the skill
	 * @return whether it is registered
	 */
	public static boolean isRegistered(BossSkill skill) {
		return skills.contains(skill) || natives.contains(skill);
	}

	/**
	 * Return all registered skills. Cannot modify!
	 *
	 * @return
	 */
	public static List<BossSkill> getRegistered() {
		return Collections.unmodifiableList(Common.joinArrays(skills, natives));
	}

	/**
	 * Get a skill by its {@link BossSkill#getName()}
	 *
	 * @param name the name
	 * @return the found, or null if not
	 */
	public static BossSkill getByName(String name) {
		for (final BossSkill skill : Common.joinArrays(skills, natives))
			if (skill.getName().equalsIgnoreCase(name.replace("_", " ")))
				return skill;

		return null;
	}
}
