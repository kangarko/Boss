package org.mineacademy.boss.prompt;

import org.bukkit.conversations.Conversation;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.SelectBossMenu;
import org.mineacademy.boss.menu.boss.BossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.CreatePrompt;

/**
 * The prompt used to create new Bosses.
 */
public final class CreateBossPrompt extends CreatePrompt<Boss> {

	/**
	 * The boss type to create
	 */
	private final EntityType type;

	/*
	 * Create new prompt for the given entity type
	 */
	private CreateBossPrompt(EntityType type) {
		super("Boss");

		this.type = type;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#create()
	 */
	@Override
	protected Boss create(String name) {
		Valid.checkNotNull(this.type, "Prompt failed to carry Boss type");

		return Boss.createBoss(name, this.type);
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#findByName(java.lang.String)
	 */
	@Override
	protected String findByName(String name) {
		final Boss boss = Boss.findBoss(name);

		return boss != null ? boss.getName() : null;
	}

	/**
	 * @see org.mineacademy.boss.prompt.CreatePrompt#onCreateFinish(java.lang.Object, org.bukkit.entity.Player)
	 */
	@Override
	protected void onCreateFinish(Player player, Boss createdItem) {
		BossMenu.showTo(SelectBossMenu.create(), player, createdItem);

		Messenger.success(player, "&7Boss &e" + createdItem.getName() + " &7created.");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Show this prompt
	 *
	 * @param player
	 * @param type
	 * @return
	 */
	public static Conversation showTo(Player player, EntityType type) {
		return new CreateBossPrompt(type).show(player);
	}
}
