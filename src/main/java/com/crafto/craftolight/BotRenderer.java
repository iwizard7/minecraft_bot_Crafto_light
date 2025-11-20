package com.crafto.craftolight;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BotRenderer extends MobRenderer<BotEntity, VillagerModel<BotEntity>> {
    public BotRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BotEntity entity) {
        return new ResourceLocation("minecraft", "textures/entity/villager/villager.png");
    }
}
