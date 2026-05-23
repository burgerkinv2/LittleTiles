package team.creative.littletiles.common.item.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
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
        return new MeasurementsComponent(measurements, null);
    }

    public static MeasurementsComponent of(List<LittleMeasurement> measurements, String unit) {
        return new MeasurementsComponent(measurements, normalize(unit));
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

    private static String normalize(String value) {
        return value != null && !value.isEmpty() ? value : null;
    }

    private final List<LittleMeasurement> measurements;
    private final String unit;

    private MeasurementsComponent(CompoundTag nbt) {
        ListTag list = nbt.getList("c", Tag.TAG_COMPOUND);
        this.measurements = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LittleMeasurement measurement = LittleMeasurement.load(list.getCompound(i));
            if (measurement != null)
                this.measurements.add(measurement);
        }
        this.unit = nbt.contains("u", Tag.TAG_STRING) ? normalize(nbt.getString("u")) : null;
    }

    private MeasurementsComponent(List<LittleMeasurement> measurements, String unit) {
        this.measurements = measurements;
        this.unit = unit;
    }

    public List<LittleMeasurement> value() {
        return new ArrayList<>(measurements);
    }

    public String unit() {
        return unit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeasurementsComponent s)
            return java.util.Objects.equals(s.measurements, measurements) && java.util.Objects.equals(s.unit, unit);
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(measurements, unit);
    }

    public CompoundTag save() {
        ListTag list = new ListTag();
        for (int i = 0; i < measurements.size(); i++)
            list.add(measurements.get(i).save());
        CompoundTag nbt = new CompoundTag();
        nbt.put("c", list);
        if (unit != null)
            nbt.putString("u", unit);
        return nbt;
    }

}
