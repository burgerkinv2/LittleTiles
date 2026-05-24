package team.creative.littletiles.common.gui.control.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import team.creative.creativecore.common.gui.control.collection.GuiStackSelector;
import team.creative.creativecore.common.util.filter.BiFilter;
import team.creative.littletiles.api.common.block.LittleElementAppearanceRegistry;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.filter.TileFilters;
import team.creative.littletiles.common.gui.LittleGuiUtils;

public class GuiElementFilterBlock extends GuiElementFilter {
    
    protected GuiStackSelector selector;
    protected final Block initialBlock;
    protected final CompoundTag initialAppearance;
    
    public GuiElementFilterBlock(Player player, Block block) {
        this(player, block, null);
    }

    public GuiElementFilterBlock(Player player, Block block, CompoundTag appearance) {
        this.initialBlock = block;
        this.initialAppearance = appearance != null && !appearance.isEmpty() ? appearance.copy() : null;
        add(selector = new GuiStackSelector("filter", player, LittleGuiUtils.getCollector(player), true));
        if (block != null)
            selector.setSelectedForce(new ItemStack(block));
        selector.setExpandableX();
    }
    
    @Override
    public BiFilter<IParentCollection, LittleTile> get() {
        ItemStack selected = selector.getSelected();
        if (selected == null)
            return null;
        Block filterBlock = Block.byItem(selected.getItem());
        if (filterBlock != null && !(filterBlock instanceof AirBlock) && LittleAction.isBlockValid(filterBlock.defaultBlockState()))
            return TileFilters.block(filterBlock, appearanceForSelectedStack(selected, filterBlock));
        return null;
    }

    private CompoundTag appearanceForSelectedStack(ItemStack selected, Block filterBlock) {
        CompoundTag appearance = LittleElementAppearanceRegistry.getAppearance(selected);
        if (appearance != null && !appearance.isEmpty())
            return appearance;
        if (filterBlock == initialBlock && initialAppearance != null)
            return initialAppearance.copy();
        return null;
    }
    
}
