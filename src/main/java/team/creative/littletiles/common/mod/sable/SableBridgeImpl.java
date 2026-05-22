package team.creative.littletiles.common.mod.sable;

import java.util.function.Predicate;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.math.box.ABB;
import team.creative.creativecore.common.util.math.box.OBB;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.matrix.IVecOrigin;
import team.creative.creativecore.common.util.math.matrix.Matrix3;
import team.creative.creativecore.common.util.mc.PlayerUtils;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.common.mod.sable.SableBridge.Context;

final class SableBridgeImpl {

    private SableBridgeImpl() {}

    static Context findContext(Level level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        return subLevel == null ? null : new Context(subLevel);
    }

    static Context findContext(Level level, Vec3 pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        return subLevel == null ? null : new Context(subLevel);
    }

    static Vec3 transformPointToSubLevelLocal(Context context, Vec3 point) {
        return ((SubLevel) context.unwrap()).logicalPose().transformPositionInverse(point);
    }

    static Vec3 transformPointToSubLevelWorld(Context context, Vec3 point) {
        return ((SubLevel) context.unwrap()).logicalPose().transformPosition(point);
    }

    static Vec3 transformVectorToSubLevelWorld(Context context, Vec3 vector) {
        return ((SubLevel) context.unwrap()).logicalPose().transformNormal(vector);
    }

    static Vec3 transformVectorToSubLevelLocal(Context context, Vec3 vector) {
        return ((SubLevel) context.unwrap()).logicalPose().transformNormalInverse(vector);
    }

    private static AABB transformAABB(AABB box, java.util.function.Function<Vec3, Vec3> transformer) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    Vec3 transformed = transformer.apply(new Vec3(x == 0 ? box.minX : box.maxX, y == 0 ? box.minY : box.maxY, z == 0 ? box.minZ : box.maxZ));
                    minX = Math.min(minX, transformed.x);
                    minY = Math.min(minY, transformed.y);
                    minZ = Math.min(minZ, transformed.z);
                    maxX = Math.max(maxX, transformed.x);
                    maxY = Math.max(maxY, transformed.y);
                    maxZ = Math.max(maxZ, transformed.z);
                }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static AABB transformAABBToSubLevelLocal(Context context, AABB box) {
        return transformAABB(box, point -> transformPointToSubLevelLocal(context, point));
    }

    static AABB transformAABBToSubLevelWorld(Context context, AABB box) {
        return transformAABB(box, point -> transformPointToSubLevelWorld(context, point));
    }

    static ABB transformOBBToSubLevelWorld(Context context, ABB box, IVecOrigin origin) {
        return new OBB(box, new SableComposedOrigin(context, origin));
    }

    static BlockHitResult raytraceInContext(Level level, Player player, Context context, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        double reach = PlayerUtils.getReach(player);
        Vec3 view = player.getViewVector(partialTick);
        Vec3 far = eye.add(view.x * reach, view.y * reach, view.z * reach);

        Vec3 from = eye;
        Vec3 to = far;
        if (context != null) {
            from = transformPointToSubLevelLocal(context, eye);
            to = transformPointToSubLevelLocal(context, far);
            if (from == null || to == null)
                return null;
        }

        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        ClipContextExtension ext = (ClipContextExtension) ctx;
        ext.sable$setDoNotProject(true);
        if (context == null)
            ext.sable$setSubLevelIgnoring((Predicate<SubLevel>) x -> true);
        return level.clip(ctx);
    }

    private static final class SableComposedOrigin implements IVecOrigin {

        private final Context context;
        private final IVecOrigin origin;

        private SableComposedOrigin(Context context, IVecOrigin origin) {
            this.context = context;
            this.origin = origin;
        }

        @Override
        public void transformPointToWorld(Vec3d vec) {
            origin.transformPointToWorld(vec);
            Vec3 transformed = transformPointToSubLevelWorld(context, new Vec3(vec.x, vec.y, vec.z));
            vec.set(transformed.x, transformed.y, transformed.z);
        }

        @Override
        public void transformPointToFakeWorld(Vec3d vec) {
            Vec3 transformed = transformPointToSubLevelLocal(context, new Vec3(vec.x, vec.y, vec.z));
            vec.set(transformed.x, transformed.y, transformed.z);
            origin.transformPointToFakeWorld(vec);
        }

        @Override
        public double offX() {
            return origin.offX();
        }

        @Override
        public double offY() {
            return origin.offY();
        }

        @Override
        public double offZ() {
            return origin.offZ();
        }

        @Override
        public double rotX() {
            return origin.rotX();
        }

        @Override
        public double rotY() {
            return origin.rotY();
        }

        @Override
        public double rotZ() {
            return origin.rotZ();
        }

        @Override
        public double offXLast() {
            return origin.offXLast();
        }

        @Override
        public double offYLast() {
            return origin.offYLast();
        }

        @Override
        public double offZLast() {
            return origin.offZLast();
        }

        @Override
        public double rotXLast() {
            return origin.rotXLast();
        }

        @Override
        public double rotYLast() {
            return origin.rotYLast();
        }

        @Override
        public double rotZLast() {
            return origin.rotZLast();
        }

        @Override
        public boolean isRotated() {
            return true;
        }

        @Override
        public void offX(double value) {
            origin.offX(value);
        }

        @Override
        public void offY(double value) {
            origin.offY(value);
        }

        @Override
        public void offZ(double value) {
            origin.offZ(value);
        }

        @Override
        public void off(double x, double y, double z) {
            origin.off(x, y, z);
        }

        @Override
        public void rotX(double value) {
            origin.rotX(value);
        }

        @Override
        public void rotY(double value) {
            origin.rotY(value);
        }

        @Override
        public void rotZ(double value) {
            origin.rotZ(value);
        }

        @Override
        public void rot(double x, double y, double z) {
            origin.rot(x, y, z);
        }

        @Override
        public Vec3d deltaMovement() {
            return origin.deltaMovement();
        }

        @Override
        public void deltaMovement(Vec3d value) {
            origin.deltaMovement(value);
        }

        @Override
        public Vec3d center() {
            return origin.center();
        }

        @Override
        public void setCenter(Vec3d vec) {
            origin.setCenter(vec);
        }

        @Override
        public Matrix3 rotation() {
            return origin.rotation();
        }

        @Override
        public Matrix3 rotationInv() {
            return origin.rotationInv();
        }

        @Override
        public Vec3d translation() {
            return origin.translation();
        }

        @Override
        public void tick() {
            origin.tick();
        }

        @Override
        public IVecOrigin getParent() {
            return origin.getParent();
        }

        @Override
        public double translationCombined(Axis axis) {
            return origin.translationCombined(axis);
        }

        @Override
        public IVecOrigin copy() {
            return new SableComposedOrigin(context, origin.copy());
        }
    }
}
