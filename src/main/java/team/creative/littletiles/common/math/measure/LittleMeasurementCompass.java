package team.creative.littletiles.common.math.measure;

import java.util.List;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.client.render.overlay.OverlayRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;

public class LittleMeasurementCompass extends LittleMeasurement {

    protected final LittleBoxAbsolute vertex;
    protected final LittleBoxAbsolute armA;
    protected final LittleBoxAbsolute armB;

    private Vec3d posC;
    private Vec3d posA;
    private Vec3d posB;
    private Vec3d midCA;
    private Vec3d midCB;
    private double lengthCA;
    private double lengthCB;
    private double angleDegrees;

    public LittleMeasurementCompass(CompoundTag nbt) {
        super(nbt);
        int[] vArr = nbt.getIntArray("0");
        int[] aArr = nbt.getIntArray("1");
        int[] bArr = nbt.getIntArray("2");
        if (vArr.length < 3 || aArr.length < 3 || bArr.length < 3)
            throw new IllegalArgumentException("LittleMeasurementCompass: missing position arrays");
        this.vertex = LittleBoxAbsolute.of(vArr);
        this.armA = LittleBoxAbsolute.of(aArr);
        this.armB = LittleBoxAbsolute.of(bArr);
        changed();
    }

    public LittleMeasurementCompass(List<LittleBoxAbsolute> positions) {
        this.vertex = positions.get(0);
        this.armA = positions.get(1);
        this.armB = positions.get(2);
        changed();
    }

    @Override
    public void changed() {
        super.changed();
        posC = vertex.getVanillaCenter();
        posA = armA.getVanillaCenter();
        posB = armB.getVanillaCenter();
        midCA = new Vec3d((posC.x + posA.x) * 0.5, (posC.y + posA.y) * 0.5, (posC.z + posA.z) * 0.5);
        midCB = new Vec3d((posC.x + posB.x) * 0.5, (posC.y + posB.y) * 0.5, (posC.z + posB.z) * 0.5);
        lengthCA = posC.distance(posA);
        lengthCB = posC.distance(posB);
        if (lengthCA <= 0 || lengthCB <= 0) {
            angleDegrees = 0;
            return;
        }
        double sideAB = posA.distance(posB);
        double cosVal = (lengthCA * lengthCA + lengthCB * lengthCB - sideAB * sideAB) / (2.0 * lengthCA * lengthCB);
        angleDegrees = Math.toDegrees(Math.acos(Mth.clamp(cosVal, -1.0, 1.0)));
    }

    @Override
    public void overlay(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam, String unit) {
        int displayColor = color != 0 ? color : ColorUtils.WHITE;
        LittleGrid grid = pickFinerGrid(vertex.grid, armA.grid);
        grid = pickFinerGrid(grid, armB.grid);
        overlay.renderLabel(cam, new Vec3d(posC.x, posC.y + 0.06, posC.z), Component.literal(formatAngle(angleDegrees)), displayColor);
        overlay.renderLabel(cam, midCA, Component.literal(LittleMeasurementUnits.format(lengthCA, grid, unit)), displayColor);
        overlay.renderLabel(cam, midCB, Component.literal(LittleMeasurementUnits.format(lengthCB, grid, unit)), displayColor);
    }

    private static LittleGrid pickFinerGrid(LittleGrid a, LittleGrid b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return a.count >= b.count ? a : b;
    }

    private static String formatAngle(double deg) {
        double rounded = Math.round(deg * 10.0) / 10.0;
        if (rounded == Math.floor(rounded))
            return ((long) rounded) + " deg";
        return rounded + " deg";
    }

    @Override
    public void build(PreviewRenderer renderer, PoseStack pose, BufferBuilder builder) {
        emitLine(pose, builder, posC, posA);
        emitLine(pose, builder, posC, posB);
    }

    private void emitLine(PoseStack pose, BufferBuilder builder, Vec3d from, Vec3d to) {
        int vertexColor = color != 0 ? color : ColorUtils.WHITE;
        Vec3f normal = new Vec3f();
        VectorFan.setLineNormal(normal, (float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z);
        builder.addVertex(pose.last().pose(), (float) from.x, (float) from.y, (float) from.z).setColor(vertexColor).setNormal(pose.last(), normal.x, normal.y, normal.z);
        builder.addVertex(pose.last().pose(), (float) to.x, (float) to.y, (float) to.z).setColor(vertexColor).setNormal(pose.last(), normal.x, normal.y, normal.z);
    }

    @Override
    public void collectPositions(List<LittleBoxAbsolute> positions) {
        positions.add(vertex);
        positions.add(armA);
        positions.add(armB);
    }

    @Override
    protected void saveExtra(CompoundTag nbt) {
        nbt.putIntArray("0", vertex.toArray());
        nbt.putIntArray("1", armA.toArray());
        nbt.putIntArray("2", armB.toArray());
    }

}
