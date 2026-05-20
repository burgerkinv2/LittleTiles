package team.creative.littletiles.mixin.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import team.creative.littletiles.common.mod.sable.SableBridge;

@Mixin(WorldBorder.class)
public class WorldBorderSableSubLevelMixin {
    
    @Inject(method = "isWithinBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void littletiles$sableSubLevelInBounds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level != null && SableBridge.isInSableSubLevel(level, pos))
                cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }
}
