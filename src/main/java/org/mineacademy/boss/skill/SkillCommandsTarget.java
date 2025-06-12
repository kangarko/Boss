package org.mineacademy.boss.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkillCommandsTarget extends AbstractTargetSkill {

	@Override
	public RangedSimpleTime getDefaultDelay() {
		return RangedSimpleTime.fromString("30 seconds - 1 minute");
	}

	@Override
	public boolean execute(Player target, LivingEntity entity) {
		this.sendSkillMessage(target, entity);

		return true;
	}

	@Override
	public String[] getDefaultMessage() {
		return new String[0];
	}

	@Override
	public void readSettings(SerializedMap map) {
	}

	@Override
	public SerializedMap writeSettings() {
		return new SerializedMap();
	}

	@Override
	public ItemStack getIcon() {
		return ItemCreator.from(
				CompMaterial.CHAIN_COMMAND_BLOCK,
				"Commands For Target",
				"",
				"This is an empty skill you",
				"can program to run custom",
				"commands for the player the",
				"Boss is targeting.")
				.make();
	}
}
