package org.mineacademy.boss.tool;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossLocation;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualTool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Tool to create location points
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocationTool extends VisualTool {

	/**
	 * The singleton
	 */
	@Getter
	private static final LocationTool instance = new LocationTool();

	@Override
	public String getBlockName(final Block block, final Player player) {
		return "&f[&6Location " + BossLocation.findLocation(block.getLocation()).getName() + "&f]";
	}

	@Override
	public CompMaterial getBlockMask(final Block block, final Player player) {
		return CompMaterial.GOLD_BLOCK;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.from(
				CompMaterial.GOLDEN_AXE,
				"Location Tool",
				"",
				"Use this tool to create",
				"locations for Boss to spawn.",
				"",
				"Click a block to &acreate",
				"or &cremove &7a location point.")
				.make();
	}

	/**
	 * Simulate a click on the block
	 *
	 * @param player
	 * @param primary
	 * @param location
	 */
	public void simulateClick(final Player player, final boolean primary, final Location location) {
		this.onBlockClick(player, primary ? ClickType.LEFT : ClickType.RIGHT, location.getBlock());
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final ClickType click, final Block block) {

		final Location location = block.getLocation();

		if (player.isConversing()) {
			Messenger.error(player, "You have a pending chat conversation, answer it before setting a location.");

			return;
		}

		final BossLocation oldLocation = BossLocation.findLocation(location);

		if (oldLocation != null) {
			Messenger.success(player, "Location '" + oldLocation.getName() + "' has been &cremoved.");

			BossLocation.removeLocation(oldLocation);
		} else
			new SimpleStringPrompt("&nEnter location name to chat&r&7, using English-only alphabet.") {

				@Override
				protected boolean isInputValid(ConversationContext context, String input) {
					return Valid.isInRange(input.length(), 3, 24) && !BossLocation.isLocationLoaded(input);
				}

				@Override
				protected String getFailedValidationText(org.bukkit.conversations.ConversationContext context, String invalidInput) {
					if (BossLocation.isLocationLoaded(invalidInput))
						return "Location '" + invalidInput + "' already exists! Enter a unique name";

					return "Invalid input '" + invalidInput + "'! Enter an English-only name 3-24 letters long";
				}

				@Override
				protected void onValidatedInput(ConversationContext context, String input) {
					BossLocation.createLocation(input, location);

					this.tell(Messenger.getSuccessPrefix().appendMiniAmpersand("Location '&e" + input + "&7' has been created!"));

					// Trick to refresh visualized points
					LocationTool.this.onHotbarDefocused(player);
					LocationTool.this.onHotbarFocused(player);
				}

			}.show(player);
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getVisualizedPoints(org.bukkit.entity.Player)
	 */
	@Override
	protected List<Location> getVisualizedPoints(@NonNull Player player) {
		return BossLocation.getBukkitLocations();
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#autoCancel()
	 */
	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.BlockTool#ignoreCancelled()
	 */
	@Override
	protected boolean ignoreCancelled() {
		return true;
	}
}
