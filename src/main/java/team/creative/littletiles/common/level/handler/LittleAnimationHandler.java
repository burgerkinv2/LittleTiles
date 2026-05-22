package team.creative.littletiles.common.level.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.common.level.IOrientatedLevel;
import team.creative.creativecore.common.level.ISubLevel;
import team.creative.creativecore.common.util.math.box.ABB;
import team.creative.creativecore.common.util.math.box.BoxesVoxelShape;
import team.creative.creativecore.common.util.math.box.OBB;
import team.creative.littletiles.common.entity.LittleEntity;
import team.creative.littletiles.common.math.vec.LittleHitResult;
import team.creative.littletiles.common.mod.sable.SableBridge;
import team.creative.littletiles.common.mod.sable.SableBridge.Context;

public abstract class LittleAnimationHandler extends LevelHandler {

    public static boolean isRemotePlayer(Entity entity) {
        if (entity.level().isClientSide)
            return isRemotePlayerClient(entity);
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isRemotePlayerClient(Entity entity) {
        return entity instanceof RemotePlayer;
    }

    public Set<LittleEntity> entities = new CopyOnWriteArraySet<>();

    public LittleAnimationHandler(Level level) {
        super(level);
    }

    @Override
    public void unload() {
        super.unload();
        entities.clear();
    }

    protected void tickEntity(LittleEntity entity) {
        if (entity.level() != level || entity.level() instanceof IOrientatedLevel)
            return;
        entity.performTick();
    }

    public void tick() {
        for (LittleEntity entity : entities)
            tickEntity(entity);
    }

    public List<LittleEntity> find(SectionPos pos) {
        return find(new AABB(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX() + 1, pos.maxBlockY() + 1, pos.maxBlockZ() + 1));
    }

    public List<LittleEntity> find(AABB bb) {
        if (entities.isEmpty())
            return Collections.emptyList();

        List<LittleEntity> found = new ArrayList<>();
        for (LittleEntity entity : entities) {
            if (!entity.hasLoaded())
                continue;
            AABB query = queryBox(entity, bb);
            if (query != null && entity.getBoundingBox().intersects(query))
                found.add(entity);
        }
        return found;
    }

    public LittleEntity find(UUID uuid) {
        for (LittleEntity entity : entities)
            if (entity.getUUID().equals(uuid))
                return entity;
        return null;
    }

    public void add(LittleEntity entity) {
        entities.add(entity);
    }

    public void remove(LittleEntity entity) {
        entities.remove(entity);
    }

    private Context sableContext(LittleEntity entity) {
        return SableBridge.findContext(entity.level(), entity.blockPosition());
    }

    private AABB queryBox(LittleEntity entity, AABB box) {
        Context context = sableContext(entity);
        if (context == null)
            return box;
        AABB transformed = SableBridge.transformAABBToSubLevelLocal(context, box);
        return transformed == null ? box : transformed;
    }

    private Vec3 queryPoint(Context context, Vec3 point) {
        if (context == null)
            return point;
        Vec3 transformed = SableBridge.transformPointToSubLevelLocal(context, point);
        return transformed == null ? point : transformed;
    }

    private Vec3 resultPoint(Context context, LittleEntity entity, LittleHitResult result) {
        Vec3 localWorld = entity.getOrigin().transformPointToWorld(result.hit.getLocation());
        if (context == null)
            return localWorld;
        Vec3 transformed = SableBridge.transformPointToSubLevelWorld(context, localWorld);
        return transformed == null ? localWorld : transformed;
    }

    public Iterable<VoxelShape> collisionExcept(@Nullable Entity colliding, AABB box, Level level) {
        if (level instanceof ISubLevel)
            return null;
        List<VoxelShape> shapes = null;
        for (LittleEntity entity : find(box)) {
            if (!entity.physic.shouldPush() || entity.noClip() || entity.physic.noCollision())
                continue;

            List<ABB> boxes = null;
            Context context = sableContext(entity);
            AABB query = queryBox(entity, box);
            ABB transformedBox = entity.getOrigin().getOBB(query);
            for (VoxelShape shape : entity.getSubLevel().getBlockCollisions(colliding, transformedBox.toVanilla()))
                for (AABB bb : shape.toAabbs())
                    if (transformedBox.intersects(bb)) {
                        if (boxes == null)
                            boxes = new ArrayList<>();
                        if (context == null)
                            boxes.add(new OBB(bb, entity.getOrigin()));
                        else {
                            AABB worldBox = entity.getOrigin().getAABB(asABB(bb)).toVanilla();
                            AABB sableWorldBox = SableBridge.transformAABBToSubLevelWorld(context, worldBox);
                            boxes.add(asABB(sableWorldBox == null ? worldBox : sableWorldBox));
                        }
                    }

            if (boxes != null) {
                if (shapes == null)
                    shapes = new ArrayList<>();
                shapes.add(BoxesVoxelShape.create(boxes));
            }
        }
        return shapes;
    }

    public LittleHitResult getHit(Vec3 pos, Vec3 look, double reach) {
        AABB box = new AABB(pos, look);

        LittleHitResult newHit = null;
        double distance = reach;
        for (LittleEntity entity : find(box)) {
            if (!entity.hasLoaded())
                continue;
            Context context = sableContext(entity);
            Vec3 queryPos = queryPoint(context, pos);
            Vec3 queryLook = queryPoint(context, look);
            LittleHitResult tempResult = entity.rayTrace(queryPos, queryLook);
            if (tempResult == null || !(tempResult.hit instanceof BlockHitResult))
                continue;
            double tempDistance = pos.distanceTo(resultPoint(context, entity, tempResult));
            if (tempDistance < distance) {
                newHit = tempResult;
                distance = tempDistance;
            }
        }

        return newHit;
    }

    private ABB asABB(AABB box) {
        return new ABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

}
