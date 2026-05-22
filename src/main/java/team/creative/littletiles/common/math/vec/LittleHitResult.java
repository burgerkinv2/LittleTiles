package team.creative.littletiles.common.math.vec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.level.ISubLevel;
import team.creative.littletiles.common.entity.LittleEntity;
import team.creative.littletiles.common.level.little.LittleSubLevel;
import team.creative.littletiles.common.mod.sable.SableBridge;
import team.creative.littletiles.common.mod.sable.SableBridge.Context;

public class LittleHitResult extends EntityHitResult {

    public final HitResult hit;
    public final LittleSubLevel level;

    public LittleHitResult(Entity entity, HitResult result, LittleSubLevel level) {
        super(entity);
        this.hit = result;
        this.level = level;
    }

    public boolean isBlock() {
        return hit instanceof BlockHitResult;
    }

    public boolean isEntity() {
        return hit instanceof EntityHitResult;
    }

    public BlockHitResult asBlockHit() {
        return (BlockHitResult) hit;
    }

    public EntityHitResult asEntityHit() {
        return (EntityHitResult) hit;
    }

    public LittleEntity getHolder() {
        if (level instanceof ISubLevel)
            return (LittleEntity) ((ISubLevel) level).getHolder();
        return null;
    }

    public Vec3 getRealLocation() {
        Vec3 location = level.getOrigin().transformPointToWorld(hit.getLocation());
        LittleEntity holder = getHolder();
        if (holder != null) {
            Context context = SableBridge.findContext(holder.level(), holder.blockPosition());
            if (context != null) {
                Vec3 transformed = SableBridge.transformPointToSubLevelWorld(context, location);
                if (transformed != null)
                    return transformed;
            }
        }
        return location;
    }

}
