package team.creative.littletiles.common.gui.tool.blueprint;

import com.mojang.serialization.DataResult;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.CustomData;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.simple.GuiButtonHoldSlim;
import team.creative.creativecore.common.gui.control.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiTextfield;
import team.creative.creativecore.common.gui.event.GuiControlChangedEvent;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.style.ControlFormatting;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.text.TextBuilder;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.common.gui.control.GuiGridConfig;
import team.creative.littletiles.common.gui.tool.GuiConfigureTool;
import team.creative.littletiles.common.item.ItemLittleBlueprint;
import team.creative.littletiles.common.placement.mode.PlacementMode;
import team.creative.littletiles.common.placement.setting.PlacementPlayerSetting;

public class GuiBlueprintSecondary extends GuiConfigureTool {

    private static final float ITEM_MODEL_SCALE_STEP = 0.1F;
    
    public GuiBlueprintSecondary(ContainerSlotView view) {
        super("blueprint_secondary", 180, 285, view);
        flow = GuiFlow.STACK_Y;
        align = Align.STRETCH;
        spacing = 4;
        registerEventChanged(x -> {
            if (x.control.is("mode")) {
                GuiComboBox<PlacementMode> modeBox = (GuiComboBox<PlacementMode>) x.control;
                TextBuilder builder = new TextBuilder();
                if (modeBox.selected().canPlaceStructures())
                    builder.text("" + ChatFormatting.BOLD).translate("placement.mode.placestructure").text("" + ChatFormatting.WHITE).newLine();
                builder.translate(modeBox.selected().translatableKey() + ".tooltip");
                ((GuiLabel) get("text")).setTitle(builder.build());
                LittleTilesClient.placementMode(modeBox.selected());
            } else if (x.control.is("show_blueprint_background"))
                applyModelSettingsNow();
        });
    }
    
    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        data.set(LittleTilesRegistry.COLOR.get(), get("picker", GuiColorPicker.class).color.toInt());
        data.set(LittleTilesRegistry.COLOR_SECONDARY.get(), get("picker2", GuiColorPicker.class).color.toInt());

        CompoundTag config = ILittleTool.getData(tool.get());
        float scale = get("item_model_scale", GuiBlueprintScaleControl.class).getValue();
        boolean showBackground = get("show_blueprint_background", GuiCheckBox.class).value;
        ItemLittleBlueprint.setItemModelSettings(config, scale, showBackground);
        data.set(LittleTilesRegistry.DATA.get(), CustomData.of(config));
        return true;
    }

    private void applyModelSettingsNow() {
        if (!isClient())
            return;
        PatchedDataComponentMap map = new PatchedDataComponentMap(DataComponentMap.EMPTY);
        if (saveConfiguration(map)) {
            DataResult<Tag> dataresult = DataComponentPatch.CODEC.encode(map.asPatch(), NbtOps.INSTANCE, new CompoundTag());
            SAVE_CONFIG.send((CompoundTag) dataresult.getOrThrow());
        }
    }
    
    @Override
    public void create() {
        if (!isClient())
            return;
        add(new GuiLabel("label").setTranslate("gui.blueprint.color.primary"));
        add(new GuiColorPicker("picker", new Color(tool.get().getOrDefault(LittleTilesRegistry.COLOR, ItemLittleBlueprint.DEFAULT_COLOR)), LittleTiles.CONFIG.isTransparencyEnabled(
            getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        
        add(new GuiLabel("label").setTranslate("gui.blueprint.color.secondary"));
        add(new GuiColorPicker("picker2", new Color(tool.get().getOrDefault(LittleTilesRegistry.COLOR_SECONDARY, ItemLittleBlueprint.DEFAULT_COLOR_SECONDARY)), LittleTiles.CONFIG
                .isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));

        CompoundTag config = ILittleTool.getData(tool.get());
        add(new GuiLabel("item_model_scale_label").setTranslate("gui.blueprint.item_model_scale"));
        add(new GuiBlueprintScaleControl(ItemLittleBlueprint.getItemModelScale(tool.get())));
        add(new GuiCheckBox("show_blueprint_background", ItemLittleBlueprint.showBlueprintBackground(config)).setTranslate("gui.blueprint.show_background"));
        
        add(new GuiGridConfig("grid", getPlayer(), PlacementPlayerSetting.grid(getPlayer()), LittleTilesClient::grid));
        
        GuiComboBox<PlacementMode> modeBox = new GuiComboBox<>("mode", PlacementMode.map());
        modeBox.select(PlacementPlayerSetting.placementMode(getPlayer()));
        add(modeBox);
        add(new GuiLabel("text"));
        raiseEvent(new GuiControlChangedEvent(modeBox));
    }

    private class GuiBlueprintScaleControl extends GuiParent {

        private final GuiTextfield textfield;

        public GuiBlueprintScaleControl(float value) {
            super("item_model_scale", GuiFlow.STACK_X);
            spacing = 1;
            textfield = new GuiTextfield("value", format(value)).setDim(34, 10).setFloatOnly();
            add(textfield.setExpandableX());
            GuiParent buttons = new GuiParent(GuiFlow.STACK_Y);
            buttons.spacing = 0;
            buttons.add(new GuiButtonHoldSlim("+", x -> step(ITEM_MODEL_SCALE_STEP)).setTranslate("gui.plus").setDim(6, 3));
            buttons.add(new GuiButtonHoldSlim("-", x -> step(-ITEM_MODEL_SCALE_STEP)).setTranslate("gui.minus").setDim(6, 3));
            add(buttons);
        }

        public float getValue() {
            return Mth.clamp(textfield.parseFloat(), ItemLittleBlueprint.MIN_ITEM_MODEL_SCALE, ItemLittleBlueprint.MAX_ITEM_MODEL_SCALE);
        }

        private void step(float amount) {
            setValue(getValue() + amount);
            applyModelSettingsNow();
        }

        private void setValue(float value) {
            textfield.setText(format(Mth.clamp(value, ItemLittleBlueprint.MIN_ITEM_MODEL_SCALE, ItemLittleBlueprint.MAX_ITEM_MODEL_SCALE)));
        }

        private String format(float value) {
            return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        @Override
        public ControlFormatting getControlFormatting() {
            return ControlFormatting.TRANSPARENT;
        }

    }
    
}
