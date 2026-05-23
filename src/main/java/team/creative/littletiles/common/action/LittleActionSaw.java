package team.creative.littletiles.common.action;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.littletiles.common.action.LittleActionPlace.PlaceAction;
import team.creative.littletiles.common.action.cancel.ActionCancelContext;
import team.creative.littletiles.common.action.exception.LittleActionException;
import team.creative.littletiles.common.action.source.LittleActionSource;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.LittleTileContext;
import team.creative.littletiles.common.block.little.tile.group.LittleGroupAbsolute;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.item.component.LittleSawMode;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesNoOverlap;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.placement.PlacementPreview;
import team.creative.littletiles.common.placement.mode.PlacementMode;

public class LittleActionSaw extends LittleActionInteract<Boolean> {

    public boolean toLimit;
    public boolean shrink;
    public LittleGrid grid;
    public LittleSawMode mode = LittleSawMode.SINGLE;
    public transient LittleAction revertAction;

    public LittleActionSaw(Level level, BlockPos blockPos, Player player, boolean toLimit, LittleGrid grid) {
        this(level, blockPos, player, toLimit, grid, false, LittleSawMode.SINGLE);
    }

    public LittleActionSaw(Level level, BlockPos blockPos, Player player, boolean toLimit, LittleGrid grid, boolean shrink) {
        this(level, blockPos, player, toLimit, grid, shrink, LittleSawMode.SINGLE);
    }

    public LittleActionSaw(Level level, BlockPos blockPos, Player player, boolean toLimit, LittleGrid grid, boolean shrink, LittleSawMode mode) {
        super(level, blockPos, player);
        this.toLimit = toLimit;
        this.grid = grid;
        this.shrink = shrink;
        this.mode = mode;
    }

    public LittleActionSaw() {}

    @Override
    public boolean canBeReverted() {
        return revertAction != null;
    }

    @Override
    public LittleAction revert(LittleActionSource source) {
        return revertAction;
    }

    @Override
    protected Boolean action(Level level, BETiles be, LittleTileContext context, ItemStack stack, LittleActionSource source, BlockHitResult hit, BlockPos pos,
            boolean secondMode) throws LittleActionException {
        if (context.parent.isStructure())
            return false;

        Facing facing = Facing.get(hit.getDirection());
        if (facing == null)
            return false;

        LittleGrid usedGrid = grid != null && grid.count > context.parent.getGrid().count ? grid : context.parent.getGrid();
        LittleBoxesNoOverlap boxes = new LittleBoxesNoOverlap(context.parent.getPos(), usedGrid);
        for (SawTarget target : targets(level, context, facing, usedGrid))
            boxes.add(target.slice(facing, shrink, toLimit, usedGrid, context.parent.getPos()));

        LittleAction<Boolean> action = shrink ? new LittleActionDestroyBoxes(uuid, boxes) : new LittleActionPlace(PlaceAction.ABSOLUTE, PlacementPreview.load(uuid,
            PlacementMode.FILL, new LittleGroupAbsolute(boxes, context.tile)));
        Boolean result = action.action(source);
        if (action.wasSuccessful(result)) {
            revertAction = action.revert(source);
            return true;
        }
        return false;
    }

    private Iterable<SawTarget> targets(Level level, LittleTileContext context, Facing facing, LittleGrid usedGrid) {
        if (mode == LittleSawMode.CONNECTED)
            return connectedTargets(level, context, facing, usedGrid);
        if (mode == LittleSawMode.SAME_TYPE)
            return sameTypeTargets(context, facing, usedGrid);
        return java.util.List.of(new SawTarget(context.parent, context.tile, convertBox(context.box, context.parent.getGrid(), usedGrid)));
    }

    private Iterable<SawTarget> sameTypeTargets(LittleTileContext context, Facing facing, LittleGrid usedGrid) {
        java.util.List<SawTarget> targets = new java.util.ArrayList<>();
        SawTarget start = new SawTarget(context.parent, context.tile, convertBox(context.box, context.parent.getGrid(), usedGrid));
        int face = start.globalFace(facing, usedGrid, context.parent.getPos());
        for (LittleTile tile : context.parent)
            if (context.tile.is(tile))
                for (LittleBox box : tile) {
                    SawTarget target = new SawTarget(context.parent, tile, convertBox(box, context.parent.getGrid(), usedGrid));
                    if (target.globalFace(facing, usedGrid, context.parent.getPos()) == face)
                        targets.add(target);
                }
        return targets;
    }

    private Iterable<SawTarget> connectedTargets(Level level, LittleTileContext context, Facing facing, LittleGrid usedGrid) {
        HashMap<BlockPos, BETiles> blocks = new HashMap<>();
        HashSet<String> visited = new HashSet<>();
        ArrayDeque<SawTarget> queue = new ArrayDeque<>();
        java.util.List<SawTarget> targets = new java.util.ArrayList<>();
        SawTarget start = new SawTarget(context.parent, context.tile, convertBox(context.box, context.parent.getGrid(), usedGrid));
        BlockPos origin = context.parent.getPos();
        int face = start.globalFace(facing, usedGrid, origin);
        queue.add(start);
        visited.add(start.key(usedGrid));
        while (!queue.isEmpty()) {
            SawTarget current = queue.removeFirst();
            targets.add(current);
            BETiles be = current.parent.getBE();
            blocks.put(be.getBlockPos(), be);
            addTouching(level, blocks, visited, queue, current, context.tile, facing, usedGrid, origin, face);
        }
        return targets;
    }

    private void addTouching(Level level, HashMap<BlockPos, BETiles> blocks, HashSet<String> visited, ArrayDeque<SawTarget> queue, SawTarget current, LittleTile selected,
            Facing actionFacing, LittleGrid usedGrid, BlockPos origin, int face) {
        addTouchingInBlock(visited, queue, current, selected, current.parent.getBE(), actionFacing, usedGrid, current.box, origin, face);
        for (Facing searchFacing : Facing.VALUES) {
            if (!current.box.isFaceAtEdge(usedGrid, searchFacing))
                continue;
            BlockPos neighborPos = current.parent.getPos().relative(searchFacing.toVanilla());
            BETiles neighbor = blocks.get(neighborPos);
            if (neighbor == null) {
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be instanceof BETiles tiles)
                    neighbor = tiles;
                else
                    continue;
                blocks.put(neighborPos, neighbor);
            }
            LittleBox neighborSpace = current.box.copy();
            neighborSpace.sub(usedGrid.count * searchFacing.offset(Axis.X), usedGrid.count * searchFacing.offset(Axis.Y), usedGrid.count * searchFacing.offset(Axis.Z));
            addTouchingInBlock(visited, queue, current, selected, neighbor, actionFacing, usedGrid, neighborSpace, origin, face);
        }
    }

    private void addTouchingInBlock(HashSet<String> visited, ArrayDeque<SawTarget> queue, SawTarget current, LittleTile selected, BETiles be, Facing facing, LittleGrid usedGrid,
            LittleBox touchingBox, BlockPos origin, int face) {
        for (Pair<IParentCollection, LittleTile> pair : be.allBoxes()) {
            if (pair.key.isStructure() || !selected.is(pair.value))
                continue;
            for (LittleBox box : pair.value) {
                LittleBox converted = convertBox(box, pair.key.getGrid(), usedGrid);
                SawTarget target = new SawTarget(pair.key, pair.value, converted);
                if (visited.contains(target.key(usedGrid)) || target.key(usedGrid).equals(current.key(usedGrid)))
                    continue;
                if (target.globalFace(facing, usedGrid, origin) != face)
                    continue;
                if (converted.doesTouch(usedGrid, usedGrid, touchingBox)) {
                    visited.add(target.key(usedGrid));
                    queue.add(target);
                }
            }
        }
    }

    private static LittleBox convertBox(LittleBox box, LittleGrid from, LittleGrid to) {
        LittleBox converted = box.copy();
        if (from != to)
            converted.convertTo(from, to);
        return converted;
    }

    public static LittleBox addSlice(LittleBox box, Facing facing, boolean toLimit, LittleGrid grid) {
        LittleBox slice = box.copy();
        if (facing.positive) {
            int min = box.getMax(facing.axis);
            slice.setMin(facing.axis, min);
            slice.setMax(facing.axis, toLimit ? nextEdge(min, grid) : min + 1);
        } else {
            int max = box.getMin(facing.axis);
            slice.setMin(facing.axis, toLimit ? previousEdge(max, grid) : max - 1);
            slice.setMax(facing.axis, max);
        }
        return slice;
    }

    public static LittleBox deleteSlice(LittleBox box, Facing facing) {
        LittleBox slice = box.copy();
        if (facing.positive)
            slice.setMin(facing.axis, box.getMax(facing.axis) - 1);
        else
            slice.setMax(facing.axis, box.getMin(facing.axis) + 1);
        return slice;
    }

    private static int nextEdge(int value, LittleGrid grid) {
        int remainder = Math.floorMod(value, grid.count);
        return remainder == 0 ? value + grid.count : value + grid.count - remainder;
    }

    private static int previousEdge(int value, LittleGrid grid) {
        int remainder = Math.floorMod(value, grid.count);
        return remainder == 0 ? value - grid.count : value - remainder;
    }

    @Override
    protected boolean requiresBreakEvent() {
        return false;
    }

    @Override
    protected boolean isRightClick() {
        return true;
    }

    @Override
    protected Boolean ignored() {
        return false;
    }

    @Override
    public Boolean failed() {
        return false;
    }

    @Override
    public boolean wasSuccessful(Boolean result) {
        return result;
    }

    @Override
    public void cancel(ActionCancelContext context) throws LittleActionException {
        if (revertAction != null)
            revertAction.cancel(context);
    }

    @Override
    public LittleAction mirror(Axis axis, LittleBoxAbsolute box) {
        return null;
    }

    @Override
    public void include(LittleBoxes boxes) {}

    @Override
    public void exclude(LittleBoxes boxes) {}

    private static class SawTarget {

        public final IParentCollection parent;
        public final LittleTile tile;
        public final LittleBox box;

        public SawTarget(IParentCollection parent, LittleTile tile, LittleBox box) {
            this.parent = parent;
            this.tile = tile;
            this.box = box;
        }

        public LittleBox slice(Facing facing, boolean shrink, boolean toLimit, LittleGrid grid, BlockPos origin) {
            LittleBox slice = shrink ? deleteSlice(box, facing) : addSlice(box, facing, toLimit, grid);
            slice.add(new LittleVec(grid, parent.getPos().subtract(origin)));
            return slice;
        }

        public String key(LittleGrid grid) {
            return parent.getPos().toShortString() + ":" + grid.count + ":" + box.minX + "," + box.minY + "," + box.minZ + "," + box.maxX + "," + box.maxY + "," + box.maxZ;
        }

        public int globalFace(Facing facing, LittleGrid grid, BlockPos origin) {
            int blockOffset = parent.getPos().subtract(origin).get(facing.axis.toVanilla()) * grid.count;
            return blockOffset + (facing.positive ? box.getMax(facing.axis) : box.getMin(facing.axis));
        }

    }

}
