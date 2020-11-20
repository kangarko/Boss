package org.mineacademy.boss.tool;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectorTool extends Tool {

	@Getter
	private static final InspectorTool instance = new InspectorTool();

	private static final ItemStack item = ItemCreator.of(CompMaterial.STICK,
			"Inspector Tool",
			"",
			"Left click on a Boss",
			"to open his menu directly.")
			.glow(true)
			.build().make();

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public void onBlockClick(final PlayerInteractEvent e) {
		e.setCancelled(true);
	}

	@Override
	public boolean ignoreCancelled() {
		return false;
	}
}