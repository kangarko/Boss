package org.mineacademy.boss.tool;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Permissions;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tool to tame a Boss to you.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BossTamerTool extends Tool {

	/**
	 * The singleton
	 */
	@Getter
	private static final BossTamerTool instance = new BossTamerTool();

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.from(
				CompMaterial.ARMOR_STAND,
				"Tamer Tool",
				"",
				"Right click any tameable",
				"entity to edit its owner",
				"as you &6&l>>")
				.glow(true)
				.make();
	}

	@Override
	public void onEntityRightClick(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof LivingEntity))
			return;

		final Player player = event.getPlayer();
		final LivingEntity entity = (LivingEntity) event.getRightClicked();
		final String entityName = Remain.getEntityName(entity);

		if (!Valid.checkPermission(player, Permissions.Use.INSPECT))
			return;

		if (!(entity instanceof Tameable)) {
			Messenger.error(player, Lang.component("tool-tamer-not-applicable", "entity_name", entityName));

			return;
		}

		final Tameable tameable = (Tameable) entity;

		if (player.isConversing()) {
			Messenger.error(player, Lang.component("conversation-already-conversing", "player", player.getName()));

			return;
		}

		new SimpleStringPrompt(Lang.legacy("tool-tamer-prompt", "current_owner", tameable.getOwner() == null ? Lang.plain("part-none") : tameable.getOwner().getName())) {

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				if ("none".equals(input))
					return true;

				final OfflinePlayer newOwner = Bukkit.getOfflinePlayer(input);

				if (newOwner == null || !newOwner.hasPlayedBefore()) {
					Messenger.error(player, Lang.component("player-not-played-before", "player", input));

					return false;
				}

				return super.isInputValid(context, input);
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				if ("none".equals(input)) {
					tameable.setOwner(null);

					Messenger.success(player, Lang.component("tool-tamer-success-reset", "entity_name", entityName));

				} else {
					final OfflinePlayer newOwner = Bukkit.getOfflinePlayer(input);
					tameable.setOwner(newOwner);

					Messenger.success(player, Lang.component("tool-tamer-success",
							"entity_name", entityName,
							"new_owner", newOwner.getName()));
				}
			}

		}.show(player);
	}

	@Override
	protected boolean autoCancel() {
		return true;
	}
}
