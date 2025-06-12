package org.mineacademy.boss.skill;

import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.model.BossSkillTag;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillDisarm extends AbstractTargetSkill {

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("1 minute - 2 minutes");
	}

	@Override
	public boolean execute(final Player target, final LivingEntity entity) {
		final ItemStack hand = target.getItemInHand();

		if (!CompMaterial.isAir(hand)) {
			final Item item = target.getWorld().dropItemNaturally(target.getLocation(), hand);

			// Prevent lightning and fireball from destroying the item
			BossSkillTag.IS_CANCELLING_DAMAGE_TO_VICTIM.set(item, true);
			BossSkillTag.IS_CANCELLING_COMBUSTION.set(item, true);

			item.setPickupDelay(2 * 20);

			target.getInventory().setItemInHand(null);

			this.sendSkillMessage(target, entity);
		}

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return Lang.plain("skill-disarm-default-message").split("\n");
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.LEAD,
				"Disarm",
				"",
				"Throw away the item the",
				"player is holding in hand!")
				.make();
	}
}
