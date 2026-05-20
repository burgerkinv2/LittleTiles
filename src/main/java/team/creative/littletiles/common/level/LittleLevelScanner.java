package team.creative.littletiles.common.level;

import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.common.util.filter.BiFilter;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;

public class LittleLevelScanner {

    public static LittleBoxes scan(Level level, BlockPos pos, @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        LittleBoxes boxes = new LittleBoxesSimple(pos, LittleGrid.MIN);

        BETiles te = BlockTile.loadBE(level, pos);

        if (te == null)
            return boxes;

        for (Pair<IParentCollection, LittleTile> pair : te.allTiles())
            if (filter == null || filter.is(pair.key, pair.value))
                boxes.addBoxes(pair.key, pair.value);

        return boxes;
    }

    public static LittleBoxes scan(Level level, BlockPos pos, BlockPos pos2, @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        return scan(level, pos, pos2, filter, false);
    }

    public static LittleBoxes scan(Level level, BlockPos pos, BlockPos pos2, @Nullable BiFilter<IParentCollection, LittleTile> filter, boolean includeVanilla) {
        LittleBoxes boxes = new LittleBoxesSimple(pos, LittleGrid.MIN);

        int minX = Math.min(pos.getX(), pos2.getX());
        int maxX = Math.max(pos.getX(), pos2.getX());
        int minY = Math.min(pos.getY(), pos2.getY());
        int maxY = Math.max(pos.getY(), pos2.getY());
        int minZ = Math.min(pos.getZ(), pos2.getZ());
        int maxZ = Math.max(pos.getZ(), pos2.getZ());

        MutableBlockPos position = new MutableBlockPos();

        for (int posX = minX; posX <= maxX; posX++) {
            for (int posY = minY; posY <= maxY; posY++) {
                for (int posZ = minZ; posZ <= maxZ; posZ++) {

                    position.set(posX, posY, posZ);

                    BETiles te = BlockTile.loadBE(level, position);

                    if (te == null) {
                        if (includeVanilla)
                            emitVanillaFullBlock(boxes, level, position);
                        continue;
                    }

                    for (Pair<IParentCollection, LittleTile> pair : te.allTiles())
                        if (filter == null || filter.is(pair.key, pair.value))
                            boxes.addBoxes(pair.key, pair.value);
                }
            }
        }

        return boxes;
    }

    public static LittleBoxes scan(Level level, LittleBoxAbsolute absolute, @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        return scan(level, absolute, filter, false);
    }

    public static LittleBoxes scan(Level level, LittleBoxAbsolute absolute, @Nullable BiFilter<IParentCollection, LittleTile> filter, boolean includeVanilla) {
        LittleBoxes boxes = new LittleBoxesSimple(absolute.getMinPos(), LittleGrid.MIN);
        BlockPos minPos = absolute.getMinPos();
        BlockPos maxPos = absolute.getMaxPos();

        MutableBlockPos position = new MutableBlockPos();
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    position.set(x, y, z);
                    BETiles te = BlockTile.loadBE(level, position);
                    BlockPos blockPos = position.immutable();
                    if (te == null) {
                        if (includeVanilla) {
                            BlockState state = level.getBlockState(blockPos);
                            if (LittleAction.isBlockValid(state)) {
                                LittleBox slice = absolute.extractSimple(blockPos);
                                if (slice != null)
                                    boxes.addBox(absolute.grid, blockPos, slice.copy());
                            }
                        }
                        continue;
                    }

                    te.sameGrid(absolute, () -> clipBlockTiles(boxes, te, absolute.extractSimple(blockPos), filter));
                }
            }
        }
        return boxes;
    }

    public static LittleBoxes scan(Level level, LittleBoxes constraint, @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        return scan(level, constraint, filter, false);
    }

    public static LittleBoxes scan(Level level, LittleBoxes constraint, @Nullable BiFilter<IParentCollection, LittleTile> filter, boolean includeVanilla) {
        LittleBoxes boxes = new LittleBoxesSimple(constraint.pos, LittleGrid.MIN);
        var blockwise = constraint.generateBlockWise();

        MutableBlockPos position = new MutableBlockPos();
        for (Entry<BlockPos, ? extends List<LittleBox>> entry : blockwise.entrySet()) {
            BlockPos blockPos = entry.getKey();
            position.set(blockPos);
            BETiles te = BlockTile.loadBE(level, position);
            if (te == null) {
                if (includeVanilla) {
                    BlockState state = level.getBlockState(blockPos);
                    if (LittleAction.isBlockValid(state))
                        for (LittleBox box : entry.getValue())
                            boxes.addBox(constraint.getGrid(), blockPos, box.copy());
                }
                continue;
            }

            te.sameGrid(constraint, () -> {
                var refreshed = constraint.generateBlockWise().get(blockPos);
                if (refreshed == null || refreshed.isEmpty())
                    return;
                clipBlockTiles(boxes, te, refreshed, filter);
            });
        }
        return boxes;
    }

    private static void emitVanillaFullBlock(LittleBoxes out, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (LittleAction.isBlockValid(state))
            out.addBox(out.getGrid(), pos, out.getGrid().box());
    }

    private static void clipBlockTiles(LittleBoxes out, BETiles te, @Nullable LittleBox constraint,
            @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        if (constraint == null)
            return;
        for (Pair<IParentCollection, LittleTile> pair : te.allTiles()) {
            if (filter != null && !filter.is(pair.key, pair.value))
                continue;
            IParentCollection parent = pair.key;
            for (LittleBox tileBox : pair.value) {
                if (!LittleBox.intersectsWith(tileBox, constraint))
                    continue;
                LittleBox overlap = tileBox.intersection(constraint);
                if (overlap != null)
                    out.addBox(parent.getGrid(), parent.getPos(), overlap.copy());
            }
        }
    }

    private static void clipBlockTiles(LittleBoxes out, BETiles te, List<LittleBox> constraints,
            @Nullable BiFilter<IParentCollection, LittleTile> filter) {
        for (Pair<IParentCollection, LittleTile> pair : te.allTiles()) {
            if (filter != null && !filter.is(pair.key, pair.value))
                continue;
            IParentCollection parent = pair.key;
            for (LittleBox tileBox : pair.value) {
                for (LittleBox constraint : constraints) {
                    if (!LittleBox.intersectsWith(tileBox, constraint))
                        continue;
                    LittleBox overlap = tileBox.intersection(constraint);
                    if (overlap != null)
                        out.addBox(parent.getGrid(), parent.getPos(), overlap.copy());
                }
            }
        }
    }

}
