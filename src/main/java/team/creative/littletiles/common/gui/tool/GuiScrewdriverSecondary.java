package team.creative.littletiles.common.gui.tool;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleSelector;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.common.gui.control.GuiGridConfig;
import team.creative.littletiles.common.item.component.SelectionComponent;
import team.creative.littletiles.common.placement.selection.SelectionMode;
import team.creative.littletiles.common.placement.setting.PlacementPlayerSetting;

public class GuiScrewdriverSecondary extends GuiConfigureTool {

    public static final String INCLUDE_VANILLA_KEY = "incl_vanilla";

    public GuiScrewdriverSecondary(ContainerSlotView view) {
        super("screwdriver_secondary", 160, 120, view);
        flow = GuiFlow.STACK_Y;
        align = Align.STRETCH;
        spacing = 4;
    }

    @Override
    public void create() {
        if (!isClient())
            return;

        SelectionComponent component = ((ILittleSelector) tool.get().getItem()).getSelection(tool.get());
        add(new GuiLabel("mode_label").setTranslate("gui.selection.mode"));
        GuiComboBox<SelectionMode> modeBox = new GuiComboBox<>("selection_mode", new TextMapBuilder<SelectionMode>().addEntrySet(SelectionMode.REGISTRY.entrySet(), x -> x
                .getValue().getTranslation()));
        modeBox.select(component.mode);
        add(modeBox.setExpandableX());

        add(new GuiLabel("grid_label").setTranslate("gui.grid"));
        add(new GuiGridConfig("grid", getPlayer(), PlacementPlayerSetting.grid(getPlayer()), LittleTilesClient::grid));

        add(new GuiCheckBox("include_vanilla", component.getConfig().getBoolean(INCLUDE_VANILLA_KEY)).setTranslate("gui.screwdriver.include_vanilla"));
    }

    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        @SuppressWarnings("unchecked")
        GuiComboBox<SelectionMode> modeBox = (GuiComboBox<SelectionMode>) get("selection_mode");
        SelectionMode selectedMode = modeBox.selected(SelectionMode.REGISTRY.getDefault());

        SelectionComponent existing = ((ILittleSelector) tool.get().getItem()).getSelection(tool.get());
        CompoundTag config = existing.getConfig();
        boolean includeVanilla = get("include_vanilla", GuiCheckBox.class).value;

        boolean changed = existing.mode != selectedMode || config.getBoolean(INCLUDE_VANILLA_KEY) != includeVanilla;
        if (!changed)
            return false;

        config.putBoolean(INCLUDE_VANILLA_KEY, includeVanilla);
        data.set(LittleTilesRegistry.SELECTION.get(), SelectionComponent.of(selectedMode, config));
        return true;
    }

}
