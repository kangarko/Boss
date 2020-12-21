package org.mineacademy.boss.api;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.storage.SimplePlayerData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a special Boss ability.
 */
public abstract class BossSkill implements Listener {

	// ------------------------------------------------------------------------
	// Settings below are loaded automatically from the config, do not modify!
	// ------------------------------------------------------------------------

	/**
	 * The delay of this skill, set automatically when loading this skill.
	 */
	@Setter
	@Getter
	private BossSkillDelay delay = null;

	/**
	 * The skill messages from the Boss to the player, set automatically when loading this skill.
	 */
	@Setter
	@Getter
	private String[] messages = null;

	/**
	 * Send the skill message to the player
	 *
	 * @param player the player
	 */
	public final void sendSkillMessage(Player player, SpawnedBoss boss) {
		if (messages != null && messages.length > 0) {
			final int index = RandomUtil.nextBetween(0, messages.length - 1);

			Common.tell(player, messages[index].replace("{boss}", boss.getBoss().getName()).replace("{world}", boss.getEntity().getWorld().getName()));
		}
	}

	// ------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------

	/**
	 * Get the name of the skill. This can be used
	 * in Boss' configuration.
	 *
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Get the icon in the menu
	 *
	 * @return the menu icon
	 */
	public abstract ItemStack getIcon();

	/**
	 * Get the default delay in case not set
	 *
	 * @return the delay delay
	 */
	public abstract BossSkillDelay getDefaultDelay();

	/**
	 * Get the default message to the player when the skill is executed properly
	 * They are picked up randomly.
	 * <p>
	 * NB: IT IS YOUR RESPONSIBILITY TO INVOKE {@link #sendSkillMessage(Player)}!
	 *
	 * @return the default message, null or empty if none
	 */
	public abstract String[] getDefaultMessage();

	/**
	 * Evaluate if the skill can be used on the Minecraft server the user has.
	 *
	 * @return if the skill is compatible with the Minecraft server that is used
	 */
	public boolean isCompatible() {
		return true;
	}

	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	/**
	 * Runs this skill for the specified living Boss.
	 *
	 * @param spawned the spawned Boss for which the ability shall be executed
	 * @return whether or not the skill has been executed successfully
	 */
	public abstract boolean execute(SpawnedBoss spawned);

	/**
	 * If this skill adds an additional effect for the target player like setting his walk speed
	 * to 0 or potion effects, remove them.
	 *
	 * @param player
	 */
	public void removeSkillEffect(Player player) {
		final BossSkillRestore skillRestore = getSkillRestore();

		if (skillRestore != null)
			skillRestore.removeSkillEffect(player);
	}

	/**
	 * Get the method that removes the effect this skill applies, for example
	 * freeze skill will set the player walk speed to 0. This method restores it back.
	 *
	 * @return
	 */
	protected BossSkillRestore getSkillRestore() {
		return null;
	}

	/**
	 * Schedule the {@link #getSkillRestore(Player)} removing the skill effect from player
	 * after certain time. If the player has disconnected, we remove the effect once he logs back in
	 * automatically.
	 *
	 * @param delayTicks
	 * @param player
	 */
	protected final void scheduleSkillRestore(int delayTicks, Player player) {
		final BossSkillRestore skillRestore = getSkillRestore();
		Valid.checkNotNull("Boss skill " + getName() + " does not implement getSkillRestore!");

		Common.runLater(delayTicks, () -> {
			final Player updatedPlayer = Bukkit.getPlayer(player.getUniqueId());

			if (updatedPlayer != null && updatedPlayer.isOnline())
				skillRestore.removeSkillEffect(updatedPlayer);
			else
				SimplePlayerData.$().addSkillToRestore(player, this);
		});
	}

	// ------------------------------------------------------------------------
	// Configuration (default API)
	// ------------------------------------------------------------------------

	/**
	 * Get additional lines used in the header that will be placed as such:
	 * <p>
	 * -------------------
	 * {main header text}
	 * <p>
	 * {
	 * your lines
	 * }
	 * -------------------
	 *
	 * @return the header
	 */
	public String[] getDefaultHeader() {
		return null;
	}

	/**
	 * Serialize the settings for this skill.
	 * You MUST include the default options in case this is called when the
	 * settings file doesn't exists or some options aren't set.
	 *
	 * @return the {@link Map} of serialized configuration options
	 */
	public Map<String, Object> writeSettings() {
		return new HashMap<>();
	}

	/**
	 * Deserialize and load settings from the map.
	 *
	 * @param map the {@link Map} to serialize from
	 */
	public void readSettings(Map<String, Object> map) {
	}

	// ------------------------------------------------------------------------
	// Utility for quickly setting and reading metadata
	// ------------------------------------------------------------------------

	/**
	 * Set an entity metadata
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	protected final void setMetadata(Entity entity, String key, Object value) {
		entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));
	}

	/**
	 * Return a certain metadata for the entity, or null if not set
	 *
	 * @param <T>
	 * @param entity
	 * @param key
	 * @return
	 */
	protected final <T> T getMetadata(Entity entity, String key, T def) {
		return entity.hasMetadata(key) ? (T) entity.getMetadata(key).get(0).value() : def;
	}
}
