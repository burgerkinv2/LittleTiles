package team.creative.littletiles.common.item.component;

import com.mojang.serialization.Codec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import team.creative.creativecore.common.util.text.TextMapBuilder;

public enum LittleSawMode {

    SINGLE("tile"),
    SAME_TYPE("same_type"),
    CONNECTED("connected");

    public static final Codec<LittleSawMode> CODEC = Codec.stringResolver(x -> x.id, LittleSawMode::get);
    public static final StreamCodec<FriendlyByteBuf, LittleSawMode> STREAM_CODEC = StreamCodec.of((buffer, x) -> buffer.writeUtf(x.id),
        buffer -> LittleSawMode.get(buffer.readUtf()));
    public static final TextMapBuilder<LittleSawMode> MAP = new TextMapBuilder<LittleSawMode>().addComponent(values(), LittleSawMode::translatable);

    public final String id;

    private LittleSawMode(String id) {
        this.id = id;
    }

    public Component translatable() {
        return Component.translatable("shape.tiles." + id);
    }

    public static LittleSawMode get(String id) {
        for (LittleSawMode mode : values())
            if (mode.id.equals(id))
                return mode;
        return SINGLE;
    }

}
