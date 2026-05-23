package team.creative.littletiles.common.gui.premade.text;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.common.gui.premade.photo.PhotoImportOptions;
import team.creative.littletiles.common.gui.premade.photo.PhotoImporterReader;
import team.creative.littletiles.common.grid.LittleGrid;

public class TypeWriterReader {
    
    private static final String FONT_FOLDER = "fonts";
    private static List<String> cachedFonts;
    
    public static List<String> fonts() {
        if (cachedFonts != null)
            return cachedFonts;
        
        registerCustomFonts();
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        List<String> fonts = new ArrayList<>(names.length);
        for (String name : names)
            fonts.add(name);
        fonts.sort(String.CASE_INSENSITIVE_ORDER);
        cachedFonts = List.copyOf(fonts);
        return cachedFonts;
    }
    
    public static void reloadFonts() {
        cachedFonts = null;
        fonts();
    }
    
    private static void registerCustomFonts() {
        File folder = new File(FONT_FOLDER);
        if (!folder.exists())
            folder.mkdirs();
        File[] files = folder.listFiles(file -> {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return file.isFile() && (name.endsWith(".ttf") || name.endsWith(".otf"));
        });
        if (files == null)
            return;
        List<File> sorted = new ArrayList<>(List.of(files));
        sorted.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (File file : sorted)
            try {
                environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, file));
            } catch (FontFormatException | IOException ignored) {}
        
        File readme = new File(folder, "README.txt");
        if (!readme.exists())
            try {
                Files.writeString(readme.toPath(), "Place .ttf or .otf files in this folder, then reopen the Type Writer GUI.\n");
            } catch (IOException ignored) {}
    }
    
    public static BufferedImage render(String text, String fontName, int fontSize, Color color, double rotation, @Nullable Map<TextAttribute, Object> attributes) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("Text is empty");
        
        Font font = new Font(fontName, Font.PLAIN, Math.max(1, fontSize));
        if (attributes != null && !attributes.isEmpty())
            font = font.deriveFont(attributes);
        
        BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = metricsImage.createGraphics();
        metricsGraphics.setFont(font);
        FontMetrics metrics = metricsGraphics.getFontMetrics();
        int width = Math.max(1, metrics.stringWidth(text) + 10);
        int height = Math.max(1, metrics.getHeight() + 1);
        int ascent = metrics.getAscent();
        metricsGraphics.dispose();
        
        double radians = Math.toRadians(rotation);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int rotatedWidth = Math.max(1, (int) Math.floor(width * cos + height * sin));
        int rotatedHeight = Math.max(1, (int) Math.floor(height * cos + width * sin));
        
        BufferedImage image = new BufferedImage(rotatedWidth, rotatedHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        AffineTransform transform = new AffineTransform();
        transform.translate((rotatedWidth - width) / 2D, (rotatedHeight - height) / 2D);
        transform.rotate(radians, width / 2D, height / 2D);
        graphics.setTransform(transform);
        graphics.setFont(font);
        graphics.setColor(new java.awt.Color(color.toInt(), true));
        graphics.drawString(text, 0, ascent);
        graphics.dispose();
        return image;
    }
    
    public static TextInfo measure(String text, String fontName, int fontSize, @Nullable Map<TextAttribute, Object> attributes) {
        BufferedImage image = render(text == null || text.isEmpty() ? " " : text, fontName, fontSize, new Color(255, 255, 255, 255), 0, attributes);
        return new TextInfo(image.getWidth(), image.getHeight());
    }
    
    public static CompoundTag toBlueprintContent(BufferedImage image, LittleGrid grid, boolean createStructure) {
        return PhotoImporterReader.toBlueprintContent(image, new PhotoImportOptions(grid, false, createStructure, 1, LittleTilesRegistry.CLEAN.value().defaultBlockState()));
    }
    
    public record TextInfo(int width, int height) {}
    
}
