package team.creative.littletiles.mixin.client.render;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import team.creative.littletiles.client.render.cache.buffer.ChunkBufferUploader;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements ChunkBufferUploader {
    
    @Final
    @Shadow
    private ByteBufferBuilder buffer;
    
    @Final
    @Shadow
    private VertexFormat format;
    
    @Shadow
    private int vertices;
    
    @Shadow
    private long vertexPointer = -1L;
    
    @Shadow
    private int vertexSize;

    @Unique
    private int ltPendingVertices;
    
    @Override
    public int uploadIndex() {
        return ((ByteBufferBuilderAccessor) buffer).getWriteOffset();
    }
    
    @Override
    public void upload(ByteBuffer buffer) {
        this.ensureBuilding();

        int ltVertices = buffer.limit() / vertexSize;
        long pointer = this.buffer.reserve(buffer.limit());
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), pointer, buffer.limit());

        if (this.vertices == 0)
            this.vertexPointer = pointer;

        this.ltPendingVertices += ltVertices;
    }

    @Inject(method = "build", at = @At("HEAD"))
    private void onBuildHead(CallbackInfoReturnable<MeshData> cir) {
        if (this.ltPendingVertices > 0) {
            this.vertices += this.ltPendingVertices;
            this.ltPendingVertices = 0;
        }
    }

    @Inject(method = "build", at = @At("TAIL"))
    private void onBuildTail(CallbackInfoReturnable<MeshData> cir) {
        try {
            java.lang.reflect.Field field = BufferBuilder.class.getDeclaredField("iris$vertexCount");
            field.setAccessible(true);
            field.setInt(this, 0);
        } catch (Exception ignored) {}
    }
    
    @Shadow
    public abstract void ensureBuilding();
    
    @Shadow
    public abstract void endLastVertex();
    
    @Override
    public boolean hasFacingSupport() {
        return false;
    }
    
    @Override
    public int uploadIndex(int facing) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void upload(int facing, ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addTexture(TextureAtlasSprite texture) {}
    
}
