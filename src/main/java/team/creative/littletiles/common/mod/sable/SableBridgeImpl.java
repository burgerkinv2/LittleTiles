package team.creative.littletiles.common.mod.sable;

import java.util.function.Predicate;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
    
    static Vec3 transformPointToSubLevelLocal(Context context, Vec3 point) {
        return ((SubLevel) context.unwrap()).logicalPose().transformPositionInverse(point);
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
