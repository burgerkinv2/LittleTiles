package team.creative.littletiles.client.render.tile;

import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.box.QuadGeneratorContext;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.api.client.render.LittleElementAppearanceModelRegistry;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;

public class LittleRenderBox extends RenderBox {

    public LittleBox box;
    public CompoundTag appearance;

    public LittleRenderBox(AlignedBox box) {
        super(box);
    }
    
    public LittleRenderBox(AlignedBox box, BlockState state) {
        super(box, state);
    }
    
    public LittleRenderBox(LittleGrid grid, LittleBox box) {
        super(grid.toVanillaGridF(box.minX), grid.toVanillaGridF(box.minY), grid.toVanillaGridF(box.minZ), grid.toVanillaGridF(box.maxX), grid.toVanillaGridF(box.maxY), grid
                .toVanillaGridF(box.maxZ), (BlockState) null);
        this.color = ColorUtils.WHITE;
        this.box = box;
    }
    
    public LittleRenderBox(LittleGrid grid, LittleBox box, BlockState state) {
        super(grid.toVanillaGridF(box.minX), grid.toVanillaGridF(box.minY), grid.toVanillaGridF(box.minZ), grid.toVanillaGridF(box.maxX), grid.toVanillaGridF(box.maxY), grid
                .toVanillaGridF(box.maxZ), state);
        this.color = ColorUtils.WHITE;
        this.box = box;
    }
    
    public LittleRenderBox(LittleGrid grid, LittleBox box, LittleElement element) {
        super(grid.toVanillaGridF(box.minX), grid.toVanillaGridF(box.minY), grid.toVanillaGridF(box.minZ), grid.toVanillaGridF(box.maxX), grid.toVanillaGridF(box.maxY), grid
                .toVanillaGridF(box.maxZ), element.getState());
        this.color = element.color;
        this.box = box;
        this.appearance = element.appearance();
    }
    
    @Override
    public LittleRenderBox setColor(int color) {
        return (LittleRenderBox) super.setColor(color);
    }

    @Override
    public List<BakedQuad> getBakedQuad(QuadGeneratorContext holder, LevelAccessor level, BlockPos pos, BlockPos offset, BlockState state, BakedModel blockModel,
            ModelData modelData, Facing facing, RenderType layer, RandomSource rand, boolean overrideTint, int defaultColor) {
        BakedModel appearanceModel = LittleElementAppearanceModelRegistry.getModel(state, pos, appearance);
        return super.getBakedQuad(holder, level, pos, offset, state, appearanceModel != null ? appearanceModel : blockModel, modelData, facing, layer, rand, overrideTint,
            defaultColor);
    }

}
