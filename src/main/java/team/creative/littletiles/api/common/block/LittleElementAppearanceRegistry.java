package team.creative.littletiles.api.common.block;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class LittleElementAppearanceRegistry {
    
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    
    public static void register(Provider provider) {
        if (provider != null)
            PROVIDERS.add(provider);
    }
    
    public static CompoundTag getAppearance(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return null;
        for (Provider provider : PROVIDERS) {
            CompoundTag appearance = normalize(provider.getAppearance(stack));
            if (appearance != null)
                return appearance;
        }
        return null;
    }
    
    public static CompoundTag getAppearance(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null)
            return null;
        for (Provider provider : PROVIDERS) {
            CompoundTag appearance = normalize(provider.getAppearance(level, pos, state));
            if (appearance != null)
                return appearance;
        }
        return null;
    }
    
    private static CompoundTag normalize(CompoundTag appearance) {
        return appearance != null && !appearance.isEmpty() ? appearance.copy() : null;
    }
    
    public interface Provider {
        
        public default CompoundTag getAppearance(ItemStack stack) {
            return null;
        }
        
        public default CompoundTag getAppearance(LevelAccessor level, BlockPos pos, BlockState state) {
            return null;
        }
    }
}
