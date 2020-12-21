package org.mineacademy.boss.util;

import static org.mineacademy.boss.util.Constants.NBT.TAG;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.nbt.NBTCompound;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BossNBTUtil {

	// -----------------------------------------------------------------------------
	// Read
	// -----------------------------------------------------------------------------

	public String readBossName(final ItemStack egg) {
		if (egg == null || CompMaterial.isAir(egg.getType()))
			return null;

		final NBTItem nbt = new NBTItem(egg);

		return nbt.hasKey(TAG) ? nbt.getCompound(TAG).getString("name") : null;
	}

	// -----------------------------------------------------------------------------
	// Write
	// -----------------------------------------------------------------------------

	public ItemStack writeBossName(final ItemStack stack, final Boss boss) {
		Valid.checkNotNull(boss, "Boss = null");

		return setNbtString(stack, "name", boss.getName());
	}

	private ItemStack setNbtString(final ItemStack item, final String key, final String name) {
		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.addCompound(TAG);

		tag.setString(key, name);
		return nbt.getItem();
	}
}