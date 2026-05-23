package team.creative.littletiles.common.gui.tool;

import net.minecraft.core.component.PatchedDataComponentMap;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.common.item.component.LittleSawMode;

public class GuiSaw extends GuiConfigureTool {

    public GuiSaw(ContainerSlotView view) {
        super("saw", 160, 80, view);
        flow = GuiFlow.STACK_Y;
        spacing = 4;
    }

    @Override
    public void create() {
        if (!isClient())
            return;

        add(new GuiLabel("mode_label").setTranslate("gui.saw.mode"));
        GuiComboBox<LittleSawMode> modeBox = new GuiComboBox<>("mode", LittleSawMode.MAP);
        modeBox.select(mode());
        add(modeBox.setExpandableX());

        add(new GuiLabel("shift_mode_label").setTranslate("gui.saw.shift_mode"));
        GuiComboBox<LittleSawMode> shiftModeBox = new GuiComboBox<>("shift_mode", LittleSawMode.MAP);
        shiftModeBox.select(shiftMode());
        add(shiftModeBox.setExpandableX());
    }

    private LittleSawMode mode() {
        LittleSawMode mode = tool.get().get(LittleTilesRegistry.SAW_MODE.value());
        return mode != null ? mode : LittleSawMode.SINGLE;
    }

    private LittleSawMode shiftMode() {
        LittleSawMode mode = tool.get().get(LittleTilesRegistry.SAW_SHIFT_MODE.value());
        return mode != null ? mode : LittleSawMode.SAME_TYPE;
    }

    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        GuiComboBox<LittleSawMode> modeBox = get("mode");
        LittleSawMode selected = modeBox.selected(LittleSawMode.SINGLE);
        GuiComboBox<LittleSawMode> shiftModeBox = get("shift_mode");
        LittleSawMode shiftSelected = shiftModeBox.selected(LittleSawMode.SAME_TYPE);
        boolean changed = selected != mode() || shiftSelected != shiftMode();
        if (changed) {
            data.set(LittleTilesRegistry.SAW_MODE.value(), selected);
            data.set(LittleTilesRegistry.SAW_SHIFT_MODE.value(), shiftSelected);
        }
        return changed;
    }

}
