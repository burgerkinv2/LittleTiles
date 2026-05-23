package team.creative.littletiles.common.gui.premade.photo;

import net.minecraft.world.level.block.state.BlockState;
import team.creative.littletiles.common.grid.LittleGrid;

public record PhotoImportOptions(LittleGrid grid, boolean ignoreAlpha, double colorAccuracy, int maxPixels, BlockState state) {
    
}
