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
import team.creative.creativecore.common.util.mc.PlayerUtils;
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
}
