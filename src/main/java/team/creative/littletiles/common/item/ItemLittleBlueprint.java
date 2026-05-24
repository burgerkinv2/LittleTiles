package team.creative.littletiles.common.item;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittlePlacer;
import team.creative.littletiles.api.common.tool.ILittleSelector;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.action.LittleActionHandlerClient;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.tool.LittleToolPlacer;
import team.creative.littletiles.client.tool.LittleToolSelection;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.gui.tool.GuiConfigure;
import team.creative.littletiles.common.gui.tool.blueprint.GuiBlueprint;
import team.creative.littletiles.common.gui.tool.blueprint.GuiBlueprintSecondary;
import team.creative.littletiles.common.gui.tool.blueprint.GuiBlueprintSelection;
import team.creative.littletiles.common.item.tooltip.IItemTooltip;
import team.creative.littletiles.common.math.vec.LittleVecGrid;
import team.creative.littletiles.common.packet.action.BlockPacket;
import team.creative.littletiles.common.packet.action.BlockPacket.BlockPacketAction;

public class ItemLittleBlueprint extends Item implements ILittlePlacer, ILittleSelector, IItemTooltip {
    
    public static final String CONTENT_KEY = "c";
    public static final String NO_ITEM_PREVIEW_SHRINK_KEY = "lt_no_item_preview_shrink";
    public static final String ITEM_MODEL_SCALE_KEY = "item_model_scale";
    public static final String ITEM_MODEL_EXTRA_SCALE_KEY = "lt_item_model_extra_scale";
    public static final String ITEM_MODEL_DISPLAY_SCALE_KEY = "lt_item_model_display_scale";
    public static final String SHOW_BLUEPRINT_BACKGROUND_KEY = "show_blueprint_background";
    public static final int DEFAULT_COLOR = ColorUtils.rgb(242, 231, 198);
    public static final int DEFAULT_COLOR_SECONDARY = ColorUtils.rgb(185, 169, 123);
    public static final float DEFAULT_ITEM_MODEL_SCALE = 1;
    public static final float MIN_ITEM_MODEL_SCALE = 0.05F;
    public static final float MAX_ITEM_MODEL_SCALE = 16F;
    
    public static CompoundTag getContent(ItemStack stack) {
        return getContent(ILittleTool.getData(stack));
    }

    public static CompoundTag getContent(CompoundTag nbt) {
        if (nbt.contains(CONTENT_KEY) && !nbt.contains(LittleGroup.BOXES_COUNT_KEY))
            return nbt.getCompound(CONTENT_KEY);
        return nbt;
    }

    public static ItemStack getContentStack(ItemStack stack) {
        return getContentStack(stack, true);
    }

    public static ItemStack getContentStack(ItemStack stack, boolean shrinkItemPreview) {
        CompoundTag stackData = ILittleTool.getData(stack);
        CompoundTag content = getContent(stack).copy();
        if (!LittleGroup.shouldRenderInHand(content))
            return ItemStack.EMPTY;
        float automaticScale = getAutomaticItemModelScale(content);
        float targetScale = getItemModelScale(stackData, content);
        boolean hasCustomScale = stackData.contains(ITEM_MODEL_SCALE_KEY);

        content.remove(ITEM_MODEL_SCALE_KEY);
        content.remove("item_model_scale_itemblock");
        content.remove(ITEM_MODEL_EXTRA_SCALE_KEY);
        content.remove(ITEM_MODEL_DISPLAY_SCALE_KEY);
        content.remove(SHOW_BLUEPRINT_BACKGROUND_KEY);
        content.remove(NO_ITEM_PREVIEW_SHRINK_KEY);
        ItemStack contentStack = new ItemStack(LittleTilesRegistry.ITEM_TILES.value());
        if (shrinkItemPreview) {
            float renderScale = targetScale / automaticScale;
            if (hasCustomScale && renderScale != DEFAULT_ITEM_MODEL_SCALE) {
                if (renderScale < DEFAULT_ITEM_MODEL_SCALE)
                    content.putFloat(ITEM_MODEL_EXTRA_SCALE_KEY, renderScale);
                else
                    content.putFloat(ITEM_MODEL_DISPLAY_SCALE_KEY, renderScale);
            }
        }
        if (!shrinkItemPreview)
            content.putBoolean(NO_ITEM_PREVIEW_SHRINK_KEY, true);
        ILittleTool.setData(contentStack, content);
        return contentStack;
    }

    public static float getItemModelScale(ItemStack stack) {
        CompoundTag data = ILittleTool.getData(stack);
        return getItemModelScale(data, getContent(stack));
    }

    public static float getItemModelScale(CompoundTag data) {
        if (!data.contains(ITEM_MODEL_SCALE_KEY))
            return DEFAULT_ITEM_MODEL_SCALE;
        return Mth.clamp(data.getFloat(ITEM_MODEL_SCALE_KEY), MIN_ITEM_MODEL_SCALE, MAX_ITEM_MODEL_SCALE);
    }

    public static float getItemModelExtraScale(CompoundTag data) {
        if (!data.contains(ITEM_MODEL_EXTRA_SCALE_KEY))
            return DEFAULT_ITEM_MODEL_SCALE;
        return Mth.clamp(data.getFloat(ITEM_MODEL_EXTRA_SCALE_KEY), MIN_ITEM_MODEL_SCALE, MAX_ITEM_MODEL_SCALE);
    }

    public static float getItemModelDisplayScale(CompoundTag data) {
        if (!data.contains(ITEM_MODEL_DISPLAY_SCALE_KEY))
            return DEFAULT_ITEM_MODEL_SCALE;
        return Mth.clamp(data.getFloat(ITEM_MODEL_DISPLAY_SCALE_KEY), MIN_ITEM_MODEL_SCALE, MAX_ITEM_MODEL_SCALE);
    }

    public static float getItemModelScale(CompoundTag data, CompoundTag content) {
        if (data.contains(ITEM_MODEL_SCALE_KEY))
            return getItemModelScale(data);
        return getAutomaticItemModelScale(content);
    }

    @OnlyIn(Dist.CLIENT)
    public static float getAutomaticItemModelScale(CompoundTag content) {
        LittleGroup group = LittleGroup.load(content);
        float scale = getAutomaticItemModelScale(group.getRenderingBoxes(false));
        if (group.hasTranslucentBlocksInGui())
            scale = Math.min(scale, getAutomaticItemModelScale(group.getRenderingBoxes(true)));
        return scale;
    }

    @OnlyIn(Dist.CLIENT)
    private static float getAutomaticItemModelScale(List<? extends RenderBox> boxes) {
        return Mth.clamp(LittleGroup.getItemPreviewScale(boxes, DEFAULT_ITEM_MODEL_SCALE), MIN_ITEM_MODEL_SCALE, DEFAULT_ITEM_MODEL_SCALE);
    }

    public static boolean isLargerThanOneBlock(CompoundTag content) {
        LittleVecGrid size = LittleGroup.getSize(content);
        return size != null && (size.getPosX() > 1 || size.getPosY() > 1 || size.getPosZ() > 1);
    }

    public static boolean showBlueprintBackground(ItemStack stack) {
        return showBlueprintBackground(ILittleTool.getData(stack));
    }

    public static boolean showBlueprintBackground(CompoundTag data) {
        return !data.contains(SHOW_BLUEPRINT_BACKGROUND_KEY) || data.getBoolean(SHOW_BLUEPRINT_BACKGROUND_KEY);
    }

    public static void setItemModelSettings(CompoundTag data, float scale, boolean showBackground) {
        data.remove("item_model_scale_itemblock");
        scale = Mth.clamp(scale, MIN_ITEM_MODEL_SCALE, MAX_ITEM_MODEL_SCALE);
        if (scale == getAutomaticItemModelScale(getContent(data)))
            data.remove(ITEM_MODEL_SCALE_KEY);
        else
            data.putFloat(ITEM_MODEL_SCALE_KEY, scale);

        if (showBackground)
            data.remove(SHOW_BLUEPRINT_BACKGROUND_KEY);
        else
            data.putBoolean(SHOW_BLUEPRINT_BACKGROUND_KEY, false);
    }

    public static void copyItemModelSettings(CompoundTag source, CompoundTag target) {
        if (source.contains(ITEM_MODEL_SCALE_KEY))
            target.putFloat(ITEM_MODEL_SCALE_KEY, getItemModelScale(source));
        if (!showBlueprintBackground(source))
            target.putBoolean(SHOW_BLUEPRINT_BACKGROUND_KEY, false);
    }
    
    public ItemLittleBlueprint() {
        super(new Item.Properties());
    }
    
    @Override
    public Component getName(ItemStack stack) {
        var content = getContent(stack);
        if (content.contains(LittleGroup.STRUCTURE_KEY) && content.getCompound(LittleGroup.STRUCTURE_KEY).contains("n"))
            return Component.literal(content.getCompound(LittleGroup.STRUCTURE_KEY).getString("n"));
        return super.getName(stack);
    }
    
    @Override
    public boolean hasSelection(ItemStack stack) {
        return !hasTiles(stack);
    }
    
    @Override
    public boolean hasTiles(ItemStack stack) {
        return ILittleTool.hasData(stack);
    }
    
    @Override
    public LittleGroup getTiles(ItemStack stack) {
        return LittleGroup.load(getContent(stack));
    }
    
    @Override
    public LittleGroup getLow(ItemStack stack) {
        return LittleGroup.loadLow(getContent(stack));
    }
    
    public void saveTiles(ItemStack stack, LittleGroup group) {
        CompoundTag stackTag = ILittleTool.getData(stack);
        stackTag.put(ItemLittleBlueprint.CONTENT_KEY, LittleGroup.save(group));
        ILittleTool.setData(stack, stackTag);
    }
    
    @Override
    public GuiConfigure getConfigure(Player player, ContainerSlotView view, boolean secondary) {
        if (secondary)
            return new GuiBlueprintSecondary(view);
        if (!((ItemLittleBlueprint) view.get().getItem()).hasTiles(view.get()))
            return new GuiBlueprintSelection(view);
        return new GuiBlueprint(view);
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
    public boolean containsIngredients(ItemStack stack) {
        return false;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(LittleGroup.printTooltip(getContent(stack)));
    }
    
    @Override
    public LittleVecGrid getCachedSize(ItemStack stack) {
        return LittleGroup.getSize(getContent(stack));
    }
    
    @Override
    public LittleVecGrid getCachedMin(ItemStack stack) {
        return LittleGroup.getMin(getContent(stack));
    }
    
    @Override
    public String tooltipTranslateKey(ItemStack stack, String defaultKey) {
        if (hasTiles(stack))
            return "littletiles.blueprint.placement.tooltip";
        return "littletiles.blueprint.selection.tooltip";
    }
    
    @Override
    public Object[] tooltipData(ItemStack stack) {
        if (hasTiles(stack))
            return new Object[] { LittleTilesClient.KEY_CONFIGURE.getTranslatedKeyMessage(), LittleTilesClient.KEY_CONFIGURE_SECONDARY.getTranslatedKeyMessage(), LittleTilesClient
                    .arrowKeysTooltip(), LittleTilesClient.KEY_MIRROR.getTranslatedKeyMessage() };
        return new Object[] { Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage(), Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(), Minecraft
                .getInstance().options.keyPickItem.getTranslatedKeyMessage(), LittleTilesClient.KEY_CONFIGURE.getTranslatedKeyMessage() };
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isCorrectTool(ItemStack stack, LittleTool tool) {
        return hasTiles(stack) == (tool instanceof LittleToolPlacer);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public LittleTool tool(ItemStack stack) {
        if (hasTiles(stack))
            return new LittleToolPlacer(stack);
        return new LittleToolBlueprintSelection(stack);
    }
    
    @OnlyIn(Dist.CLIENT)
    public class LittleToolBlueprintSelection extends LittleToolSelection {
        
        public LittleToolBlueprintSelection(ItemStack stack) {
            super(stack);
        }
        
        @Override
        public boolean onMouseWheelClickBlock(PreviewRenderer renderer, BlockHitResult result) {
            BlockState state = renderer.level().getBlockState(result.getBlockPos());
            if (state.getBlock() instanceof BlockTile) {
                CompoundTag nbt = new CompoundTag();
                nbt.putBoolean("secondMode", LittleActionHandlerClient.isUsingSecondMode());
                LittleTiles.NETWORK.sendToServer(new BlockPacket(renderer.level(), result.getBlockPos(), renderer.player(), BlockPacketAction.BLUEPRINT, nbt));
                return true;
            }
            return true;
        }
        
    }
    
}
