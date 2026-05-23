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

    public static String formatBlockTile(double blocks, LittleGrid grid) {
        if (grid == null)
            grid = LittleGrid.MIN;
        long total = Math.round(blocks * grid.count);
        if (total <= 0)
            return "0";
        long b = total / grid.count;
        long t = total % grid.count;
        if (t == 0)
            return b + "B";
        if (b == 0)
            return t + "/" + grid.count + "t";
        return b + "B " + t + "/" + grid.count + "t";
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
