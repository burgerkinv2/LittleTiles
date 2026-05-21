package team.creative.littletiles.client.mod.sable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.block.little.tile.LittleTileContext;
import team.creative.littletiles.common.mod.sable.SableBridge;
import team.creative.littletiles.common.mod.sable.SableBridge.Context;

public final class SableClientBridge {
    
    private SableClientBridge() {}
    
    public static boolean isDynamicDirectionalShadingEnabled() {
        if (!SableBridge.isAvailable())
            return false;
        try {
            return SableClientBridgeImpl.isDynamicDirectionalShadingEnabled();
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to read dynamic shading state: {}", t.toString());
            return true;
        }
    }
    
    public static void applyPoseToModelViewForBlockPos(Context context, BlockPos blockPos) {
        applyPoseToModelViewForBlockPos(context, blockPos, null, 1);
    }

    public static void applyPoseToModelViewForBlockPos(Context context, BlockPos blockPos, Vec3 cam, float partialTick) {
        if (!SableBridge.isAvailable() || context == null || blockPos == null)
            return;
        try {
            SableClientBridgeImpl.applyPoseToModelViewForBlockPos(context, blockPos, cam, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to apply model-view pose for {}: {}", blockPos, t.toString());
        }
    }
    
    public static void applyPoseToModelViewForPosition(Context context, double x, double y, double z) {
        applyPoseToModelViewForPosition(context, x, y, z, null, 1);
    }

    public static void applyPoseToModelViewForPosition(Context context, double x, double y, double z, Vec3 cam, float partialTick) {
        if (!SableBridge.isAvailable() || context == null)
            return;
        try {
            SableClientBridgeImpl.applyPoseToModelViewForPosition(context, x, y, z, cam, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to apply model-view pose for position [{}, {}, {}]: {}", x, y, z, t.toString());
        }
    }
    
    public static void applyPoseToPoseStackForBlockPos(Context context, BlockPos blockPos, PoseStack stack, double camX, double camY, double camZ) {
        applyPoseToPoseStackForBlockPos(context, blockPos, stack, camX, camY, camZ, 1);
    }

    public static void applyPoseToPoseStackForBlockPos(Context context, BlockPos blockPos, PoseStack stack, double camX, double camY, double camZ, float partialTick) {
        if (!SableBridge.isAvailable() || context == null || blockPos == null || stack == null)
            return;
        try {
            SableClientBridgeImpl.applyPoseToPoseStackForBlockPos(context, blockPos, stack, camX, camY, camZ, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to apply pose stack for {}: {}", blockPos, t.toString());
        }
    }
    
    public static LittleTileContext selectFocusedWithRenderPose(BlockGetter level, BlockPos pos, Player player, float partialTick) {
        if (!(level instanceof Level actualLevel) || player == null || pos == null || !SableBridge.isAvailable())
            return null;
        Context context = SableBridge.findContext(actualLevel, pos);
        if (context == null)
            return null;
        try {
            return SableClientBridgeImpl.selectFocusedWithRenderPose(context, level, pos, player, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to select focused tile with render pose at {}: {}", pos, t.toString());
            return null;
        }
    }
    
    public static boolean tryMarkDirty(Context context, Level level, BlockPos pos) {
        if (!SableBridge.isAvailable() || context == null || level == null || pos == null)
            return false;
        try {
            return SableClientBridgeImpl.tryMarkDirty(context, pos);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to mark dirty at {}: {}", pos, t.toString());
            return false;
        }
    }
}
