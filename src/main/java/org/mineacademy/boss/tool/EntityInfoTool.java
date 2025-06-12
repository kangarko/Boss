package org.mineacademy.boss.tool;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.Permissions;
import org.mineacademy.boss.model.SpawnedBoss;
import org.mineacademy.boss.settings.Settings;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tool to get info about an entity
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityInfoTool extends Tool {

	/**
	 * The singleton
	 */
	@Getter
	private static final EntityInfoTool instance = new EntityInfoTool();

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.from(
				CompMaterial.STICK,
				"Info Tool",
				"",
				"Right click any entity",
				"to print its info &6&l>>")
				.glow(true)
				.make();
	}

	@Override
	public void onEntityRightClick(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof LivingEntity))
			return;

		final Player player = event.getPlayer();
		final LivingEntity entity = (LivingEntity) event.getRightClicked();

		if (!Valid.checkPermission(player, Permissions.Use.INSPECT))
			return;

		final SpawnedBoss spawnedBoss = Boss.findBoss(entity);
		final String uuid = entity.getUniqueId().toString();

		if (spawnedBoss == null) {
			Messenger.error(player, SimpleComponent
					.fromMiniAmpersand("This " + ChatUtil.capitalizeFully(entity.getType()) + " is not a Boss. Click ")

					.appendMiniAmpersand("<u>here</u>")
					.onHoverLegacy("&7UUID: &f" + uuid,
							"Click to copy.")
					.onClickSuggestCmd(uuid)

					.appendAmpersand(" to copy its UUID. Click ")

					.appendMiniAmpersand("<u>here</u>")
					.onHoverLegacy(
							"&c&lExplanation:",
							"Boss did not find an NBT tag we",
							"recognize. Click to dump all",
							"tags in the chat.")
					.onClickRunCmd("/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " uid nbt " + uuid)

					.appendMiniAmpersand(" to dump NBT tags."));

			event.setCancelled(true);
			return;
		}

		// Silently ignore players without the inspect permission to prevent spam
		if (!player.hasMetadata("ShowingBossInfo"))
			this.showBossInfo(player, entity, spawnedBoss.getBoss());
	}

	private void showBossInfo(Player player, Entity entity, Boss boss) {
		Messenger.info(player, SimpleComponent
				.fromMiniAmpersand("Found ")
				.appendMiniAmpersand("&n" + boss.getAlias() + "&r&7")
				.onHoverLegacy("&7Boss file: &f" + boss.getFile(),
						"&6Click to open Boss menu.")
				.onClickRunCmd("/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " menu " + boss.getName())

				.appendMiniAmpersand(". ")

				.appendMiniAmpersand("&nClick here&r&7")
				.onHoverLegacy("&7UUID: &f" + entity.getUniqueId(),
						"&6Click to copy.")
				.onClickSuggestCmd(entity.getUniqueId().toString())

				.appendMiniAmpersand(" to copy UUID and ")

				.appendMiniAmpersand("&nclick here&r&7")
				.onHoverLegacy("&6Click to dump NBT tag to console.")
				.onClickRunCmd("/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " uid nbt " + entity.getUniqueId().toString())

				.appendMiniAmpersand(" to dump its NBT tag to the console."));

		// Fix Spigot bug where interact event would fire right after and let us spawn the entity anyways
		CompMetadata.setTempMetadata(player, "ShowingBossInfo");
		Platform.runTask(2, () -> CompMetadata.removeTempMetadata(player, "ShowingBossInfo"));
	}

	@Override
	protected boolean autoCancel() {
		return true;
	}
}
