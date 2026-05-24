package team.creative.littletiles.common.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.action.LittleActionDestroyBoxes;
import team.creative.littletiles.common.action.LittleActionPlace;
import team.creative.littletiles.common.action.LittleActionPlace.PlaceAction;
import team.creative.littletiles.common.action.LittleActionSaw;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.group.LittleGroupAbsolute;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.gui.tool.GuiConfigure;
import team.creative.littletiles.common.gui.tool.GuiSaw;
import team.creative.littletiles.common.item.component.LittleSawMode;
import team.creative.littletiles.common.item.tooltip.IItemTooltip;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxesNoOverlap;
import team.creative.littletiles.common.placement.PlacementPreview;
import team.creative.littletiles.common.placement.mode.PlacementMode;
import team.creative.littletiles.common.placement.setting.PlacementPlayerSetting;

public class ItemLittleSaw extends Item implements ILittleTool, IItemTooltip {

    public ItemLittleSaw() {
        super(new Item.Properties().stacksTo(1));
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
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof BlockTile) {
            if (context.getLevel().isClientSide)
                LittleTilesClient.ACTION_HANDLER.execute(new LittleActionSaw(context.getLevel(), context.getClickedPos(), player, Screen.hasControlDown(), PlacementPlayerSetting
                        .grid(player), false, mode(context.getItemInHand())));
            return InteractionResult.SUCCESS;
        }

        if (!LittleAction.isBlockValid(state))
            return InteractionResult.PASS;

        if (context.getLevel().isClientSide)
            executeVanillaBlockAction(context.getLevel(), context.getClickedPos(), player, state, Facing.get(context.getClickedFace()), true);
        return InteractionResult.SUCCESS;
    }

    private static void executeVanillaBlockAction(Level level, BlockPos pos, Player player, BlockState state, Facing facing, boolean extend) {
        if (facing == null)
            return;

        LittleGrid grid = PlacementPlayerSetting.grid(player);
        LittleBoxesNoOverlap boxes = new LittleBoxesNoOverlap(pos, grid);
        LittleBox fullBlock = grid.box();
        if (extend)
            boxes.add(LittleActionSaw.addSlice(fullBlock, facing, Screen.hasControlDown(), grid));
        else
            boxes.add(LittleActionSaw.deleteSlice(fullBlock, facing));

        if (extend)
            LittleTilesClient.ACTION_HANDLER.execute(new LittleActionPlace(PlaceAction.ABSOLUTE, PlacementPreview.absolute(level, PlacementMode.FILL, new LittleGroupAbsolute(boxes,
                new LittleElement(state, ColorUtils.WHITE)))));
        else
            LittleTilesClient.ACTION_HANDLER.execute(new LittleActionDestroyBoxes(level, boxes));
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean executeToolAction(PreviewRenderer renderer, BlockHitResult result, ItemStack stack, boolean extend) {
        if (result == null)
            return false;

        Player player = renderer.player();
        Level level = renderer.level();
        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockTile) {
            LittleTilesClient.ACTION_HANDLER.execute(new LittleActionSaw(level, pos, player, Screen.hasControlDown(), PlacementPlayerSetting.grid(player), !extend, mode(stack)));
            return true;
        }

        if (!LittleAction.isBlockValid(state))
            return false;

        executeVanillaBlockAction(level, pos, player, state, Facing.get(result.getDirection()), extend);
        return true;
    }

    @Override
    public Object[] tooltipData(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return new Object[] { minecraft.options.keyUse.getTranslatedKeyMessage(), minecraft.options.keyAttack.getTranslatedKeyMessage() };
    }

    public static LittleSawMode mode(ItemStack stack) {
        LittleSawMode mode = Screen.hasShiftDown() ? stack.get(LittleTilesRegistry.SAW_SHIFT_MODE.value()) : stack.get(LittleTilesRegistry.SAW_MODE.value());
        if (mode != null)
            return mode;
        return Screen.hasShiftDown() ? LittleSawMode.SAME_TYPE : LittleSawMode.SINGLE;
    }

    @Override
    public GuiConfigure getConfigure(Player player, ContainerSlotView view, boolean secondary) {
        return new GuiSaw(view);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public LittleTool tool(ItemStack stack) {
        return new LittleTool(stack) {

            @Override
            protected void tickInternal(PreviewRenderer renderer) {}

            @Override
            protected void renderInternal(PreviewRenderer renderer, PoseStack pose, Vec3 cam, boolean lines) {}

            @Override
            public boolean onLeftClick(PreviewRenderer renderer, BlockHitResult result) {
                return executeToolAction(renderer, result, this.stack, false);
            }

            @Override
            public boolean onRightClick(PreviewRenderer renderer, BlockHitResult result) {
                return executeToolAction(renderer, result, this.stack, true);
            }
        };
    }

}
