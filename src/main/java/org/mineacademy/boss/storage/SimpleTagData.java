package org.mineacademy.boss.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlSectionConfig;

import lombok.Getter;

@Getter
public final class SimpleTagData extends YamlSectionConfig {

	private final static SimpleTagData instance = new SimpleTagData();

	public static SimpleTagData $() {
		return instance;
	}

	// Boss name, UUIDs
	private final StrictMap<String, Set<String>> spawnedBosses = new StrictMap<>();

	public SimpleTagData() {
		super("Database");

		loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
	}

	@Override
	protected void onLoadFinish() {
		spawnedBosses.clear();

		final SerializedMap map = getMap("Stored_Bosses");

		for (final Entry<String, Object> e : map.entrySet()) {
			final List<String> list = (List<String>) e.getValue();

			spawnedBosses.override(e.getKey(), new HashSet<>(list));
		}
	}

	public String getTag(final UUID id) {

		for (final Entry<String, Set<String>> e : spawnedBosses.entrySet())
			if (e.getValue().contains(id.toString()))
				return e.getKey();

		return null;
	}

	public void addTag(final UUID id, final String name) {
		Valid.checkBoolean(name != null && !name.isEmpty(), "The boss is lacking a name!");

		if (spawnedBosses.containsKey(name))
			spawnedBosses.get(name).add(id.toString());
		else
			spawnedBosses.put(name, Common.newSet(id.toString()));

		save("Stored_Bosses", spawnedBosses);
	}

	public void removeTagIfExists(final UUID id) {
		if (hasStored(id))
			removeTag(id);
	}

	public void clear() {
		boolean updated = false;

		final List<String> toRemoveNames = new ArrayList<>();

		for (final Entry<String, Set<String>> e : spawnedBosses.entrySet()) {
			final List<String> toRemove = new ArrayList<>();

			for (final String id : e.getValue()) {
				final Entity en = Remain.getEntity(UUID.fromString(id));

				if (en == null || !en.isValid() || en.isDead()) {
					toRemove.add(id);

					updated = true;
				}
			}

			e.getValue().removeAll(toRemove);

			if (e.getValue().isEmpty())
				toRemoveNames.add(e.getKey());
		}

		spawnedBosses.removeAll(toRemoveNames);

		if (updated)
			save("Stored_Bosses", spawnedBosses);
	}

	public void removeTag(final UUID id) {
		boolean updated = false;

		for (final Entry<String, Set<String>> e : spawnedBosses.entrySet())
			if (e.getValue().contains(id.toString())) {
				e.getValue().remove(id.toString());

				updated = true;
			}

		if (updated)
			save("Stored_Bosses", spawnedBosses);
	}

	public boolean hasStored(final UUID id) {
		return getTag(id) != null;
	}
}
