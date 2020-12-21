package org.mineacademy.boss.util;

import java.util.List;

import org.bukkit.entity.Entity;
import org.mineacademy.boss.api.Boss;
import org.mineacademy.boss.api.BossRegionSettings;
import org.mineacademy.boss.api.BossRegionType;
import org.mineacademy.boss.storage.SimpleTagData;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import lombok.experimental.UtilityClass;

/**
 * The identification of already spawned bosses
 */

@UtilityClass
public final class BossTaggingUtil {

	public void setTag(final Entity en, final Boss boss) {
		if (Remain.hasScoreboardTags())
			CompMetadata.setMetadata(en, Constants.NBT.TAG, boss.getName());
		else
			SimpleTagData.$().addTag(en.getUniqueId(), boss.getName());

		setTagKeepInside(en, boss);
	}

	private void setTagKeepInside(final Entity en, final Boss boss) {
		final List<String> regions = BossRegionType.BOSS.findRegions(en.getLocation());

		if (regions != null)
			for (final String name : regions) {
				final BossRegionSettings rg = boss.getSpawning().getRegions().findRegion(BossRegionType.BOSS, name);

				if (rg != null && rg.getKeepInside()) {
					CompMetadata.setMetadata(en, Constants.NBT.KEEP_INSIDE, name);

					return;
				}
			}
	}

	public String getTag(final Entity en) {
		if (Remain.hasScoreboardTags())
			return CompMetadata.getMetadata(en, Constants.NBT.TAG);

		return SimpleTagData.$().getTag(en.getUniqueId());
	}

	// Returns Boss Region Name
	public String getTagKeepInside(final Entity en) {
		return CompMetadata.getMetadata(en, Constants.NBT.KEEP_INSIDE);
	}
}
