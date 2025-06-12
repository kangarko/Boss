package org.mineacademy.boss.skill;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossCommand;
import org.mineacademy.boss.model.BossCommandType;
import org.mineacademy.boss.task.TaskBehavior;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.RangedSimpleTime;

import lombok.NonNull;

/**
 * Represents a special Boss ability executed when nearby players in a period.
 */
public abstract class BossSkill implements ConfigSerializable {

	/**
	 * Registered skills by name
	 */
	private static final Map<String, Class<? extends BossSkill>> byName = new HashMap<>();

	/**
	 * The name of this skill
	 */
	private String name;

	/**
	 * The boss owning this skill
	 */
	private Boss boss;

	/**
	 * The delay of this skill, set automatically when loading this skill.
	 */
	private RangedSimpleTime delay = null;

	/**
	 * A list of commands executed when the skill is successfully fired.
	 */
	private List<BossCommand> commands = null;

	/**
	 * The skill messages from the Boss to the player, set automatically when loading this skill.
	 */
	private List<String> messages = null;

	/**
	 * Whether to stop the boss after the first successful skill was found and executed
	 */
	private boolean stopMoreSkills = false;

	/*
	 * Create a new boss skill.
	 */
	protected BossSkill() {
		Valid.checkBoolean(!(this instanceof Listener), "Skill " + this.getClass().getSimpleName() + " can no longer implement a Listener, take your event handlers into a separate class!");
	}

	/* ------------------------------------------------------------------------------- */
	/* Main functions */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Runs this skill for the specified living Boss.
	 *
	 * @param boss
	 *
	 * @return whether or not the skill has been executed successfully
	 */
	public abstract boolean execute(LivingEntity boss);

	/**
	 * Send the skill message to the player
	 *
	 * @param player the player
	 * @param entity
	 */
	public final void sendSkillMessage(Player player, LivingEntity entity) {
		if (this.messages != null && !this.messages.isEmpty()) {
			final String message = RandomUtil.nextItem(this.messages);

			if (message != null && entity.isValid())
				// We need to replace as legacy because for example hover event will BREAK placeholders due to Adventure bug
				Messenger.warn(player, this.boss.replaceVariablesLegacy(message, entity, player));
		}
	}

	/**
	 * Execute the player and console commands for the Boss
	 *
	 * @param player the player to replace variables as, can be mnull
	 * @param bossEntity the Boss entity
	 */
	public final void executeSkillCommands(@Nullable Player player, LivingEntity bossEntity) {
		for (final BossCommand command : this.commands)
			this.boss.runCommand(command.getCommand(), command.getChance(), command.isConsole(), player, bossEntity);
	}

	/* ------------------------------------------------------------------------------- */
	/* Implementable methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Evaluate if the skill can be used on the Minecraft server the user has.
	 *
	 * @return if the skill is compatible with the Minecraft server that is used
	 */
	public boolean isCompatible() {
		return true;
	}

	/**
	 * Return if your skill can apply to the given boss
	 *
	 * @param boss
	 * @return
	 */
	public boolean canApplyTo(Boss boss) {
		return true;
	}

	/**
	 * Get the icon in the menu
	 *
	 * @return the menu icon
	 */
	public abstract ItemStack getIcon();

	/**
	 * The skill menu where custom settings residue
	 * @param parent
	 *
	 * @return
	 */
	public Menu getMenu(Menu parent) {
		return null;
	}

	/**
	 * Get the default delay in case not set
	 *
	 * @return the delay delay
	 */
	protected abstract RangedSimpleTime getDefaultDelay();

	/**
	 * Get the default message to the player when the skill is executed properly
	 * They are picked up randomly.
	 * <p>
	 * NB: IT IS YOUR RESPONSIBILITY TO INVOKE {@link #sendSkillMessage(Player)}!
	 *
	 * @return the default message, null or empty if none
	 */
	protected abstract String[] getDefaultMessage();

	/* ------------------------------------------------------------------------------- */
	/* Saving skill setting */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Serialize the settings for this skill.
	 * You MUST include the default options in case this is called when the
	 * settings file doesn't exists or some options aren't set.
	 *
	 * @return the serialize map of serialized configuration options
	 */
	public SerializedMap writeSettings() {
		return new SerializedMap();
	}

	/**
	 * Deserialize and load settings from the map.
	 *
	 * @param map the serialized map to serialize from
	 */
	public void readSettings(SerializedMap map) {
	}

	/**
	 * Saves the skill settings for the given boss
	 *
	 */
	public final void save() {
		this.boss.addSkill(this);
	}

	/* ------------------------------------------------------------------------------- */
	/* Final getter */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Get the name of the skill. This can be used
	 * in Boss' configuration.
	 *
	 * @return the name
	 */
	public final String getName() {
		Valid.checkNotNull(this.name);

		return this.name;
	}

	/**
	 * @return the boss
	 */
	public final Boss getBoss() {
		Valid.checkNotNull(this.boss);

		return this.boss;
	}

	/**
	 * @return the delay
	 */
	public final RangedSimpleTime getDelay() {
		Valid.checkNotNull(this.delay);

		return this.delay;
	}

	/**
	 * @param delay the delay to set
	 */
	public final void setDelay(RangedSimpleTime delay) {
		this.delay = delay;

		this.save();

		TaskBehavior.getInstance().resetFutureTimes(this);
	}

	/**
	 *
	 * @return
	 */
	public final List<BossCommand> getCommands() {
		Valid.checkNotNull(this.commands);

		return Collections.unmodifiableList(this.commands);
	}

	/**
	 * @param commands the commands to set
	 */
	public final void setCommands(List<BossCommand> commands) {
		this.commands = commands;

		this.save();
	}

	/**
	 * @param command the command to add
	 */
	public final void addCommand(String command) {
		this.commands.add(BossCommand.create(this.boss, BossCommandType.SKILL, command));

		this.save();
	}

	/**
	 * @param command the command to add
	 */
	public final void removeCommand(BossCommand command) {
		this.commands.remove(command);

		this.save();
	}

	/**
	 * @return the messages
	 */
	public final List<String> getMessages() {
		Valid.checkNotNull(this.messages);

		return this.messages;
	}

	/**
	 * @param messages the messages to set
	 */
	public final void setMessages(@NonNull List<String> messages) {
		this.messages = messages;

		this.save();
	}

	/**
	 * @return
	 */
	public final boolean isStopMoreSkills() {
		return this.stopMoreSkills;
	}

	/**
	 * Set whether to stop looking for more skills to execute after this one
	 *
	 * @param stopMoreSkills
	 */
	public final void setStopMoreSkills(boolean stopMoreSkills) {
		this.stopMoreSkills = stopMoreSkills;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		return obj instanceof BossSkill && ((BossSkill) obj).getName().equalsIgnoreCase(this.name);
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public final SerializedMap serialize() {
		return SerializedMap.fromArray(
				"Delay", this.delay,
				"Commands", this.commands,
				"Messages", this.messages,
				"Stop_More_Skills", this.stopMoreSkills,
				"Settings", this.writeSettings());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Register a new skill by its unique name.
	 *
	 * @param name
	 * @param skillClass
	 */
	public static final void registerSkill(String name, Class<? extends BossSkill> skillClass) {
		byName.put(name, skillClass);
	}

	/**
	 * Return all skill names.
	 *
	 * @return
	 */
	public static final Set<String> getSkillsNames() {
		return byName.keySet();
	}

	/**
	 * Create a new BossSkill instance
	 *
	 * @param skillName
	 * @param map
	 * @param boss
	 * @return
	 */
	public static final BossSkill createInstance(String skillName, Boss boss, SerializedMap map) {
		final Class<? extends BossSkill> skillClass = byName.get(skillName);
		Valid.checkNotNull(skillClass, "Skill " + skillName + " not registered!");
		Valid.checkBoolean(!Modifier.isAbstract(skillClass.getModifiers()), "Cannot create skill " + skillName + " because its class is abstract: " + skillClass);

		final BossSkill instance;

		// Create instance
		try {
			final Constructor<? extends BossSkill> constructor = skillClass.getDeclaredConstructor();
			constructor.setAccessible(true);

			Valid.checkBoolean(Modifier.isPrivate(constructor.getModifiers()) && constructor.getParameterCount() == 0,
					"Your skill class " + skillClass.getSimpleName() + " must have a private no args constructor, found: " + constructor);

			instance = constructor.newInstance();

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Failed to create new skill from " + skillClass + " for " + boss);

			return null;
		}

		if (!instance.canApplyTo(boss))
			return null;

		// Assign fields
		instance.name = skillName;
		instance.boss = boss;
		instance.delay = map.containsKey("Delay") ? map.get("Delay", RangedSimpleTime.class) : instance.getDefaultDelay();
		instance.commands = map.getList("Commands", BossCommand.class, boss);
		instance.messages = Collections.unmodifiableList(map.getStringList("Messages", Arrays.asList(instance.getDefaultMessage())));
		instance.stopMoreSkills = map.getBoolean("Stop_More_Skills", true);

		// Load skill-specific settings
		instance.readSettings(map.getMap("Settings"));

		return instance;
	}
}
