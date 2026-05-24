package team.creative.littletiles.api.client.render;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class LittleElementAppearanceModelRegistry {
    
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    
    public static void register(Provider provider) {
        if (provider != null)
            PROVIDERS.add(provider);
    }
    
    public static BakedModel getModel(BlockState state, BlockPos pos, CompoundTag appearance) {
        if (state == null || appearance == null || appearance.isEmpty())
            return null;
        for (Provider provider : PROVIDERS) {
            BakedModel model = provider.getModel(state, pos, appearance.copy());
            if (model != null)
                return model;
        }
        return null;
    }
    
    public interface Provider {
        
        public BakedModel getModel(BlockState state, BlockPos pos, CompoundTag appearance);
    }
}
