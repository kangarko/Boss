package org.mineacademy.boss.prompt;

import java.util.Arrays;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.SelectSpawnRuleMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.spawn.SpawnRule;
import org.mineacademy.boss.spawn.SpawnRuleType;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.CreatePrompt;
import org.mineacademy.fo.model.IsInList;

/**
 * Used to create new spawn rules
 */
public final class CreateSpawnRulePrompt extends CreatePrompt<SpawnRule> {

	/**
	 * The type
	 */
	private final SpawnRuleType type;

	/**
	 * The optional nullable Bossík for whom we create the rule automatically
	 */
	@Nullable
	private final Boss bossFor;

	/*
	 * Create a new spawn rule prompt
	 */
	private CreateSpawnRulePrompt(SpawnRuleType type, Boss bossFor) {
		super("Spawn Rule");

		this.type = type;
		this.bossFor = bossFor;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#create()
	 */
	@Override
	protected SpawnRule create(String name) {
		Valid.checkNotNull(this.type, "Prompt failed to carry SpawnRuleType");

		final SpawnRule rule = SpawnRule.createRule(name, this.type);

		if (this.bossFor != null)
			rule.setBosses(IsInList.fromList(Arrays.asList(this.bossFor.getName())));

		rule.save();
		return rule;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#findByName(java.lang.String)
	 */
	@Override
	protected String findByName(String name) {
		final SpawnRule rule = SpawnRule.findRule(name);

		return rule != null ? rule.getName() : null;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#allowSpaces()
	 */
	@Override
	protected boolean allowSpaces() {
		return true;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#onCreateFinish(java.lang.Object, org.bukkit.entity.Player)
	 */
	@Override
	protected void onCreateFinish(Player player, SpawnRule createdItem) {
		SelectSpawnRuleMenu.createSpawnRuleMenu(createdItem).displayTo(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Show this prompt to the player
	 * @param player
	 * @param type
	 * @param bossFor
	 */
	public static void showTo(Player player, SpawnRuleType type, @Nullable Boss bossFor) {
		new CreateSpawnRulePrompt(type, bossFor).show(player);
	}
}
