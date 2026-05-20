package team.creative.littletiles.mixin.common.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.block.entity.BETiles;

@Pseudo
@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class SubLevelAssemblyHelperLTRotateMixin {
    
    @Inject(method = "moveBlocks", at = @At("RETURN"), require = 0)
    private static void littletiles$rotateTilesAfterSableMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks,
            CallbackInfo ci) {
        final Rotation rotation;
        try {
            rotation = transform.getRotation();
        } catch (Throwable t) {
            return;
        }
        if (rotation == null || rotation == Rotation.NONE)
            return;
        
        final Level destination;
        try {
            destination = transform.getLevel();
        } catch (Throwable t) {
            return;
        }
        if (destination == null)
            return;
        
        Rotation ltRotation = switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
        
        for (BlockPos source : blocks) {
            BlockPos target = transform.apply(source);
            BlockEntity be = destination.getBlockEntity(target);
            if (be instanceof BETiles tiles)
                try {
                    tiles.rotate(ltRotation);
                } catch (Throwable t) {
                    LittleTiles.LOGGER.warn("[sable] failed to rotate LT tiles at {} by {}", target, ltRotation, t);
                }
        }
    }
}
