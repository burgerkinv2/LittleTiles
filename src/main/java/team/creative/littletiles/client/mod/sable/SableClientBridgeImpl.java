package team.creative.littletiles.client.mod.sable;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
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

    static Vec3 transformCameraToSubLevelLocal(Context context, Vec3 cam, float partialTick) {
        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        return pose == null ? null : pose.transformPositionInverse(cam);
    }

    static Vec3 transformPointToSubLevelRenderLocal(Context context, Vec3 point, float partialTick) {
        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        return pose == null ? null : pose.transformPositionInverse(point);
    }

    static boolean applyRenderRotation(Context context, PoseStack stack, float partialTick) {
        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        if (pose == null)
            return false;

        stack.mulPose(new Quaternionf(pose.orientation()));
        return true;
    }
    
    static Vec3 setupAnimationRenderPose(Context context, PoseStack stack, Vec3 cam, BlockPos renderOrigin, float partialTick) {
        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        if (pose == null)
            return null;

        Vector3d renderPos = new Vector3d(pose.position());
        Vector3d renderCOR = pose.orientation().transform(new Vector3d(pose.rotationPoint()).sub(renderOrigin.getX(), renderOrigin.getY(), renderOrigin.getZ()));
        renderPos.sub(renderCOR);
        Vector3d fogOffset = new Vector3d(renderPos.x - cam.x, renderPos.y - cam.y, renderPos.z - cam.z);
        Vector3d fogOffsetRot = pose.orientation().transformInverse(fogOffset, new Vector3d());

        stack.mulPose(new Quaternionf(pose.orientation()));
        return new Vec3(renderOrigin.getX() - fogOffsetRot.x, renderOrigin.getY() - fogOffsetRot.y, renderOrigin.getZ() - fogOffsetRot.z);
    }

    static boolean setupVanillaRenderShader(Context context, ShaderInstance shader, boolean upload) {
        if (shader.FOG_SHAPE != null && RenderSystem.getShaderFogShape() != FogShape.SPHERE) {
            shader.FOG_SHAPE.set(FogShape.SPHERE.getIndex());
            if (upload)
                shader.FOG_SHAPE.upload();
        }

        VanillaSubLevelRenderDispatcher.setupDynamicEffects(shader, true, upload);

        Uniform sableSkyLightScale = shader.getUniform("SableSkyLightScale");
        if (sableSkyLightScale != null) {
            int skyLight = ((ClientSubLevel) context.unwrap()).getLatestSkyLightScale();
            sableSkyLightScale.set(skyLight / 15.0F);
            if (upload)
                sableSkyLightScale.upload();
        }

        return true;
    }

    static boolean resetVanillaRenderShader(ShaderInstance shader, boolean upload) {
        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
            if (upload)
                shader.FOG_SHAPE.upload();
        }

        VanillaSubLevelRenderDispatcher.setupDynamicEffects(shader, false, upload);
        return true;
    }

    static LittleTileContext selectFocusedWithRenderPose(Context context, BlockGetter level, BlockPos pos, Player player, float partialTick) {
        BETiles be = BlockTile.loadBE(level, pos);
        if (be == null)
            return null;

        Vec3 eye = player.getEyePosition(partialTick);
        double reach = PlayerUtils.getReach(player);
        Vec3 view = player.getViewVector(partialTick);
        Vec3 look = eye.add(view.x * reach, view.y * reach, view.z * reach);

        Pose3dc pose = ((ClientSubLevel) context.unwrap()).renderPose(partialTick);
        if (pose == null)
            return null;

        eye = pose.transformPositionInverse(eye);
        look = pose.transformPositionInverse(look);

        if (level != player.level() && level instanceof IOrientatedLevel orientated) {
            eye = orientated.getOrigin().transformPointToFakeWorld(eye);
            look = orientated.getOrigin().transformPointToFakeWorld(look);
        }

        return be.getFocusedTile(eye, look);
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
