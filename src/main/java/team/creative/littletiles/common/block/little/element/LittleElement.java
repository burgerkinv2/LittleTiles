package team.creative.littletiles.common.block.little.element;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.common.network.type.NetworkFieldTypeClass;
import team.creative.creativecore.common.network.type.NetworkFieldTypes;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.block.LittleElementAppearanceRegistry;
import team.creative.littletiles.api.common.block.LittleBlock;
import team.creative.littletiles.common.block.little.registry.LittleBlockRegistry;

public class LittleElement {
    
    public static final Codec<LittleElement> CODEC = RecordCodecBuilder.create(x -> x.group(Codec.STRING.fieldOf("state").forGetter(e -> e.getBlockName()), Codec.INT.fieldOf(
        "color").forGetter(e -> e.color), CompoundTag.CODEC.optionalFieldOf("appearance").forGetter(e -> Optional.ofNullable(e.appearance()))).apply(x,
            (state, color, appearance) -> new LittleElement(state, color, appearance.orElse(null))));
    public static final StreamCodec<RegistryFriendlyByteBuf, LittleElement> STREAM_CODEC = NetworkFieldTypes.registerAndCodec(new NetworkFieldTypeClass<LittleElement>() {
        
        @Override
        protected void writeContent(LittleElement content, RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(content.getBlockName());
            buffer.writeInt(content.color);
            buffer.writeBoolean(content.hasAppearance());
            if (content.hasAppearance())
                buffer.writeNbt(content.appearance);
        }
        
        @Override
        protected LittleElement readContent(RegistryFriendlyByteBuf buffer) {
            String blockName = buffer.readUtf();
            int color = buffer.readInt();
            CompoundTag appearance = buffer.readBoolean() ? (CompoundTag) buffer.readNbt(NbtAccounter.unlimitedHeap()) : null;
            return new LittleElement(blockName, color, appearance);
        }
    }, LittleElement.class);
    
    public static LittleElement getOrDefault(ItemStack stack) {
        var element = stack.get(LittleTilesRegistry.ELEMENT);
        if (element != null)
            return element;
        return new LittleElement(Blocks.STONE.defaultBlockState(), ColorUtils.WHITE);
    }
    
    public static LittleElement of(ItemStack stack, int color) throws NotBlockException {
        Block block = Block.byItem(stack.getItem());
        if (block != null && !(block instanceof AirBlock))
            return new LittleElement(block.defaultBlockState(), color, LittleElementAppearanceRegistry.getAppearance(stack));
        throw new NotBlockException();
    }
    
    private BlockState state;
    protected LittleBlock block;
    public int color;
    private CompoundTag appearance;
    
    public LittleElement(LittleElement element) {
        this.state = element.state;
        this.block = element.block;
        this.color = element.color;
        setAppearance(element.appearance);
    }
    
    public LittleElement(LittleElement element, int color) {
        this.state = element.state;
        this.block = element.block;
        this.color = color;
        setAppearance(element.appearance);
    }
    
    public LittleElement(BlockState state, int color) {
        this(state, color, null);
    }

    public LittleElement(BlockState state, int color, CompoundTag appearance) {
        setState(state);
        this.color = color;
        setAppearance(appearance);
    }
    
    @Deprecated
    public LittleElement(BlockState state, LittleBlock block, int color) {
        this(state, block, color, null);
    }

    @Deprecated
    public LittleElement(BlockState state, LittleBlock block, int color, CompoundTag appearance) {
        this.state = state;
        this.block = block;
        this.color = color;
        setAppearance(appearance);
    }
    
    public LittleElement(CompoundTag nbt) {
        this(nbt.getString("s"), nbt.contains("c") ? nbt.getInt("c") : ColorUtils.WHITE, nbt.contains("a") ? nbt.getCompound("a") : null);
    }
    
    public LittleElement(String name, int color) {
        this(name, color, null);
    }

    public LittleElement(String name, int color, CompoundTag appearance) {
        setState(LittleBlockRegistry.loadState(name));
        if (state.getBlock() == LittleTilesRegistry.MISSING.value())
            this.block = LittleBlockRegistry.getMissing(name);
        this.color = color;
        setAppearance(appearance);
    }
    
    public BlockState getState() {
        return state;
    }
    
    public void setState(BlockState state) {
        if (this.state != state) {
            this.state = state;
            this.block = LittleBlockRegistry.get(state.getBlock());
        }
    }
    
    public LittleBlock getBlock() {
        return block;
    }
    
    public boolean hasColor() {
        return color != ColorUtils.WHITE;
    }
    
    public boolean hasAppearance() {
        return appearance != null && !appearance.isEmpty();
    }

    public CompoundTag appearance() {
        return hasAppearance() ? appearance.copy() : null;
    }

    public void setAppearance(CompoundTag appearance) {
        this.appearance = appearance != null && !appearance.isEmpty() ? appearance.copy() : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, color, appearance);
    }
    
    public String getBlockName() {
        String name = LittleBlockRegistry.saveState(getState());
        if (name == null)
            return getBlock().blockName();
        return name;
    }
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("s", getBlockName());
        nbt.putInt("c", color);
        if (hasAppearance())
            nbt.put("a", appearance.copy());
        return nbt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LittleElement element)
            return element.state == state && element.color == color && Objects.equals(element.appearance, appearance);
        return super.equals(obj);
    }
    
    public boolean is(LittleElement element) {
        return element.state == state && element.block == block && element.color == color && Objects.equals(element.appearance, appearance);
    }
    
    @Override
    public String toString() {
        return "[" + getBlockName() + "|" + color + (hasAppearance() ? "|" + appearance : "") + "]";
    }
    
    public boolean checkEntityCollision() {
        return block.checkEntityCollision();
    }
    
    public static class NotBlockException extends Exception {}
    
}
