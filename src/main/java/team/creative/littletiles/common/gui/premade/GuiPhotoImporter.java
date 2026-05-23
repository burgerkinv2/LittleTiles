package team.creative.littletiles.common.gui.premade;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.mojang.blaze3d.Blaze3D;

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
import team.creative.creativecore.common.gui.control.simple.GuiCounter;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiSlider;
import team.creative.creativecore.common.gui.control.simple.GuiTextfield;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.integration.ScreenEventListener;
import team.creative.creativecore.common.gui.sync.GuiSyncLocal;
import team.creative.creativecore.common.util.mc.PlayerUtils;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.gui.premade.photo.PhotoImportOptions;
import team.creative.littletiles.common.gui.premade.photo.PhotoImportSource;
import team.creative.littletiles.common.gui.premade.photo.PhotoImporterReader;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public class GuiPhotoImporter extends GuiLayer {
    
    private static final int DEFAULT_MAX_PIXELS = 4096;
    
    public final Container outputSlot = new SimpleContainer(1);
    private ImageInfo imageInfo;
    private boolean adjustingAspect;
    
    public final GuiSyncLocal<CompoundTag> IMPORT_PHOTO = getSyncHolder().register("import_photo", nbt -> {
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
    
    public GuiPhotoImporter() {
        super("photo_importer", 260, 230);
        flow = GuiFlow.STACK_Y;
        align = Align.STRETCH;
    }
    
    @Override
    public void create() {
        GuiComboBox<PhotoImportSource> source = new GuiComboBox<>("source", new TextMapBuilder<PhotoImportSource>().addComponent(PhotoImportSource.values(), x -> Component
                .translatable(x.translationKey)));
        add(new GuiLabeledControl("gui.photo_importer.source", source.setExpandableX()));
        
        GuiTextfield path = new GuiTextfield("path") {
            private int lastButton = -1;
            private double lastClickTime;
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean result = super.mouseClicked(mouseX, mouseY, button);
                double time = Blaze3D.getTime();
                if (button == 0 && source() == PhotoImportSource.FILE && lastButton == button && time - lastClickTime < ScreenEventListener.DOUBLE_CLICK_TIME)
                    openFileDialog();
                lastButton = button;
                lastClickTime = time;
                return result;
            }
        };
        path.setMaxStringLength(Integer.MAX_VALUE);
        path.setSuggestion(Component.translatable("gui.photo_importer.path.file_hint").getString());
        add(new GuiLabeledControl("gui.photo_importer.path", path.setExpandableX()));
        
        GuiParent sizeRow = new GuiParent(GuiFlow.STACK_X);
        add(sizeRow);
        sizeRow.add(new GuiLabeledControl("gui.photo_importer.width", new GuiCounter("width", 16, 1, Integer.MAX_VALUE)));
        sizeRow.add(new GuiLabeledControl("gui.photo_importer.height", new GuiCounter("height", 16, 1, Integer.MAX_VALUE)));
        
        GuiParent optionRow = new GuiParent(GuiFlow.STACK_X);
        add(optionRow);
        optionRow.add(new GuiCheckBox("ignore_alpha", false).setTranslate("gui.photo_importer.ignore_alpha"));
        optionRow.add(new GuiCheckBox("keep_aspect", true).setTranslate("gui.photo_importer.keep_aspect"));
        add(new GuiCheckBox("create_structure", true).setTranslate("gui.photo_importer.create_structure"));
        
        add(new GuiLabeledControl("gui.grid", new GuiComboBox<LittleGrid>("grid", LittleTiles.CONFIG.build.get(getPlayer()).gridBuilder()).setExpandableX()));
        add(new GuiLabeledControl("gui.photo_importer.color_accuracy", new GuiSlider("color_accuracy", 1, 0, 1).setDim(80, 10)));
        
        GuiLeftRightBox actions = new GuiLeftRightBox();
        add(actions.setExpandableX());
        actions.addLeft(new GuiButton("read_size", x -> updateImageSize()).setTranslate("gui.photo_importer.read_size"));
        actions.addLeft(new GuiButton("auto_scale", x -> autoScale()).setTranslate("gui.photo_importer.auto_scale"));
        actions.addRight(new GuiButton("import", x -> importImage()).setTranslate("gui.photo_importer.import"));
        
        add(new GuiLabel("status").setTitle(Component.empty()));
        add(new GuiInventoryGrid("output", outputSlot));
        add(new GuiPlayerInventoryGrid(getPlayer()).setUnexpandableX());
        
        registerEventChanged(x -> {
            if (x.control.is("source")) {
                updateSourceControls();
                imageInfo = null;
            }
            if (!adjustingAspect && x.control.is("width") && get("keep_aspect", GuiCheckBox.class).value)
                adjustHeightToAspect();
            if (!adjustingAspect && x.control.is("height") && get("keep_aspect", GuiCheckBox.class).value)
                adjustWidthToAspect();
        });
        updateSourceControls();
    }
    
    @Override
    public void closed() {
        super.closed();
        PlayerUtils.addOrDrop(getPlayer(), outputSlot);
    }
    
    private PhotoImportSource source() {
        GuiComboBox<PhotoImportSource> source = get("source");
        return source.selected(PhotoImportSource.FILE);
    }
    
    private void updateSourceControls() {
        PhotoImportSource source = source();
        get("path", GuiTextfield.class).setSuggestion(Component.translatable(source == PhotoImportSource.FILE ? "gui.photo_importer.path.file_hint" : "gui.photo_importer.path.url_hint")
                .getString());
    }
    
    private void openFileDialog() {
        try {
            String selected;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(3);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.flip();
                
                selected = TinyFileDialogs.tinyfd_openFileDialog(Component.translatable("gui.photo_importer.file_dialog.title").getString(), get("path", GuiTextfield.class)
                        .getText(), filters, Component.translatable("gui.photo_importer.file_dialog.filter").getString(), false);
            }
            if (selected != null && !selected.isBlank()) {
                get("path", GuiTextfield.class).setText(selected);
                updateImageSize();
            }
        } catch (Throwable e) {
            LittleTiles.LOGGER.warn("Could not open photo importer file dialog", e);
            status("gui.photo_importer.status.failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    
    private void importImage() {
        try {
            BufferedImage image = readImage();
            int width = get("width", GuiCounter.class).getValue();
            int height = get("height", GuiCounter.class).getValue();
            if (image.getWidth() != width || image.getHeight() != height)
                image = PhotoImporterReader.resize(image, width, height);
            IMPORT_PHOTO.send(PhotoImporterReader.toBlueprintContent(image, options()));
            status("gui.photo_importer.status.imported", width * height);
        } catch (Exception e) {
            LittleTiles.LOGGER.warn("Could not import photo", e);
            status("gui.photo_importer.status.failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    
    private void updateImageSize() {
        try {
            BufferedImage image = readImage();
            cacheImageInfo(image);
            setSizeFields(image.getWidth(), image.getHeight());
            status("gui.photo_importer.status.loaded", image.getWidth(), image.getHeight());
        } catch (Exception e) {
            status("gui.photo_importer.status.failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    
    private void autoScale() {
        try {
            ImageInfo info = imageInfo();
            int width = info.width();
            int height = info.height();
            int pixels = width * height;
            if (pixels > DEFAULT_MAX_PIXELS) {
                double scale = Math.sqrt(DEFAULT_MAX_PIXELS / (double) pixels);
                width = Math.max(1, (int) Math.floor(width * scale));
                height = Math.max(1, (int) Math.floor(height * scale));
            }
            setSizeFields(width, height);
            status("gui.photo_importer.status.loaded", width, height);
        } catch (Exception e) {
            status("gui.photo_importer.status.failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    
    private void adjustHeightToAspect() {
        try {
            ImageInfo info = imageInfo();
            int width = get("width", GuiCounter.class).getValue();
            setSizeFields(width, Math.max(1, (int) Math.round(width * (info.height() / (double) info.width()))));
        } catch (Exception ignored) {}
    }
    
    private void adjustWidthToAspect() {
        try {
            ImageInfo info = imageInfo();
            int height = get("height", GuiCounter.class).getValue();
            setSizeFields(Math.max(1, (int) Math.round(height * (info.width() / (double) info.height()))), height);
        } catch (Exception ignored) {}
    }
    
    private void setSizeFields(int width, int height) {
        adjustingAspect = true;
        try {
            get("width", GuiCounter.class).setValue(width);
            get("height", GuiCounter.class).setValue(height);
        } finally {
            adjustingAspect = false;
        }
    }
    
    private ImageInfo imageInfo() throws IOException {
        PhotoImportSource source = source();
        String path = get("path", GuiTextfield.class).getText();
        if (imageInfo != null && imageInfo.matches(source, path))
            return imageInfo;
        BufferedImage image = readImage();
        cacheImageInfo(image);
        return imageInfo;
    }
    
    private void cacheImageInfo(BufferedImage image) {
        imageInfo = new ImageInfo(source(), get("path", GuiTextfield.class).getText(), image.getWidth(), image.getHeight());
    }
    
    private PhotoImportOptions options() {
        GuiComboBox<LittleGrid> grid = get("grid");
        return new PhotoImportOptions(grid.selected(LittleGrid.overallDefault()), get("ignore_alpha", GuiCheckBox.class).value, get("create_structure", GuiCheckBox.class).value,
            get("color_accuracy", GuiSlider.class).getValue(), DEFAULT_MAX_PIXELS, LittleTilesRegistry.CLEAN.value().defaultBlockState());
    }
    
    private BufferedImage readImage() throws IOException {
        BufferedImage image;
        PhotoImportSource source = source();
        if (source == PhotoImportSource.FILE)
            image = PhotoImporterReader.readFile(get("path", GuiTextfield.class).getText());
        else if (source == PhotoImportSource.URL)
            image = PhotoImporterReader.readURL(get("path", GuiTextfield.class).getText());
        else
            throw new IOException("Unsupported source " + source);
        if (image == null)
            throw new IOException("Could not read image");
        return image;
    }
    
    private void status(String key, Object... args) {
        get("status", GuiLabel.class).setTitle(Component.translatable(key, args));
    }
    
    private record ImageInfo(PhotoImportSource source, String path, int width, int height) {
        
        private boolean matches(PhotoImportSource source, String path) {
            return this.source == source && this.path.equals(path);
        }
    }
    
}
