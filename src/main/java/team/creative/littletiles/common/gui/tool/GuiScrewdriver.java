package team.creative.littletiles.common.gui.tool;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import team.creative.creativecore.common.gui.control.collection.GuiStackSelector;
import team.creative.creativecore.common.gui.control.parent.GuiLeftRightBox;
import team.creative.creativecore.common.gui.control.simple.GuiButton;
import team.creative.creativecore.common.gui.control.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.dialog.DialogGuiLayer.DialogButton;
import team.creative.creativecore.common.gui.dialog.GuiDialogHandler;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.flow.GuiSizeRule.GuiSizeRules;
import team.creative.creativecore.common.util.filter.BiFilter;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.api.common.tool.ILittleSelector;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.action.LittleActionColorBoxes;
import team.creative.littletiles.common.action.LittleActionDestroyBoxes;
import team.creative.littletiles.common.action.LittleActionPlace;
import team.creative.littletiles.common.action.LittleActionPlace.PlaceAction;
import team.creative.littletiles.common.action.exception.LittleActionException;
import team.creative.littletiles.common.action.LittleActions;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.group.LittleGroupAbsolute;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.filter.TileFilters;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.gui.LittleGuiUtils;
import team.creative.littletiles.common.gui.control.filter.GuiElementFilter;
import team.creative.littletiles.common.item.component.SelectionComponent;
import team.creative.littletiles.common.level.LittleLevelScanner;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesNoOverlap;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.placement.PlacementPreview;
import team.creative.littletiles.common.placement.mode.PlacementMode;
import team.creative.littletiles.common.placement.selection.ExactAreaSelectionMode;
import team.creative.littletiles.common.placement.selection.TileSelectionMode;

public class GuiScrewdriver extends GuiConfigure {

    public static BiFilter<IParentCollection, LittleTile> lastSelectedFilter = TileFilters.and();
    public static ItemStack lastSelectedReplaceStack;

    protected GuiElementFilter filter;

    public GuiScrewdriver(ContainerSlotView view) {
        super("screwdriver", 200, 205, view);
        flow = GuiFlow.STACK_Y;
    }

    @Override
    public void create() {
        if (!isClient())
            return;

        //add(new GuiCheckBox("no_structure", lastSelectedFilter instanceof TileNoStructureFilter).setTranslate("gui.no_structure"));
        add(new GuiLabel("filter_label").setTranslate("gui.filter"));
        add(filter = (GuiElementFilter) GuiElementFilter.ofGroup(getPlayer(), lastSelectedFilter).setExpandableX().setDim(new GuiSizeRules().prefHeight(100)));
        add(new GuiCheckBox("remove", false).setTranslate("gui.remove"));

        add(new GuiCheckBox("replace", false).setTranslate("gui.replace_with"));

        GuiStackSelector selector = new GuiStackSelector("replacement", getPlayer(), LittleGuiUtils.getCollector(getPlayer()), true);
        if (lastSelectedReplaceStack != null)
            selector.setSelectedForce(lastSelectedReplaceStack);
        add(selector.setExpandableX());

        Color color = new Color(255, 255, 255, 255);
        add(new GuiCheckBox("colorize", false).setTranslate("gui.colorize"));

        add(new GuiColorPicker("picker", color, LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())).setExpandableX());

        GuiLeftRightBox actions = new GuiLeftRightBox().addLeft(new GuiButton("undo", x -> {
            try {
                LittleTilesClient.ACTION_HANDLER.undo();
            } catch (LittleActionException e) {
                getPlayer().sendSystemMessage(Component.literal(e.getLocalizedMessage()));
            }
        }).setTranslate("gui.undo")).addLeft(new GuiButton("redo", x -> {
            try {
                LittleTilesClient.ACTION_HANDLER.redo();
            } catch (LittleActionException e) {
                getPlayer().sendSystemMessage(Component.literal(e.getLocalizedMessage()));
            }
        }).setTranslate("gui.redo")).addRight(new GuiButton("run", x -> {
            LittleAction action = getDesiredAction();
            if (action != null)
                if (action.wasSuccessful(LittleTilesClient.ACTION_HANDLER.execute(action)))
                    playSound(SoundEvents.LEVER_CLICK);
        }).setTranslate("gui.run"));
        add(actions);
    }

    public LittleAction getDesiredAction() {
        BiFilter<IParentCollection, LittleTile> filter = this.filter.get();

        Level level = getPlayer().level();
        SelectionComponent selection = ((ILittleSelector) this.tool.get().getItem()).getSelection(this.tool.get());
        LittleBoxes boxes = scanForMode(level, selection, filter);

        if (boxes.isEmpty())
            return null;

        boolean remove = get("remove", GuiCheckBox.class).value;
        boolean replace = get("replace", GuiCheckBox.class).value;
        boolean colorize = get("colorize", GuiCheckBox.class).value;

        if (remove)
            return destroyAction(level, boxes, filter);
        else {
            List<LittleAction> actions = new ArrayList<>();

            if (replace) {
                GuiStackSelector replacement = get("replacement");
                ItemStack stackReplace = replacement.getSelected();
                if (stackReplace != null) {
                    LittleElement replacementElement;
                    try {
                        replacementElement = LittleElement.of(stackReplace, ColorUtils.WHITE);
                    } catch (LittleElement.NotBlockException e) {
                        replacementElement = null;
                    }
                    if (replacementElement == null || !LittleAction.isBlockValid(replacementElement.getState())) {
                        GuiDialogHandler.openDialog(getIntegratedParent(), "screwdriver_dialog", Component.translatable("dialog.screwdriver.invalid_replacement"), (x, y) -> {},
                            DialogButton.OK);
                        return null;
                    }
                    actions.add(destroyAction(level, boxes, filter));
                    LittleGroupAbsolute previews = new LittleGroupAbsolute(boxes.pos);
                    previews.add(boxes.grid, replacementElement, boxes);

                    actions.add(new LittleActionPlace(PlaceAction.ABSOLUTE, PlacementPreview.absolute(level, PlacementMode.FILL, previews)));
                }
            }

            if (colorize) {
                GuiColorPicker picker = get("picker");
                actions.add(colorAction(level, boxes, picker.color.toInt(), filter));
            }

            if (!actions.isEmpty())
                return new LittleActions(actions.toArray(new LittleAction[0]));
        }

        if (!remove && !replace && !colorize)
            GuiDialogHandler.openDialog(getIntegratedParent(), "screwdriver_dialog", Component.translatable("dialog.screwdriver.no_task"), (x, y) -> {}, DialogButton.OK);

        return null;
    }

    private LittleAction destroyAction(Level level, LittleBoxes boxes, BiFilter<IParentCollection, LittleTile> filter) {
        if (filter != null)
            return new LittleActionDestroyBoxes.LittleActionDestroyBoxesFiltered(level, boxes, filter);
        return new LittleActionDestroyBoxes(level, boxes);
    }

    private LittleAction colorAction(Level level, LittleBoxes boxes, int color, BiFilter<IParentCollection, LittleTile> filter) {
        if (filter != null)
            return new LittleActionColorBoxes.LittleActionColorBoxesFiltered(level, boxes, color, false, filter);
        return new LittleActionColorBoxes(level, boxes, color, false);
    }

    private LittleBoxes scanForMode(Level level, SelectionComponent component, BiFilter<IParentCollection, LittleTile> filter) {
        var config = component.getConfig();
        boolean includeVanilla = config.getBoolean(GuiScrewdriverSecondary.INCLUDE_VANILLA_KEY);
        if (component.mode instanceof TileSelectionMode) {
            if (!config.contains("boxes"))
                return emptyBoxes();
            return LittleLevelScanner.scan(level, new LittleBoxesNoOverlap(config.getCompound("boxes")), filter, includeVanilla);
        }

        if (component.mode instanceof ExactAreaSelectionMode) {
            LittleBoxAbsolute pos1 = null;
            if (config.contains("pos1"))
                pos1 = LittleBoxAbsolute.of(config.getIntArray("pos1"));

            LittleBoxAbsolute pos2 = null;
            if (config.contains("pos2"))
                pos2 = LittleBoxAbsolute.of(config.getIntArray("pos2"));

            if (pos1 == null && pos2 != null)
                pos1 = pos2;
            else if (pos1 != null && pos2 != null)
                pos1.include(pos2);

            if (pos1 != null)
                return LittleLevelScanner.scan(level, pos1, filter, includeVanilla);
            return emptyBoxes();
        }

        BlockPos pos = null;
        boolean hasPos1 = config.contains("pos1");
        boolean hasPos2 = config.contains("pos2");
        if (hasPos1) {
            int[] arr = config.getIntArray("pos1");
            if (arr.length >= 3)
                pos = new BlockPos(arr[0], arr[1], arr[2]);
        }
        if (pos == null && hasPos2) {
            int[] arr = config.getIntArray("pos2");
            if (arr.length >= 3)
                pos = new BlockPos(arr[0], arr[1], arr[2]);
        }
        if (pos == null)
            return emptyBoxes();

        BlockPos pos2 = pos;
        if (hasPos2) {
            int[] arr = config.getIntArray("pos2");
            if (arr.length >= 3)
                pos2 = new BlockPos(arr[0], arr[1], arr[2]);
        }

        return LittleLevelScanner.scan(level, pos, pos2, filter, includeVanilla);
    }

    private LittleBoxes emptyBoxes() {
        return new LittleBoxesSimple(BlockPos.ZERO, LittleGrid.MIN);
    }

    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        if (isClient()) {
            lastSelectedFilter = filter.get();
            lastSelectedReplaceStack = ((GuiStackSelector) get("replacement")).getSelected();
        }
        return false;
    }
}
