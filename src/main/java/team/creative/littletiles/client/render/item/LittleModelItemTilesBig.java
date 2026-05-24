package team.creative.littletiles.client.render.item;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public class LittleModelItemTilesBig extends LittleModelItemTiles {
    
    @Override
    public List<? extends RenderBox> getBoxes(ItemStack stack, boolean translucent) {
        List<? extends RenderBox> boxes = super.getBoxes(stack, translucent);
        if (!ILittleTool.getData(stack).getBoolean(ItemLittleBlueprint.NO_ITEM_PREVIEW_SHRINK_KEY))
            LittleGroup.shrinkCubesToOneBlock(boxes);
        return boxes;
    }
    
}
