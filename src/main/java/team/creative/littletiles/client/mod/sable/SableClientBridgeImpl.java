package team.creative.littletiles.client.mod.sable;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.level.IOrientatedLevel;
import team.creative.creativecore.common.util.mc.PlayerUtils;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTileContext;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.mod.sable.SableBridge.Context;

final class SableClientBridgeImpl {
    
    private SableClientBridgeImpl() {}
    
    static boolean isDynamicDirectionalShadingEnabled() {
        return SableDynamicDirectionalShading.isEnabled();
    }
    
    static void applyPoseToModelViewForBlockPos(Context context, BlockPos blockPos, Vec3 cam, float partialTick) {
        applyPoseToModelViewForPosition(context, blockPos.getX(), blockPos.getY(), blockPos.getZ(), cam, partialTick);
    }
    
    static void applyPoseToModelViewForPosition(Context context, double x, double y, double z, Vec3 cam, float partialTick) {
        ClientSubLevel subLevel = (ClientSubLevel) context.unwrap();
        Pose3dc pose = subLevel.renderPose(partialTick);
        if (pose == null)
            return;
        
        if (cam == null)
            cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 subCam = pose.transformPositionInverse(cam);
        Matrix4fStack matrix = RenderSystem.getModelViewStack();
        matrix.translate((float) (pose.position().x() - cam.x), (float) (pose.position().y() - cam.y), (float) (pose.position().z() - cam.z));
        matrix.rotate(new Quaternionf(pose.orientation()));
        matrix.translate((float) (subCam.x - pose.rotationPoint().x()), (float) (subCam.y - pose.rotationPoint().y()), (float) (subCam.z - pose.rotationPoint().z()));
        matrix.scale((float) pose.scale().x(), (float) pose.scale().y(), (float) pose.scale().z());
        matrix.translate((float) (x - subCam.x), (float) (y - subCam.y), (float) (z - subCam.z));
    }
    
    static void applyPoseToPoseStackForBlockPos(Context context, BlockPos blockPos, PoseStack stack, double camX, double camY, double camZ, float partialTick) {
        ClientSubLevel subLevel = (ClientSubLevel) context.unwrap();
        Pose3dc pose = subLevel.renderPose(partialTick);
        if (pose == null)
            return;
        
        Vec3 subCam = pose.transformPositionInverse(new Vec3(camX, camY, camZ));
        stack.translate(pose.position().x() - camX, pose.position().y() - camY, pose.position().z() - camZ);
        stack.mulPose(new Quaternionf(pose.orientation()));
        stack.translate(subCam.x - pose.rotationPoint().x(), subCam.y - pose.rotationPoint().y(), subCam.z - pose.rotationPoint().z());
        stack.scale((float) pose.scale().x(), (float) pose.scale().y(), (float) pose.scale().z());
        stack.translate(blockPos.getX() - subCam.x, blockPos.getY() - subCam.y, blockPos.getZ() - subCam.z);
    }
    
    static LittleTileContext selectFocusedWithRenderPose(Context context, BlockGetter level, BlockPos pos, Player player, float partialTick) {
        BETiles be = BlockTile.loadBE(level, pos);
        if (be == null)
            return null;
        
        Vec3 eye = player.getEyePosition(partialTick);
        double reach = PlayerUtils.getReach(player);
        Vec3 view = player.getViewVector(partialTick);
        Vec3 look = eye.add(view.x * reach, view.y * reach, view.z * reach);
        
        if (level != player.level() && level instanceof IOrientatedLevel orientated) {
            eye = orientated.getOrigin().transformPointToFakeWorld(eye);
            look = orientated.getOrigin().transformPointToFakeWorld(look);
        }
        
        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        Vec3 localEye = pose.transformPositionInverse(eye);
        Vec3 localLook = pose.transformPositionInverse(look);
        return be.getFocusedTile(localEye, localLook);
    }
    
    static boolean tryMarkDirty(Context context, BlockPos pos) {
        ClientSubLevel subLevel = (ClientSubLevel) context.unwrap();
        SubLevelRenderData renderData = subLevel.getRenderData();
        if (renderData == null)
            return false;
        
        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        Minecraft.getInstance().execute(() -> renderData.setDirty(sx, sy, sz, true));
        return true;
    }
}
