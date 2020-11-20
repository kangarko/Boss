package org.mineacademy.boss.model.skills;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.BossSkillDelay;
import org.mineacademy.boss.api.SpawnedBoss;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public final class SkillDisarm extends AbstractTargetSkill {

	@Override
	public String getName() {
		return "Disarm";
	}

	@Override
	public BossSkillDelay getDefaultDelay() {
		return new BossSkillDelay("1 minute", "2 minutes");
	}

	@Override
	public boolean execute(final Player target, final SpawnedBoss spawned) {
		final ItemStack hand = target.getItemInHand();

		if (hand != null && hand.getType() != Material.AIR) {

			final Item item = target.getWorld().dropItemNaturally(target.getLocation(), hand);

			item.setPickupDelay(2 * 20);

			target.getInventory().remove(hand);

		}

		sendSkillMessage(target, spawned);
		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[] {
				"You are &ctoo weak &7to hold this item!",
				"Your item in hand has &cfell off&7, guess who did that!",
				"The {boss} has &cdisarmed &7you!"
		};
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.of(
				CompMaterial.LEAD,
				"Disarm",
				"",
				"Throw away the item the",
				"player is holding in hand!")
				.build().make();
	}

	@Override
	public void readSettings(final Map<String, Object> map) {
	}

	@Override
	public Map<String, Object> writeSettings() {
		final Map<String, Object> map = new HashMap<>();
		return map;
	}
}
