package team.creative.littletiles.common.gui.premade.photo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;

import javax.imageio.ImageIO;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.math.box.LittleBox;

public class PhotoImporterReader {
    
    public static InputStream loadURL(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        return connection.getInputStream();
    }
    
    public static BufferedImage readFile(String path) throws IOException {
        return ImageIO.read(new File(path));
    }
    
    public static BufferedImage readURL(String url) throws IOException {
        try (InputStream in = loadURL(url)) {
            return ImageIO.read(in);
        }
    }
    
    public static BufferedImage readResource(InputStream stream) throws IOException {
        try (stream) {
            return ImageIO.read(stream);
        }
    }
    
    public static ResourceLocation texturePath(ResourceLocation texture) {
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
    }
    
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }
    
    public static LittleGroup toGroup(BufferedImage source, PhotoImportOptions options) {
        BufferedImage image = normalize(source);
        LittleGroup group = new LittleGroup();
        LittleElement element = new LittleElement(options.state() != null ? options.state() : LittleTilesRegistry.CLEAN.value().defaultBlockState(), ColorUtils.WHITE);
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        
        for (int row = 0; row < height; row++) {
            int sourceRow = height - row - 1;
            int runStart = -1;
            int runColor = 0;
            for (int col = 0; col <= width; col++) {
                int color = 0;
                boolean visible = false;
                if (col < width) {
                    color = roundColor(pixels[sourceRow * width + col], options.colorAccuracy());
                    visible = !ColorUtils.isInvisible(color);
                    if (visible && options.ignoreAlpha())
                        color = ColorUtils.setAlpha(color, 255);
                }

                if (visible && runStart == -1) {
                    runStart = col;
                    runColor = color;
                    continue;
                }

                if (visible && color == runColor)
                    continue;

                if (runStart != -1) {
                    group.addFast(options.grid(), new LittleElement(element, runColor), new LittleBox(runStart, row, 0, col, row + 1, 1));
                    runStart = -1;
                }

                if (visible) {
                    runStart = col;
                    runColor = color;
                }
            }
        }
        
        group.combine(true);
        group.convertToSmallest();
        if (options.createStructure())
            group = wrapStructure(group);
        return group;
    }
    
    private static LittleGroup wrapStructure(LittleGroup group) {
        CompoundTag structure = new CompoundTag();
        structure.putString("id", "fixed");
        return new LittleGroup(structure, group, Collections.EMPTY_LIST);
    }

    public static CompoundTag toBlueprintContent(BufferedImage image, PhotoImportOptions options) {
        return LittleGroup.save(toGroup(image, options));
    }
    
    private static BufferedImage normalize(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB)
            return image;
        BufferedImage normalized = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return normalized;
    }
    
    private static int roundColor(int color, double accuracy) {
        int step = (int) (Math.abs(accuracy - 1) * 128);
        if (step <= 0)
            step = 1;
        int r = clamp(step * Math.round((float) ColorUtils.red(color) / step));
        int g = clamp(step * Math.round((float) ColorUtils.green(color) / step));
        int b = clamp(step * Math.round((float) ColorUtils.blue(color) / step));
        return ColorUtils.rgba(r, g, b, ColorUtils.alpha(color));
    }
    
    private static int clamp(int value) {
        if (value < 0)
            return 0;
        if (value > 255)
            return 255;
        return value;
    }
    
    public static PhotoImportOptions defaultOptions() {
        return new PhotoImportOptions(team.creative.littletiles.common.grid.LittleGrid.overallDefault(), false, true, 1, Blocks.STONE.defaultBlockState());
    }
    
}
