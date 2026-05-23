package team.creative.littletiles.client.render.item;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.client.render.model.CreativeBakedModel;
import team.creative.creativecore.client.render.model.CreativeBlockModel;
import team.creative.creativecore.client.render.model.CreativeItemModel;

@OnlyIn(Dist.CLIENT)
public class LittleModelItemBackground extends CreativeItemModel {
    
    private final Function<ItemStack, ItemStack> top;
    
    public LittleModelItemBackground(ModelResourceLocation location, Function<ItemStack, ItemStack> top) {
        super(location);
        this.top = top;
    }
    
    protected ItemStack getFakeStack(ItemStack current) {
        return top.apply(current);
    }
    
    @Override
    public void applyCustomOpenGLHackery(PoseStack pose, ItemStack stack, ItemDisplayContext cameraTransformType) {
        if (cameraTransformType == ItemDisplayContext.GUI) {
            
            ItemStack toFake = getFakeStack(stack);
            
            if (toFake.isEmpty())
                return;
            
            pose.pushPose();
            
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getItemRenderer().getModel(toFake, null, null, 0);
            
            prepareRenderer(cameraTransformType, pose);
            
            MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
            mc.getItemRenderer().render(toFake, cameraTransformType, false, pose, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, model);
            multibuffersource$buffersource.endBatch();
            
            pose.popPose();
        }
    }
    
    public void prepareRenderer(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI)
            pose.translate(0, 0, 1);
    }

    protected boolean shouldRenderContentDirectly(ItemStack stack, ItemDisplayContext context) {
        return isContentOnlyContext(context) && !getFakeStack(stack).isEmpty();
    }

    protected boolean isContentOnlyContext(ItemDisplayContext context) {
        return context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.GROUND;
    }

    @Override
    public CreativeBakedModel create(CreativeBlockModel block) {
        return new Baked(location, this);
    }

    private static class Baked extends CreativeBakedModel {

        public Baked(ModelResourceLocation location, LittleModelItemBackground item) {
            super(location, item);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
            if (renderedStack != null && ((LittleModelItemBackground) item).shouldRenderContentDirectly(renderedStack, transformType)) {
                Minecraft mc = Minecraft.getInstance();
                BakedModel model = mc.getItemRenderer().getModel(((LittleModelItemBackground) item).getFakeStack(renderedStack), null, null, 0);
                return model.applyTransform(transformType, poseStack, applyLeftHandTransform);
            }
            return super.applyTransform(transformType, poseStack, applyLeftHandTransform);
        }
    }
    
}
