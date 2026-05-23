package team.creative.littletiles.common.math.measure;

import java.util.List;
import java.util.Locale;

import team.creative.littletiles.common.grid.LittleGrid;

public final class LittleMeasurementUnits {

    private LittleMeasurementUnits() {}

    public static final String TILE = "tile";
    public static final String METER = "m";
    public static final String CENTIMETER = "cm";
    public static final String MILLIMETER = "mm";
    public static final String INCH = "in";
    public static final String FOOT = "feet";

    public static final List<String> ALL = List.of(TILE, METER, CENTIMETER, MILLIMETER, INCH, FOOT);

    public static String defaultUnit() {
        return TILE;
    }

    public static String format(double blocks, LittleGrid grid, String unit) {
        if (unit == null || TILE.equals(unit))
            return formatBlockTile(blocks, grid);
        return formatDecimal(convertFromBlocks(blocks, unit)) + unit;
    }

    public static String formatArea(double blocks, LittleGrid grid, String unit) {
        if (unit == null || TILE.equals(unit))
            return formatBlockTile(blocks, grid, 2);
        double factor = convertFromBlocks(1, unit);
        return formatDecimal(blocks * factor * factor) + unit + "^2";
    }

    public static String formatVolume(double blocks, LittleGrid grid, String unit) {
        if (unit == null || TILE.equals(unit))
            return formatBlockTile(blocks, grid, 3);
        double factor = convertFromBlocks(1, unit);
        return formatDecimal(blocks * factor * factor * factor) + unit + "^3";
    }

    public static String formatBlockTile(double blocks, LittleGrid grid) {
        return formatBlockTile(blocks, grid, 1);
    }

    private static String formatBlockTile(double blocks, LittleGrid grid, int power) {
        if (grid == null)
            grid = LittleGrid.MIN;
        long scale = 1;
        for (int i = 0; i < power; i++)
            scale *= grid.count;
        long total = Math.round(blocks * scale);
        if (total <= 0)
            return "0";
        long b = total / scale;
        long t = total % scale;
        String blockUnit = power == 1 ? "B" : "B^" + power;
        String tileUnit = power == 1 ? "t" : "t^" + power;
        if (t == 0)
            return b + blockUnit;
        if (b == 0)
            return t + "/" + scale + tileUnit;
        return b + blockUnit + " " + t + "/" + scale + tileUnit;
    }

    private static double convertFromBlocks(double blocks, String unit) {
        return switch (unit) {
            case METER -> blocks;
            case CENTIMETER -> blocks * 100.0;
            case MILLIMETER -> blocks * 1000.0;
            case INCH -> blocks * 39.3700787;
            case FOOT -> blocks * 3.28084;
            default -> blocks;
        };
    }

    private static String formatDecimal(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value))
            return Long.toString((long) value);
        String s = String.format(Locale.ROOT, "%.3f", value);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0')
            end--;
        if (end > 0 && s.charAt(end - 1) == '.')
            end--;
        return s.substring(0, end);
    }
}
