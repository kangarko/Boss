package org.mineacademy.boss.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import lombok.Getter;

public class ModelEngineHook {

	@Getter
	private static final boolean available;

	static {
		available = Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
	}

	public static void applyModel(final Entity entity, final String modelName) {
		if (!available)
			return;

		final ActiveModel activeModel;

		try {
			activeModel = ModelEngineAPI.createActiveModel(modelName);
		} catch (final RuntimeException exception) {
			return;
		}

		removeAllModels(entity);

		final ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);

		modeledEntity.setBaseEntityVisible(false);

		modeledEntity.addModel(activeModel, true);
	}

	public static void removeAllModels(final Entity entity) {
		if (!available)
			return;

		final ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);

		if (modeledEntity == null)
			return;

		modeledEntity.getModels().forEach((name, model) -> {
			model.destroy();
			modeledEntity.removeModel(name);
		});

		modeledEntity.setBaseEntityVisible(true);
	}
}