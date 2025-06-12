package org.mineacademy.boss.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillCommands extends BossSkill {

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(LivingEntity entity) {
		this.executeSkillCommands(null, entity);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[0];
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.COMMAND_BLOCK,
				"Commands",
				"",
				"This is an empty skill you",
				"can program to run custom",
				"commands in the given interval.",
				"",
				"&cNote: This skill has no",
				"&ctarget player, so you cannot",
				"&cuse 'tell' or {player} variable.")
				.make();
	}
}
