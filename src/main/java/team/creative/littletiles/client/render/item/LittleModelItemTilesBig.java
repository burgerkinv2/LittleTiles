package team.creative.littletiles.client.render.item;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
        return boxes;
    }

    @Override
    public void applyCustomOpenGLHackery(PoseStack pose, ItemStack stack, ItemDisplayContext context) {
        if (context == ItemDisplayContext.NONE)
            return;
        var data = ILittleTool.getData(stack);
        float offsetX = ItemLittleBlueprint.getItemModelOffsetX(data);
        float offsetY = ItemLittleBlueprint.getItemModelOffsetY(data);
        float offsetZ = ItemLittleBlueprint.getItemModelOffsetZ(data);
        float rotationX = ItemLittleBlueprint.getItemModelRotationX(data);
        float rotationY = ItemLittleBlueprint.getItemModelRotationY(data);
        float rotationZ = ItemLittleBlueprint.getItemModelRotationZ(data);
        float displayScale = ItemLittleBlueprint.getItemModelDisplayScale(data);

        if (offsetX != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_OFFSET || offsetY != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_OFFSET || offsetZ != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_OFFSET)
            pose.translate(offsetX, offsetY, offsetZ);
        if (rotationX != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_ROTATION)
            pose.mulPose(Axis.XP.rotationDegrees(rotationX));
        if (rotationY != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_ROTATION)
            pose.mulPose(Axis.YP.rotationDegrees(rotationY));
        if (rotationZ != ItemLittleBlueprint.DEFAULT_ITEM_MODEL_ROTATION)
            pose.mulPose(Axis.ZP.rotationDegrees(rotationZ));
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
