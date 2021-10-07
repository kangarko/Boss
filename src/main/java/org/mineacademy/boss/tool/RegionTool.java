package org.mineacademy.boss.tool;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.BossPlugin;
import org.mineacademy.boss.model.BossPlayer;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualTool;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the tool used to create arena region for any arena
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionTool extends VisualTool {

	@Getter
	private static final Tool instance = new RegionTool();

	@Override
	protected String getBlockName(final Block block, final Player player) {
		return "[&aRegion point&f]";
	}

	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player) {
		return CompMaterial.EMERALD_BLOCK;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.EMERALD,
				"Region Tool",
				"",
				"Use this tool to create",
				"and edit Boss regions.",
				"",
				"&b<< &7Left click &7– &7Primary",
				"&7Right click &7– &7Secondary &b>>")
				.build().make();
	}

	@Override
	protected void handleBlockClick(final Player player, final ClickType click, final Block block) {
		final BossPlayer cache = BossPlugin.getDataFor(player);
		final VisualizedRegion region = cache.getCreatedRegion();

		final Location location = block.getLocation();
		final boolean primary = click == ClickType.LEFT;

		if (region != null)
			if (location.equals(region.getPrimary()) || location.equals(region.getSecondary())) {
				Messenger.success(player, "Removed the " + (primary ? "primary" : "secondary") + " region point.");

				if (location.equals(region.getPrimary()))
					region.setPrimary(null);
				else
					region.setSecondary(null);

				return;
			}

		cache.setCreatedRegionPoint(location, primary);

		Messenger.success(player, "Set the " + (primary ? "primary" : "secondary") + " region point.");
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(Player player) {
		final List<Location> blocks = new ArrayList<>();

		if (player != null) {
			final BossPlayer cache = BossPlugin.getDataFor(player);
			final Region region = cache.getCreatedRegion();

			if (region != null) {
				if (region.getPrimary() != null)
					blocks.add(region.getPrimary());

				if (region.getSecondary() != null)
					blocks.add(region.getSecondary());
			}
		}

		return blocks;
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getVisualizedRegion()
	 */
	@Override
	protected VisualizedRegion getVisualizedRegion(Player player) {
		if (player != null) {
			final BossPlayer cache = BossPlugin.getDataFor(player);
			final VisualizedRegion region = cache.getCreatedRegion();

			if (region != null && region.isWhole())
				return region;
		}

		return null;
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
