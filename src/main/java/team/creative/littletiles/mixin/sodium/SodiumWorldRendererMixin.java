package team.creative.littletiles.mixin.sodium;

import java.util.Objects;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import team.creative.littletiles.client.LittleTilesClient;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class SodiumWorldRendererMixin {

    @Inject(method = "drawChunkLayer", at = @At("TAIL"))
    public void renderSableAnimationLayer(RenderType renderType, ChunkRenderMatrices matrices, double camX, double camY, double camZ, CallbackInfo info) {
        if (LittleTilesClient.ANIMATION_HANDLER == null || !LittleTilesClient.ANIMATION_HANDLER.hasVanillaAnimationRenderers())
            return;

        Minecraft minecraft = Minecraft.getInstance();
        float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        Matrix4f modelView = new Matrix4f(matrices.modelView());
        Matrix4f projection = new Matrix4f(matrices.projection());

        renderLayer(renderType, modelView, projection, camX, camY, camZ, partialTicks, minecraft);

        for (RenderType layer : extraLayers(renderType))
            renderLayer(layer, modelView, projection, camX, camY, camZ, partialTicks, minecraft);
    }

    private static void renderLayer(RenderType renderType, Matrix4f modelView, Matrix4f projection, double camX, double camY, double camZ, float partialTicks, Minecraft minecraft) {
        renderType.setupRenderState();
        ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader(), "shader");
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, modelView, projection, minecraft.getWindow());
        shader.apply();

        LittleTilesClient.ANIMATION_HANDLER.renderSableVanillaChunkLayer(renderType, modelView, projection, camX, camY, camZ, partialTicks, shader);

        shader.clear();
        renderType.clearRenderState();
    }

    private static Iterable<RenderType> extraLayers(RenderType renderType) {
        try {
            Object unwrapped = renderType;
            while (unwrapped != null && unwrapped.getClass().getName().endsWith("VeilRenderType$RenderTypeWrapper"))
                unwrapped = unwrapped.getClass().getMethod("get").invoke(unwrapped);

            if (unwrapped != null && unwrapped.getClass().getName().endsWith("VeilRenderType$LayeredRenderType")) {
                Object layers = unwrapped.getClass().getMethod("getLayers").invoke(unwrapped);
                if (layers instanceof Iterable<?> iterable)
                    return () -> new java.util.Iterator<>() {

                        private final java.util.Iterator<?> iterator = iterable.iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public RenderType next() {
                            return (RenderType) iterator.next();
                        }
                    };
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {}
        return java.util.List.of();
    }
}
