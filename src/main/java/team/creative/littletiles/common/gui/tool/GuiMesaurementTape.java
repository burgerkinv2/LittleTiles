package team.creative.littletiles.common.gui.tool;

import java.util.List;

import net.minecraft.core.component.PatchedDataComponentMap;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.simple.GuiButton;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiStateButton;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.client.tool.mode.BuildingModeFeatures;
import team.creative.littletiles.client.tool.mode.BuildingModeMeasures.Mode;
import team.creative.littletiles.common.item.component.MeasurementTypeComponent;
import team.creative.littletiles.common.item.component.MeasurementsComponent;
import team.creative.littletiles.common.math.measure.LittleMeasurementType;
import team.creative.littletiles.common.math.measure.LittleMeasurementUnits;

public class GuiMesaurementTape extends GuiConfigure {

    private boolean clearMeasurements;

    public GuiMesaurementTape(ContainerSlotView tool) {
        super("measurement_tape", 200, 200, tool);
    }

    @Override
    public void create() {
        flow = GuiFlow.STACK_Y;
        add(new GuiLabel("type_label").setTranslate("gui.measure_tape.type"));
        GuiComboBox<LittleMeasurementType> types = new GuiComboBox<>("type", new TextMapBuilder<LittleMeasurementType>().addComponent(LittleMeasurementType.REGISTRY.values(),
            LittleMeasurementType::translatable));
        types.select(tool.get().has(LittleTilesRegistry.MEASUREMENT_TYPE) ? tool.get().get(LittleTilesRegistry.MEASUREMENT_TYPE).type : LittleMeasurementType.REGISTRY
                .getDefault());
        add(types.setExpandableX());
        add(new GuiLabel("unit_label").setTranslate("gui.measure_tape.unit"));
        GuiComboBox<String> units = new GuiComboBox<>("unit", new TextMapBuilder<String>().addComponent(LittleMeasurementUnits.ALL,
            unit -> net.minecraft.network.chat.Component.translatable("gui.measure_tape.unit." + unit)));
        units.select(MeasurementsComponent.getUnit(tool.get()) != null ? MeasurementsComponent.getUnit(tool.get()) : LittleMeasurementUnits.defaultUnit());
        add(units.setExpandableX());
        add(new GuiLabel("default_color_label").setTranslate("gui.measure_tape.default_color"));
        add(new GuiColorPicker("default_color", new Color(MeasurementsComponent.getDefaultColor(tool.get())), true, 0));
        add(new GuiLabel("display_label").setTranslate("gui.measure_tape.display"));
        GuiStateButton<Mode> display = new GuiStateButton<>("display", new TextMapBuilder<Mode>().addComponent(Mode.values(), Mode::translatable));
        display.select(BuildingModeFeatures.MEASURES.mode());
        add(display.setExpandableX());
        add(new GuiButton("clear", x -> clearMeasurements = true).setTranslate("gui.measure_tape.clear"));
    }

    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        GuiComboBox<LittleMeasurementType> types = get("type");
        data.set(LittleTilesRegistry.MEASUREMENT_TYPE.value(), new MeasurementTypeComponent(types.selected()));
        GuiComboBox<String> units = get("unit");
        String unit = units.selected(LittleMeasurementUnits.defaultUnit());
        if (LittleMeasurementUnits.defaultUnit().equals(unit))
            unit = null;
        GuiStateButton<Mode> display = get("display");
        BuildingModeFeatures.MEASURES.setMode(display.selected());
        GuiColorPicker defaultColor = get("default_color");
        MeasurementsComponent existing = tool.get().get(LittleTilesRegistry.MEASUREMENTS.value());
        data.set(LittleTilesRegistry.MEASUREMENTS.value(), MeasurementsComponent.of(clearMeasurements || existing == null ? List.of() : existing.value(), unit, clearMeasurements || existing == null ? List
                .of() : existing.selected(), defaultColor.color.toInt()));
        return true;
    }

}
