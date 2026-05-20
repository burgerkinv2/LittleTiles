package team.creative.littletiles.client.mod.sable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.model.BakedQuad;

public final class SubLevelNormalConsumer implements VertexConsumer {
    
    private final VertexConsumer delegate;
    private boolean verticalNormal;
    
    public SubLevelNormalConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }
    
    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r, g, b, a);
        return this;
    }
    
    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }
    
    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }
    
    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }
    
    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        if (verticalNormal)
            delegate.setNormal(0F, 1F, 0F);
        else
            delegate.setNormal(x, y, z);
        return this;
    }
    
    @Override
    public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lightmap, int overlay, boolean packOverlay) {
        verticalNormal = !quad.isShade();
        VertexConsumer.super.putBulkData(pose, quad, brightness, r, g, b, a, lightmap, overlay, packOverlay);
        verticalNormal = false;
    }
}
