package org.mineacademy.boss.storage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.mineacademy.boss.api.BossSkill;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.settings.YamlSectionConfig;

public final class SimplePlayerData extends YamlSectionConfig {

	private final static SimplePlayerData instance = new SimplePlayerData();

	public static SimplePlayerData $() {
		return instance;
	}

	// Player name, List of skills to restore
	private final StrictMap<String, Set<String>> pendingSkillsToRestore = new StrictMap<>();

	public SimplePlayerData() {
		super("Player");

		loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return false;
	}

	@Override
	protected void onLoadFinish() {
		pendingSkillsToRestore.clear();

		final Object obj = getObject("Pending_Skill_Restore");

		if (obj != null) {
			final Map<String, Object> map = Common.getMapFromSection(obj);

			for (final Entry<String, Object> e : map.entrySet()) {
				final List<String> list = (List<String>) e.getValue();

				pendingSkillsToRestore.put(e.getKey(), new HashSet<>(list));
			}
		}
	}

	public Set<String> getSkillsToRestore(Player player) {
		return pendingSkillsToRestore.get(player.getName());
	}

	public void removeSkills(Player player) {
		final boolean updated = pendingSkillsToRestore.removeWeak(player.getName()) != null;

		if (updated)
			update();
	}

	public void addSkillToRestore(Player player, BossSkill skill) {
		Valid.checkNotNull(player, "Player for skill restore == null!");
		Valid.checkBoolean(!player.isOnline(), "Cannot schedule skill restore for online player " + player + "!");

		Set<String> skills = pendingSkillsToRestore.get(player.getName());

		if (skills == null)
			skills = new HashSet<>();

		skills.add(skill.getName());
		pendingSkillsToRestore.put(player.getName(), skills);

		update();
	}

	private void update() {
		save("Pending_Skill_Restore", pendingSkillsToRestore);
	}
}
