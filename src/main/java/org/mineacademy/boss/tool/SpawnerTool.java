package org.mineacademy.boss.tool;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.boss.storage.SimpleSpawnerData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpawnerTool extends Tool {

	public static final String METADATA = "BossSpawner";
	public static final String METADATA_UNSPECIFIED = "unspecified";

	public static EntityType getUnspecifiedType() {
		return MinecraftVersion.olderThan(V.v1_13) ? EntityType.PIG : EntityType.EGG;
	}

	@Getter
	private static final SpawnerTool instance = new SpawnerTool();

	private static final ItemStack item = ItemCreator.of(CompMaterial.SPAWNER,
			"Boss Spawner",
			"",
			"Right click to place",
			"a new Boss spawner.")
			.glow(true)
			.build().make();

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public void onBlockClick(final PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		e.setCancelled(true);

		final Player player = e.getPlayer();

		if (!Remain.isPaper()) {
			Common.tell(player, "Boss spawners require Paper server software, as Spigot does not saves spawner data correctly. Upgrade for free at www.papermc.io.");

			return;
		}

		final Block block = e.getClickedBlock().getRelative(e.getBlockFace());

		block.setType(CompMaterial.SPAWNER.getMaterial());
		block.setMetadata(METADATA, new FixedMetadataValue(SimplePlugin.getInstance(), METADATA_UNSPECIFIED));

		final CreatureSpawner spawner = (CreatureSpawner) block.getState();

		spawner.setSpawnedType(getUnspecifiedType());
		spawner.update(true);

		if (player.getGameMode() != GameMode.CREATIVE)
			Remain.takeItemAndSetAsHand(player, player.getItemInHand());

		try {
			SimpleSpawnerData.$().addSpawner(block.getLocation(), METADATA_UNSPECIFIED);
		} catch (final Throwable throwable) {
			Debugger.saveError(throwable, "Couldn't save spawn-data!");
		}
	}
}