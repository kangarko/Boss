package org.mineacademy.boss.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class ModelEngineHook {

    @Getter
    private static final boolean available;

    static {
        available = Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
    }

    public static void applyModel(final Entity entity, final String modelName) {
        if(!available)
            return;

        final ActiveModel activeModel;

        try {
            activeModel = ModelEngineAPI.createActiveModel(modelName);
        } catch(RuntimeException exception) {
            return;
        }

        removeModel(entity, modelName);

        final ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);

        modeledEntity.setBaseEntityVisible(false);

        modeledEntity.addModel(activeModel, true);
    }

    public static void removeModel(final Entity entity, final String modelName) {
        if(!available)
            return;

        final ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);

        if(modeledEntity == null)
            return;

        modeledEntity.getModel(modelName).ifPresent(ActiveModel::destroy);

        modeledEntity.removeModel(modelName);

        modeledEntity.setBaseEntityVisible(true);
    }
}