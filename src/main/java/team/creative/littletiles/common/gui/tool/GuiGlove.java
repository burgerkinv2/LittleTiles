package team.creative.littletiles.common.gui.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.VAlign;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.collection.GuiStackSelector;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiShowItem;
import team.creative.creativecore.common.gui.event.GuiControlChangedEvent;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.text.TextBuilder;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.client.gui.LittleElementAppearanceGuiRegistry.GuiPlacement;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.element.LittleElement.NotBlockException;
import team.creative.littletiles.common.gui.LittleGuiUtils;
import team.creative.littletiles.common.gui.control.GuiElementAppearanceControls;
import team.creative.littletiles.common.gui.control.GuiGridConfig;
import team.creative.littletiles.common.item.ItemMultiTiles;
import team.creative.littletiles.common.placement.mode.PlacementMode;
import team.creative.littletiles.common.placement.setting.PlacementPlayerSetting;

public class GuiGlove extends GuiConfigureTool {

    private final GuiPlacement appearancePlacement;

    public GuiGlove(ContainerSlotView view) {
        this(view, GuiPlacement.PRIMARY);
    }

    public GuiGlove(ContainerSlotView view, GuiPlacement appearancePlacement) {
        super("glove", appearancePlacement == GuiPlacement.SECONDARY ? 160 : 230, appearancePlacement == GuiPlacement.SECONDARY ? 80 : 200, view);
        this.appearancePlacement = appearancePlacement != null ? appearancePlacement : GuiPlacement.SECONDARY;
        registerEventChanged(x -> {
            if (x.control.is("preview"))
                rebuildAppearanceControls();
            if (x.control.is("picker", "preview") || x.control.name.startsWith("control_"))
                updateLabel();
        });
        flow = GuiFlow.STACK_Y;
        valign = VAlign.STRETCH;
        
        registerEventChanged(x -> {
            if (x.control.is("mode")) {
                GuiComboBox<PlacementMode> modeBox = (GuiComboBox<PlacementMode>) x.control;
                TextBuilder builder = new TextBuilder();
                if (modeBox.selected().canPlaceStructures())
                    builder.text("" + ChatFormatting.BOLD).translate("placement.mode.placestructure").text("" + ChatFormatting.WHITE).newLine();
                builder.translate(modeBox.selected().translatableKey() + ".tooltip");
                ((GuiLabel) get("text")).setTitle(builder.build());
                LittleTilesClient.placementMode(modeBox.selected());
            }
        });
    }
    
    @Override
    public void create() {
        if (!isClient())
            return;

        if (appearancePlacement == GuiPlacement.SECONDARY) {
            LittleElement element = LittleElement.getOrDefault(tool.get());
            ItemStack material = element.getBlock().getStack();
            GuiElementAppearanceControls appearances = new GuiElementAppearanceControls(appearancePlacement);
            add(appearances);
            appearances.rebuild(material, element);
            return;
        }
        
        GuiParent upper = new GuiParent();
        add(upper);
        
        GuiParent left = new GuiParent(GuiFlow.STACK_Y).setAlign(Align.STRETCH);
        upper.add(left);
        
        LittleElement element = LittleElement.getOrDefault(tool.get());
        Color color = new Color(element.color);
        left.add(new GuiColorPicker("picker", color, LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        
        GuiParent parent = new GuiParent(GuiFlow.STACK_X).setVAlign(VAlign.CENTER);
        left.add(parent);
        parent.add(new GuiShowItem("item").setDim(60, 60));
        parent.add(new GuiGridConfig("grid", getPlayer(), PlacementPlayerSetting.grid(getPlayer()), LittleTilesClient::grid));
        
        GuiStackSelector selector = new GuiStackSelector("preview", getPlayer(), LittleGuiUtils.getCollector(getPlayer()), true);
        selector.setSelectedForce(element.getBlock().getStack());
        left.add(selector);
        GuiElementAppearanceControls appearances = new GuiElementAppearanceControls(appearancePlacement);
        left.add(appearances);
        appearances.rebuild(selector.getSelected(), element);
        
        GuiParent right = new GuiParent(GuiFlow.STACK_Y).setAlign(Align.STRETCH);
        upper.add(right);
        
        GuiComboBox<PlacementMode> modeBox = new GuiComboBox<>("mode", PlacementMode.map());
        modeBox.select(PlacementPlayerSetting.placementMode(getPlayer()));
        right.add(modeBox);
        right.add(new GuiLabel("text"));
        raiseEvent(new GuiControlChangedEvent(modeBox));
        
        updateLabel();
    }
    
    public void updateLabel() {
        if (appearancePlacement == GuiPlacement.SECONDARY)
            return;

        GuiStackSelector selector = (GuiStackSelector) get("preview");
        ItemStack selected = selector.getSelected();
        GuiColorPicker picker = get("picker");
        LittleElement fallback = LittleElement.getOrDefault(tool.get());

        LittleElement element;
        try {
            element = LittleElement.of(selected, picker.color.toInt());
        } catch (NotBlockException e) {
            element = new LittleElement(fallback, picker.color.toInt());
        }
        element.setAppearance(get(GuiElementAppearanceControls.NAME, GuiElementAppearanceControls.class).collect(selected, fallback));

        get("item", GuiShowItem.class).stack = ItemMultiTiles.of(element);
    }

    public void rebuildAppearanceControls() {
        if (appearancePlacement == GuiPlacement.SECONDARY) {
            LittleElement element = LittleElement.getOrDefault(tool.get());
            GuiElementAppearanceControls appearances = get(GuiElementAppearanceControls.NAME);
            if (appearances != null)
                appearances.rebuild(element.getBlock().getStack(), element);
            return;
        }

        GuiStackSelector selector = (GuiStackSelector) get("preview");
        GuiElementAppearanceControls appearances = get(GuiElementAppearanceControls.NAME);
        if (selector != null && appearances != null)
            appearances.rebuild(selector.getSelected(), LittleElement.getOrDefault(tool.get()));
    }

    @Override
    public boolean saveConfiguration(PatchedDataComponentMap data) {
        if (appearancePlacement == GuiPlacement.SECONDARY) {
            LittleElement element = LittleElement.getOrDefault(tool.get());
            ItemStack material = element.getBlock().getStack();
            element.setAppearance(get(GuiElementAppearanceControls.NAME, GuiElementAppearanceControls.class).collect(material, element));
            data.set(LittleTilesRegistry.ELEMENT.get(), element);
            return true;
        }

        GuiColorPicker picker = get("picker");

        ItemStack selected = get("preview", GuiStackSelector.class).getSelected();
        LittleElement fallback = LittleElement.getOrDefault(tool.get());
        LittleElement element;
        try {
            element = LittleElement.of(selected, picker.color.toInt());
        } catch (NotBlockException e) {
            element = new LittleElement(fallback, picker.color.toInt());
        }
        element.setAppearance(get(GuiElementAppearanceControls.NAME, GuiElementAppearanceControls.class).collect(selected, fallback));
        data.set(LittleTilesRegistry.ELEMENT.get(), element);

        return true;
    }
}
