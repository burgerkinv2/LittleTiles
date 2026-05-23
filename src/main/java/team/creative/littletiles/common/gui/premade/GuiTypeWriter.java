package team.creative.littletiles.common.gui.premade;

import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiLayer;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.collection.GuiComboBox;
import team.creative.creativecore.common.gui.control.inventory.GuiInventoryGrid;
import team.creative.creativecore.common.gui.control.inventory.GuiPlayerInventoryGrid;
import team.creative.creativecore.common.gui.control.parent.GuiLabeledControl;
import team.creative.creativecore.common.gui.control.parent.GuiLeftRightBox;
import team.creative.creativecore.common.gui.control.simple.GuiButton;
import team.creative.creativecore.common.gui.control.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiCounter;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiSlider;
import team.creative.creativecore.common.gui.control.simple.GuiTextfield;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.sync.GuiSyncLocal;
import team.creative.creativecore.common.util.mc.PlayerUtils;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.gui.premade.text.TypeWriterReader;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public class GuiTypeWriter extends GuiLayer {
    
    public final Container outputSlot = new SimpleContainer(1);
    
    public final GuiSyncLocal<CompoundTag> IMPORT_TEXT = getSyncHolder().register("import_text", nbt -> {
        ItemStack stack = outputSlot.getItem(0);
        if (stack.isEmpty()) {
            if (!getPlayer().isCreative())
                return;
            stack = new ItemStack(LittleTilesRegistry.BLUEPRINT.value());
            outputSlot.setItem(0, stack);
        }
        if (stack.getItem() instanceof ItemLittleBlueprint) {
            CompoundTag stackTag = ILittleTool.getData(stack);
            stackTag.put(ItemLittleBlueprint.CONTENT_KEY, nbt);
            ILittleTool.setData(stack, stackTag);
            get("output", GuiInventoryGrid.class).setChanged();
        }
    });
    
    public GuiTypeWriter() {
        super("type_writer", 260, 250);
        flow = GuiFlow.STACK_Y;
        align = Align.STRETCH;
    }
    
    @Override
    public void create() {
        add(new GuiLabeledControl("gui.type_writer.text", new GuiTextfield("text").setMaxStringLength(Integer.MAX_VALUE).setExpandableX()));
        
        GuiTextfield search = new GuiTextfield("search");
        search.setExpandableX();
        search.setSuggestion(Component.translatable("gui.type_writer.search_hint").getString());
        add(new GuiLabeledControl("gui.type_writer.search", search));
        
        GuiComboBox<String> fonts = new GuiComboBox<>("font", fontBuilder(TypeWriterReader.fonts()));
        fonts.setExpandableX();
        add(new GuiLabeledControl("gui.type_writer.font", fonts));
        
        GuiParent sizeRow = new GuiParent(GuiFlow.STACK_X);
        add(sizeRow);
        GuiCounter fontSize = new GuiCounter("font_size", 48, 1, 512);
        fontSize.textfield.setDim(26, 10);
        sizeRow.add(new GuiLabeledControl("gui.type_writer.font_size", fontSize));
        sizeRow.add(new GuiLabeledControl("gui.grid", new GuiComboBox<LittleGrid>("grid", photoGridBuilder())));
        
        GuiParent styleRow = new GuiParent(GuiFlow.STACK_X);
        add(styleRow);
        styleRow.add(new GuiCheckBox("bold", false).setTranslate("gui.type_writer.bold"));
        styleRow.add(new GuiCheckBox("italic", false).setTranslate("gui.type_writer.italic"));
        styleRow.add(new GuiCheckBox("underline", false).setTranslate("gui.type_writer.underline"));
        styleRow.add(new GuiCheckBox("strikethrough", false).setTranslate("gui.type_writer.strikethrough"));
        
        add(new GuiLabeledControl("gui.type_writer.rotation", new GuiSlider("rotation", 0, 0, 360).setDim(80, 10)));
        add(new GuiLabeledControl("gui.type_writer.color", new GuiColorPicker("color", new Color(0, 0, 0, 255), LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG
                .getMinimumTransparency(getPlayer())).setExpandableX()));
        add(new GuiCheckBox("create_structure", true).setTranslate("gui.photo_importer.create_structure"));
        
        GuiParent infoRow = new GuiParent(GuiFlow.STACK_X);
        add(infoRow);
        infoRow.add(new GuiLabel("size").setTitle(Component.translatable("gui.type_writer.size", 0, 0)));
        infoRow.add(new GuiButton("measure", x -> updateTextSize()).setTranslate("gui.type_writer.measure"));
        
        GuiLeftRightBox actions = new GuiLeftRightBox();
        add(actions.setExpandableX());
        actions.addLeft(new GuiButton("reload_fonts", x -> reloadFonts()).setTranslate("gui.type_writer.reload_fonts"));
        actions.addRight(new GuiButton("print", x -> printText()).setTranslate("gui.type_writer.print"));
        
        add(new GuiLabel("status").setTitle(Component.empty()));
        add(new GuiInventoryGrid("output", outputSlot));
        add(new GuiPlayerInventoryGrid(getPlayer()).setUnexpandableX());
        
        registerEventChanged(x -> {
            if (x.control.is("search"))
                filterFonts();
            if (x.control.is("text") || x.control.is("font") || x.control.is("font_size") || x.control.is("bold") || x.control.is("italic") || x.control.is("underline") || x.control
                    .is("strikethrough"))
                updateTextSize();
        });
    }
    
    @Override
    public void closed() {
        super.closed();
        PlayerUtils.addOrDrop(getPlayer(), outputSlot);
    }
    
    private void printText() {
        try {
            BufferedImage image = TypeWriterReader.render(text(), font(), fontSize(), get("color", GuiColorPicker.class).color, get("rotation", GuiSlider.class).getValue(), attributes());
            GuiComboBox<LittleGrid> grid = get("grid");
            IMPORT_TEXT.send(TypeWriterReader.toBlueprintContent(image, grid.selected(LittleGrid.overallDefault()), get("create_structure", GuiCheckBox.class).value));
            status("gui.type_writer.status.printed", image.getWidth(), image.getHeight());
            updateTextSize(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            LittleTiles.LOGGER.warn("Could not print text", e);
            status("gui.photo_importer.status.failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    
    private void updateTextSize() {
        try {
            TypeWriterReader.TextInfo info = TypeWriterReader.measure(text(), font(), fontSize(), attributes());
            updateTextSize(info.width(), info.height());
        } catch (Exception ignored) {
            updateTextSize(0, 0);
        }
    }
    
    private void updateTextSize(int width, int height) {
        get("size", GuiLabel.class).setTitle(Component.translatable("gui.type_writer.size", width, height));
    }
    
    private void filterFonts() {
        String query = get("search", GuiTextfield.class).getText().toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String font : TypeWriterReader.fonts())
            if (query.isBlank() || font.toLowerCase().contains(query))
                matches.add(font);
        GuiComboBox<String> fonts = get("font");
        fonts.set(fontBuilder(matches));
        if (!matches.isEmpty())
            fonts.select(matches.getFirst());
    }
    
    private void reloadFonts() {
        TypeWriterReader.reloadFonts();
        GuiComboBox<String> fonts = get("font");
        fonts.set(fontBuilder(TypeWriterReader.fonts()));
        status("gui.type_writer.status.fonts_loaded", TypeWriterReader.fonts().size());
    }
    
    private String text() {
        return get("text", GuiTextfield.class).getText();
    }
    
    private String font() {
        GuiComboBox<String> font = get("font");
        return font.selected(TypeWriterReader.fonts().isEmpty() ? "Serif" : TypeWriterReader.fonts().getFirst());
    }
    
    private int fontSize() {
        return Math.max(1, get("font_size", GuiCounter.class).getValue());
    }
    
    private Map<TextAttribute, Object> attributes() {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        if (get("bold", GuiCheckBox.class).value)
            attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        if (get("italic", GuiCheckBox.class).value)
            attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        if (get("underline", GuiCheckBox.class).value)
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        if (get("strikethrough", GuiCheckBox.class).value)
            attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return attributes;
    }
    
    private TextMapBuilder<String> fontBuilder(List<String> fonts) {
        return new TextMapBuilder<String>().addComponent(fonts, Component::literal);
    }
    
    private TextMapBuilder<LittleGrid> photoGridBuilder() {
        List<LittleGrid> grids = new ArrayList<>();
        for (int grid = 1; grid <= 256; grid *= 2) {
            LittleGrid littleGrid = LittleGrid.tryGet(grid);
            if (littleGrid != null)
                grids.add(littleGrid);
        }
        return new TextMapBuilder<LittleGrid>().addComponent(grids, x -> Component.literal("" + x.count));
    }
    
    private void status(String key, Object... args) {
        get("status", GuiLabel.class).setTitle(Component.translatable(key, args));
    }
    
}
