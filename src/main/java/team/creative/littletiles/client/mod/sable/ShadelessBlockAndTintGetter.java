package team.creative.littletiles.client.mod.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

public final class ShadelessBlockAndTintGetter implements BlockAndTintGetter {
    
    private final BlockAndTintGetter inner;
    
    public ShadelessBlockAndTintGetter(BlockAndTintGetter inner) {
        this.inner = inner;
    }
    
    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1.0F;
    }
    
    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return inner.getBlockTint(pos, resolver);
    }
    
    @Override
    public LevelLightEngine getLightEngine() {
        return inner.getLightEngine();
    }
    
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return inner.getBlockEntity(pos);
    }
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        return inner.getBlockState(pos);
    }
    
    @Override
    public FluidState getFluidState(BlockPos pos) {
        return inner.getFluidState(pos);
    }
    
    @Override
    public int getHeight() {
        return inner.getHeight();
    }
    
    @Override
    public int getMinBuildHeight() {
        return inner.getMinBuildHeight();
    }
}
