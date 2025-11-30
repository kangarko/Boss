package org.mineacademy.boss.hook;

import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import lombok.Getter;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.fo.remain.CompMetadata;

public class ModelEngineHook {

	private static final String CUSTOM_MODEL_METADATA = "ModelEngine_CustomModel";

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

		CompMetadata.setTempMetadata(entity, CUSTOM_MODEL_METADATA, modelName);
	}

	public static String getCurrentCustomModel(final Entity entity) {
		if (!available)
			return null;

		final MetadataValue tempMetadata = CompMetadata.getTempMetadata(entity, CUSTOM_MODEL_METADATA);

		return tempMetadata != null ? tempMetadata.asString() : null;
	}

	public static void playAnimation(final Entity entity, final String animationName) {
		if (!available || entity == null || animationName == null || animationName.isEmpty())
			return;

		final ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);

		if (modeledEntity == null)
			return;

		final String preferredModel = getCurrentCustomModel(entity);

		modeledEntity.getModels().forEach((name, model) -> {
			if (preferredModel == null || preferredModel.equalsIgnoreCase(name)) {
				final AnimationHandler handler = model.getAnimationHandler();

				if (handler != null)
					handler.playAnimation(animationName, 0.3, 0.3, 1, true);
			}
		});
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