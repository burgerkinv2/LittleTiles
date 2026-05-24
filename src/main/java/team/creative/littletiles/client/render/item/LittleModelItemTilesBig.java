package team.creative.littletiles.client.render.item;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.model.CreativeBakedBoxModel;
import team.creative.creativecore.client.render.model.CreativeBakedModel;
import team.creative.creativecore.client.render.model.CreativeBlockModel;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public class LittleModelItemTilesBig extends LittleModelItemTiles {
    
    @Override
    public List<? extends RenderBox> getBoxes(ItemStack stack, boolean translucent) {
        List<? extends RenderBox> boxes = super.getBoxes(stack, translucent);
        var data = ILittleTool.getData(stack);
        if (!data.getBoolean(ItemLittleBlueprint.NO_ITEM_PREVIEW_SHRINK_KEY))
            LittleGroup.shrinkCubesToOneBlock(boxes);
        float extraScale = ItemLittleBlueprint.getItemModelExtraScale(data);
        if (extraScale != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_SCALE)
            LittleGroup.scaleCubesForItemPreview(boxes, extraScale);
        return boxes;
    }

    @Override
    public void applyCustomOpenGLHackery(PoseStack pose, ItemStack stack, ItemDisplayContext context) {
        float displayScale = ItemLittleBlueprint.getItemModelDisplayScale(ILittleTool.getData(stack));
        if (displayScale == ItemLittleBlueprint.DEFAULT_ITEM_MODEL_SCALE)
            return;
        pose.scale(displayScale, displayScale, displayScale);
    }

    @Override
    public CreativeBakedModel create(CreativeBlockModel block) {
        return new BakedItemTilesBig(this, block);
    }

    private static class BakedItemTilesBig extends CreativeBakedBoxModel {

        public BakedItemTilesBig(LittleModelItemTilesBig item, CreativeBlockModel block) {
            super(item.location, item, block);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
            getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
            if (renderedStack != null)
                item.applyCustomOpenGLHackery(poseStack, renderedStack, transformType);
            return this;
        }

    }
    
}
