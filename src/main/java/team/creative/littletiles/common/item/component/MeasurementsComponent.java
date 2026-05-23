package team.creative.littletiles.common.item.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.common.math.measure.LittleMeasurement;

public class MeasurementsComponent {

    public static final Codec<MeasurementsComponent> CODEC = CompoundTag.CODEC.xmap(MeasurementsComponent::new, MeasurementsComponent::save);
    public static final StreamCodec<FriendlyByteBuf, MeasurementsComponent> STREAM_CODEC = StreamCodec.of((buffer, s) -> {
        buffer.writeNbt(s.save());
    }, (buffer) -> new MeasurementsComponent(buffer.readNbt()));

    public static MeasurementsComponent of(List<LittleMeasurement> measurements) {
        return new MeasurementsComponent(measurements, null, List.of(), 0);
    }

    public static MeasurementsComponent of(List<LittleMeasurement> measurements, String unit) {
        return new MeasurementsComponent(measurements, normalize(unit), List.of(), 0);
    }

    public static MeasurementsComponent of(List<LittleMeasurement> measurements, String unit, List<int[]> selected) {
        return new MeasurementsComponent(measurements, normalize(unit), selected, 0);
    }

    public static MeasurementsComponent of(List<LittleMeasurement> measurements, String unit, List<int[]> selected, int defaultColor) {
        return new MeasurementsComponent(measurements, normalize(unit), selected, defaultColor);
    }

    public static MeasurementsComponent withMeasurements(ItemStack stack, List<LittleMeasurement> measurements) {
        var component = stack.get(LittleTilesRegistry.MEASUREMENTS);
        if (component == null)
            return of(measurements);
        return of(measurements, component.unit(), component.selected(), component.defaultColor());
    }

    public static List<LittleMeasurement> get(ItemStack stack) {
        var com = stack.get(LittleTilesRegistry.MEASUREMENTS);
        if (com != null)
            return com.value();
        return Collections.emptyList();
    }

    public static String getUnit(ItemStack stack) {
        var com = stack.get(LittleTilesRegistry.MEASUREMENTS);
        return com != null ? com.unit() : null;
    }

    public static int getDefaultColor(ItemStack stack) {
        var com = stack.get(LittleTilesRegistry.MEASUREMENTS);
        return com != null ? com.defaultColor() : 0;
    }

    private static String normalize(String value) {
        return value != null && !value.isEmpty() ? value : null;
    }

    private final List<LittleMeasurement> measurements;
    private final String unit;
    private final List<int[]> selected;
    private final int defaultColor;

    private MeasurementsComponent(CompoundTag nbt) {
        ListTag list = nbt.getList("c", Tag.TAG_COMPOUND);
        this.measurements = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LittleMeasurement measurement = LittleMeasurement.load(list.getCompound(i));
            if (measurement != null)
                this.measurements.add(measurement);
        }
        this.unit = nbt.contains("u", Tag.TAG_STRING) ? normalize(nbt.getString("u")) : null;
        this.defaultColor = nbt.getInt("dc");
        ListTag selectedList = nbt.getList("s", Tag.TAG_INT_ARRAY);
        this.selected = new ArrayList<>();
        for (int i = 0; i < selectedList.size(); i++) {
            int[] array = selectedList.getIntArray(i);
            if (array.length >= 3)
                this.selected.add(Arrays.copyOf(array, array.length));
        }
    }

    private MeasurementsComponent(List<LittleMeasurement> measurements, String unit, List<int[]> selected, int defaultColor) {
        this.measurements = measurements;
        this.unit = unit;
        this.selected = copySelected(selected);
        this.defaultColor = defaultColor;
    }

    public List<LittleMeasurement> value() {
        return new ArrayList<>(measurements);
    }

    public String unit() {
        return unit;
    }

    public List<int[]> selected() {
        return copySelected(selected);
    }

    public int defaultColor() {
        return defaultColor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeasurementsComponent s)
            return java.util.Objects.equals(s.measurements, measurements) && java.util.Objects.equals(s.unit, unit) && selectedEquals(s.selected, selected) && s.defaultColor == defaultColor;
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(measurements, unit, selectedHash(selected), defaultColor);
    }

    public CompoundTag save() {
        ListTag list = new ListTag();
        for (int i = 0; i < measurements.size(); i++)
            list.add(measurements.get(i).save());
        CompoundTag nbt = new CompoundTag();
        nbt.put("c", list);
        if (unit != null)
            nbt.putString("u", unit);
        if (defaultColor != 0)
            nbt.putInt("dc", defaultColor);
        if (!selected.isEmpty()) {
            ListTag selectedList = new ListTag();
            for (int[] array : selected)
                selectedList.add(new IntArrayTag(array));
            nbt.put("s", selectedList);
        }
        return nbt;
    }

    private static List<int[]> copySelected(List<int[]> selected) {
        if (selected == null || selected.isEmpty())
            return List.of();
        List<int[]> copy = new ArrayList<>(selected.size());
        for (int[] array : selected)
            if (array != null)
                copy.add(Arrays.copyOf(array, array.length));
        return copy;
    }

    private static boolean selectedEquals(List<int[]> first, List<int[]> second) {
        if (first.size() != second.size())
            return false;
        for (int i = 0; i < first.size(); i++)
            if (!Arrays.equals(first.get(i), second.get(i)))
                return false;
        return true;
    }

    private static int selectedHash(List<int[]> selected) {
        int result = 1;
        for (int[] array : selected)
            result = 31 * result + Arrays.hashCode(array);
        return result;
    }

}
