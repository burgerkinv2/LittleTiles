package team.creative.littletiles.common.gui.control;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.parent.GuiLabeledControl;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.littletiles.api.client.gui.LittleElementAppearanceGuiRegistry;
import team.creative.littletiles.api.client.gui.LittleElementAppearanceGuiRegistry.Control;
import team.creative.littletiles.api.client.gui.LittleElementAppearanceGuiRegistry.GuiPlacement;
import team.creative.littletiles.api.common.block.LittleElementAppearanceRegistry;
import team.creative.littletiles.common.block.little.element.LittleElement;

public class GuiElementAppearanceControls extends GuiParent {
    
    public static final String NAME = "appearance";
    private final GuiPlacement placement;
    
    public GuiElementAppearanceControls() {
        this(GuiPlacement.SECONDARY);
    }
    
    public GuiElementAppearanceControls(GuiPlacement placement) {
        super(NAME, GuiFlow.STACK_Y);
        this.placement = placement != null ? placement : GuiPlacement.SECONDARY;
        setExpandableX();
    }
    
    public void rebuild(ItemStack material, LittleElement fallback) {
        CompoundTag appearance = baseAppearance(material, fallback);
        clear();
        
        List<Control> controls = LittleElementAppearanceGuiRegistry.getControls(getPlayer(), material, appearance, placement);
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            if (control.key == null || control.key.isBlank() || control.options.isEmpty())
                continue;
            
            TextMapBuilder<String> builder = new TextMapBuilder<>();
            for (LittleElementAppearanceGuiRegistry.Option option : control.options)
                builder.addComponent(option.value, option.title);
            
            GuiComboBox<String> comboBox = new GuiComboBox<>(controlName(i), control.selected, builder);
            add(new GuiLabeledControl(control.title, comboBox.setExpandableX()).setExpandableX());
        }
    }
    
    public CompoundTag collect(ItemStack material, LittleElement fallback) {
        CompoundTag appearance = baseAppearance(material, fallback);
        List<Control> controls = LittleElementAppearanceGuiRegistry.getControls(getPlayer(), material, appearance, placement);
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            GuiComboBox<String> comboBox = get(controlName(i));
            if (comboBox == null)
                continue;
            String value = comboBox.selected("");
            if (value.isBlank())
                appearance.remove(control.key);
            else
                appearance.putString(control.key, value);
        }
        return appearance.isEmpty() ? null : appearance;
    }
    
    private static String controlName(int index) {
        return "control_" + index;
    }
    
    private static CompoundTag baseAppearance(ItemStack material, LittleElement fallback) {
        CompoundTag appearance = LittleElementAppearanceRegistry.getAppearance(material);
        if (appearance == null && fallback != null && sameBlock(material, fallback))
            appearance = fallback.appearance();
        return appearance != null ? appearance : new CompoundTag();
    }
    
    private static boolean sameBlock(ItemStack material, LittleElement element) {
        return material != null && !material.isEmpty() && Block.byItem(material.getItem()) == element.getState().getBlock();
    }
}
