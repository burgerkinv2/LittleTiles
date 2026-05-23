package team.creative.littletiles.common.math.measure;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.client.render.overlay.OverlayRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;

public class LittleMeasurementArea extends LittleMeasurementBox {

    public LittleMeasurementArea(CompoundTag nbt) {
        super(nbt);
    }

    public LittleMeasurementArea(List<LittleBoxAbsolute> positions) {
        super(positions);
    }

    @Override
    public void overlay(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam, String unit) {
        var bb = box.toAABB();
        if (!renderer.isVisible(bb))
            return;

        var center = bb.getCenter();
        if (cam.x > bb.maxX)
            displayFace(renderer, overlay, cam, bb, center, Facing.EAST, unit);
        else if (cam.x < bb.minX)
            displayFace(renderer, overlay, cam, bb, center, Facing.WEST, unit);

        if (cam.y > bb.maxY)
            displayFace(renderer, overlay, cam, bb, center, Facing.UP, unit);
        else if (cam.y < bb.minY)
            displayFace(renderer, overlay, cam, bb, center, Facing.DOWN, unit);

        if (cam.z > bb.maxZ)
            displayFace(renderer, overlay, cam, bb, center, Facing.SOUTH, unit);
        else if (cam.z < bb.minZ)
            displayFace(renderer, overlay, cam, bb, center, Facing.NORTH, unit);
    }

    private void displayFace(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam, AABB bb, Vec3 center, Facing facing, String unit) {
        Vec3d vec = new Vec3d(center);
        vec.set(facing.axis, facing.get(bb));
        double area = facing.axis == Axis.X ? bb.getYsize() * bb.getZsize() : (facing.axis == Axis.Y ? bb.getXsize() * bb.getZsize() : bb.getXsize() * bb.getYsize());
        overlay.renderLabel(cam, vec, Component.literal(LittleMeasurementUnits.formatArea(area, box.grid, unit)), color != 0 ? color : ColorUtils.WHITE);
    }

}
