package team.creative.littletiles.common.item;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleSelector;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.tool.LittleToolSelection;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.gui.tool.GuiConfigure;
import team.creative.littletiles.common.gui.tool.GuiScrewdriver;
import team.creative.littletiles.common.gui.tool.GuiScrewdriverSecondary;
import team.creative.littletiles.common.item.component.SelectionComponent;
import team.creative.littletiles.common.item.tooltip.IItemTooltip;
import team.creative.littletiles.common.placement.selection.SelectionMode;
import team.creative.littletiles.common.placement.setting.PlacementPlayerSetting;

public class ItemLittleScrewdriver extends Item implements ILittleTool, ILittleSelector, IItemTooltip {

    public ItemLittleScrewdriver() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public GuiConfigure getConfigure(Player player, ContainerSlotView view, boolean secondary) {
        return secondary ? new GuiScrewdriverSecondary(view) : new GuiScrewdriver(view);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0F;
    }

    @Override
    public boolean hasSelection(ItemStack stack) {
        return true;
    }

    @Override
    public SelectionComponent getSelection(ItemStack stack) {
        SelectionComponent component = stack.get(LittleTilesRegistry.SELECTION);
        if (component != null)
            return component;

        CompoundTag config = new CompoundTag();
        BlockPos pos = stack.get(LittleTilesRegistry.FIRST_POS);
        if (pos != null)
            config.putIntArray("pos1", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        pos = stack.get(LittleTilesRegistry.SECOND_POS);
        if (pos != null)
            config.putIntArray("pos2", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        component = SelectionComponent.of(SelectionMode.REGISTRY.getDefault(), config);
        if (!config.isEmpty())
            setSelection(stack, component);
        return component;
    }

    @Override
    public void setSelection(ItemStack stack, SelectionComponent component) {
        ILittleSelector.super.setSelection(stack, component);
        stack.remove(LittleTilesRegistry.FIRST_POS);
        stack.remove(LittleTilesRegistry.SECOND_POS);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag config = getSelection(stack).getConfig();
        if (config.contains("pos1")) {
            int[] pos = config.getIntArray("pos1");
            tooltip.add(Component.literal("1: " + pos[0] + " " + pos[1] + " " + pos[2]));
        } else
            tooltip.add(Component.literal("1: ").append(Component.translatable("gui.click.left")));

        if (config.contains("pos2")) {
            int[] pos = config.getIntArray("pos2");
            tooltip.add(Component.literal("2: " + pos[0] + " " + pos[1] + " " + pos[2]));
        } else
            tooltip.add(Component.literal("2: ").append(Component.translatable("gui.click.right")));
    }

    @Override
    public Object[] tooltipData(ItemStack stack) {
        return new Object[] { Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage(), Minecraft.getInstance().options.keyUse
                .getTranslatedKeyMessage(), LittleTilesClient.KEY_CONFIGURE.getTranslatedKeyMessage(), LittleTilesClient.KEY_CONFIGURE_SECONDARY.getTranslatedKeyMessage() };
    }

    @Override
    public LittleGrid getPositionGrid(Player player, ItemStack stack) {
        return PlacementPlayerSetting.grid(player);
    }
    
    @Override
    public LittleGrid getSelectorGrid(Player player, ItemStack stack) {
        return PlacementPlayerSetting.grid(player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public LittleTool tool(ItemStack stack) {
        return new LittleToolSelection(stack);
    }

    @Override
    public boolean isCorrectTool(ItemStack stack, LittleTool tool) {
        return tool instanceof LittleToolSelection;
    }

}
