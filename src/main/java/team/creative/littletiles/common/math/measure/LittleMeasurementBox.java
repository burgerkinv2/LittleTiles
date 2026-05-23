package team.creative.littletiles.common.math.measure;

import java.util.List;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.client.render.overlay.OverlayRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;
import team.creative.littletiles.common.math.measure.LittleMeasurement.LittleMeasurementSimple;

public class LittleMeasurementBox extends LittleMeasurementSimple {

    private static final double LABEL_OFFSET = 0.06;

    protected LittleBoxAbsolute box;

    public LittleMeasurementBox(CompoundTag nbt) {
        super(nbt);
    }

    public LittleMeasurementBox(List<LittleBoxAbsolute> positions) {
        super(positions);
    }

    @Override
    public void changed() {
        super.changed();
        box = first.copy();
        box.include(second);
    }

    @Override
    public void overlay(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam, String unit) {
        var bb = box.toAABB();
        if (!renderer.isVisible(bb))
            return;

        double xLen = bb.maxX - bb.minX;
        double yLen = bb.maxY - bb.minY;
        double zLen = bb.maxZ - bb.minZ;
        if (xLen <= 0 || yLen <= 0 || zLen <= 0)
            return;

        var player = renderer.player();
        if (player == null)
            return;
        Vec3 eye = player.getEyePosition();
        double cx = (bb.minX + bb.maxX) * 0.5;
        double cy = (bb.minY + bb.maxY) * 0.5;
        double cz = (bb.minZ + bb.maxZ) * 0.5;

        boolean nearMinY = eye.y < cy;
        boolean nearMinZ = eye.z < cz;
        boolean nearMinX = eye.x < cx;
        int displayColor = color != 0 ? color : ColorUtils.WHITE;

        Vec3d xPos = new Vec3d(cx, bbY(bb, nearMinY), bbZ(bb, nearMinZ));
        Vec3d yPos = new Vec3d(bbX(bb, nearMinX), cy, bbZ(bb, nearMinZ));
        Vec3d zPos = new Vec3d(bbX(bb, nearMinX), bbY(bb, nearMinY), cz);

        overlay.renderLabel(cam, xPos, Component.literal(LittleMeasurementUnits.format(xLen, box.grid, unit)), displayColor);
        overlay.renderLabel(cam, yPos, Component.literal(LittleMeasurementUnits.format(yLen, box.grid, unit)), displayColor);
        overlay.renderLabel(cam, zPos, Component.literal(LittleMeasurementUnits.format(zLen, box.grid, unit)), displayColor);
    }

    @Override
    public void build(PreviewRenderer renderer, PoseStack pose, BufferBuilder builder) {
        var renderBox = box.getRenderingBox();
        renderBox.color = color;
        renderer.buildBox(pose, renderBox, builder, 255, true);
    }

    private static double bbX(AABB bb, boolean min) {
        return (min ? bb.minX : bb.maxX) + (min ? -LABEL_OFFSET : LABEL_OFFSET);
    }

    private static double bbY(AABB bb, boolean min) {
        return (min ? bb.minY : bb.maxY) + (min ? -LABEL_OFFSET : LABEL_OFFSET);
    }

    private static double bbZ(AABB bb, boolean min) {
        return (min ? bb.minZ : bb.maxZ) + (min ? -LABEL_OFFSET : LABEL_OFFSET);
    }

}
