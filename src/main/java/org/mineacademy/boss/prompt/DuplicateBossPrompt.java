package org.mineacademy.boss.prompt;

import org.bukkit.conversations.Conversation;
import org.bukkit.entity.Player;
import org.mineacademy.boss.menu.SelectBossMenu;
import org.mineacademy.boss.menu.boss.BossMenu;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.conversation.CreatePrompt;

/**
 * The prompt used to duplicate an existing Boss under a new name.
 */
public final class DuplicateBossPrompt extends CreatePrompt<Boss> {

	private final Boss source;

	private DuplicateBossPrompt(Boss source) {
		super("Boss");

		this.source = source;
	}

	@Override
	protected Boss create(String name) {
		return Boss.duplicateBoss(this.source, name);
	}

	@Override
	protected String findByName(String name) {
		final Boss boss = Boss.findBoss(name);

		return boss != null ? boss.getName() : null;
	}

	@Override
	protected void onCreateFinish(Player player, Boss createdItem) {
		BossMenu.showTo(SelectBossMenu.create(), player, createdItem);

		Messenger.success(player, "&7Boss &e" + this.source.getName() + " &7duplicated as &e" + createdItem.getName() + "&7.");
	}

	public static Conversation showTo(Player player, Boss source) {
		return new DuplicateBossPrompt(source).show(player);
	}
}
