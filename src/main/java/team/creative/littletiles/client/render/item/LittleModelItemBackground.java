package team.creative.littletiles.client.render.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    protected ItemStack getFakeStack(ItemStack current, ItemDisplayContext context) {
        return getFakeStack(current);
    }

    protected ItemStack getQuadStack(ItemStack current) {
        return getFakeStack(current);
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

    protected boolean shouldRenderBackground(ItemStack stack, ItemDisplayContext context) {
        return true;
    }

    protected boolean shouldOverlayContentQuads(ItemStack stack, ItemDisplayContext context) {
        return false;
    }

    protected boolean shouldServeContentQuads(ItemStack stack) {
        return false;
    }

    protected BakedModel applyContentTransform(BakedModel model, ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        return model.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public CreativeBakedModel create(CreativeBlockModel block) {
        return new Baked(location, this);
    }

    private static class Baked extends CreativeBakedModel {
        private ItemStack transformedStack = ItemStack.EMPTY;
        private ItemDisplayContext transformedContext = ItemDisplayContext.NONE;
        private boolean renderBackground = true;

        public Baked(ModelResourceLocation location, LittleModelItemBackground item) {
            super(location, item);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
            transformedStack = renderedStack;
            transformedContext = transformType;
            renderBackground = renderedStack == null || ((LittleModelItemBackground) item).shouldRenderBackground(renderedStack, transformType);
            if (renderedStack != null && ((LittleModelItemBackground) item).shouldRenderContentDirectly(renderedStack, transformType)) {
                Minecraft mc = Minecraft.getInstance();
                BakedModel model = mc.getItemRenderer().getModel(((LittleModelItemBackground) item).getFakeStack(renderedStack, transformType), null, null, 0);
                return ((LittleModelItemBackground) item).applyContentTransform(model, transformType, poseStack, applyLeftHandTransform);
            }
            return super.applyTransform(transformType, poseStack, applyLeftHandTransform);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data,
                @Nullable RenderType renderType) {
            if (shouldOverlayContentQuads()) {
                List<BakedQuad> quads = new ArrayList<>();
                if (renderBackground)
                    quads.addAll(super.getQuads(state, side, rand, data, renderType));
                addContentQuads(quads, getContentQuads(state, side, rand, data, renderType));
                return quads;
            }
            if (!renderBackground && renderedStack == transformedStack)
                return Collections.emptyList();
            if (shouldServeContentQuads())
                return getContentQuads(state, side, rand, data, renderType);
            return super.getQuads(state, side, rand, data, renderType);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource source) {
            if (shouldOverlayContentQuads()) {
                List<BakedQuad> quads = new ArrayList<>();
                if (renderBackground)
                    quads.addAll(super.getQuads(state, direction, source));
                addContentQuads(quads, getContentQuads(state, direction, source));
                return quads;
            }
            if (!renderBackground && renderedStack == transformedStack)
                return Collections.emptyList();
            if (shouldServeContentQuads())
                return getContentQuads(state, direction, source);
            return super.getQuads(state, direction, source);
        }

        private boolean shouldServeContentQuads() {
            if (renderedStack == null || renderedStack.isEmpty() || renderedStack == transformedStack)
                return false;
            LittleModelItemBackground background = (LittleModelItemBackground) item;
            return background.shouldServeContentQuads(renderedStack) && !background.getQuadStack(renderedStack).isEmpty();
        }

        private boolean shouldOverlayContentQuads() {
            if (renderedStack == null || renderedStack.isEmpty() || renderedStack != transformedStack)
                return false;
            LittleModelItemBackground background = (LittleModelItemBackground) item;
            return background.shouldOverlayContentQuads(renderedStack, transformedContext) && !background.getFakeStack(renderedStack, transformedContext).isEmpty();
        }

        private List<BakedQuad> getContentQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data,
                @Nullable RenderType renderType) {
            Minecraft mc = Minecraft.getInstance();
            LittleModelItemBackground background = (LittleModelItemBackground) item;
            ItemStack content = shouldOverlayContentQuads() ? background.getFakeStack(renderedStack, transformedContext) : background.getQuadStack(renderedStack);
            BakedModel model = mc.getItemRenderer().getModel(content, null, null, 0);
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel pass : model.getRenderPasses(content, true))
                addContentQuads(quads, pass.getQuads(state, side, rand, data, renderType));
            return quads;
        }

        private List<BakedQuad> getContentQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            Minecraft mc = Minecraft.getInstance();
            LittleModelItemBackground background = (LittleModelItemBackground) item;
            ItemStack content = shouldOverlayContentQuads() ? background.getFakeStack(renderedStack, transformedContext) : background.getQuadStack(renderedStack);
            BakedModel model = mc.getItemRenderer().getModel(content, null, null, 0);
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel pass : model.getRenderPasses(content, true))
                addContentQuads(quads, pass.getQuads(state, side, rand));
            return quads;
        }

        private void addContentQuads(List<BakedQuad> target, List<BakedQuad> source) {
            target.addAll(source);
        }
    }
    
}
