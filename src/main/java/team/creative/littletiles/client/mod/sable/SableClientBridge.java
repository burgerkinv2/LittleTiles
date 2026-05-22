package team.creative.littletiles.client.mod.sable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.level.ISubLevel;
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

    public static Vec3 transformCameraToSubLevelLocal(Context context, Vec3 cam, float partialTick) {
        if (!SableBridge.isAvailable() || context == null || cam == null)
            return null;
        try {
            return SableClientBridgeImpl.transformCameraToSubLevelLocal(context, cam, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to transform render camera: {}", t.toString());
            return null;
        }
    }

    public static boolean applyRenderRotation(Context context, PoseStack stack, float partialTick) {
        if (!SableBridge.isAvailable() || context == null || stack == null)
            return false;
        try {
            return SableClientBridgeImpl.applyRenderRotation(context, stack, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to apply render rotation: {}", t.toString());
            return false;
        }
    }
    
    public static Vec3 setupAnimationRenderPose(Context context, PoseStack stack, Vec3 cam, BlockPos renderOrigin, float partialTick) {
        if (!SableBridge.isAvailable() || context == null || stack == null || cam == null || renderOrigin == null)
            return null;
        try {
            return SableClientBridgeImpl.setupAnimationRenderPose(context, stack, cam, renderOrigin, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to setup animation render pose: {}", t.toString());
            return null;
        }
    }

    public static boolean setupVanillaRenderShader(Context context, ShaderInstance shader, boolean upload) {
        if (!SableBridge.isAvailable() || context == null || shader == null)
            return false;
        try {
            return SableClientBridgeImpl.setupVanillaRenderShader(context, shader, upload);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to setup vanilla render shader: {}", t.toString());
            return false;
        }
    }

    public static boolean resetVanillaRenderShader(ShaderInstance shader, boolean upload) {
        if (!SableBridge.isAvailable() || shader == null)
            return false;
        try {
            return SableClientBridgeImpl.resetVanillaRenderShader(shader, upload);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to reset vanilla render shader: {}", t.toString());
            return false;
        }
    }

    public static LittleTileContext selectFocusedWithRenderPose(BlockGetter level, BlockPos pos, Player player, float partialTick) {
        if (!(level instanceof Level actualLevel) || player == null || pos == null || !SableBridge.isAvailable())
            return null;
        Context context = findRenderContext(actualLevel, pos);
        if (context == null)
            return null;
        try {
            return SableClientBridgeImpl.selectFocusedWithRenderPose(context, level, pos, player, partialTick);
        } catch (Throwable t) {
            LittleTiles.LOGGER.debug("[sable] failed to select focused tile with render pose at {}: {}", pos, t.toString());
            return null;
        }
    }

    public static Context findRenderContext(Level level, BlockPos pos) {
        if (!SableBridge.isAvailable() || level == null || pos == null)
            return null;
        Context context = SableBridge.findContext(level, pos);
        if (context != null)
            return context;
        if (level instanceof ISubLevel subLevel) {
            Entity holder = subLevel.getHolder();
            if (holder != null)
                return SableBridge.findContext(holder.level(), holder.blockPosition());
        }
        return null;
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
