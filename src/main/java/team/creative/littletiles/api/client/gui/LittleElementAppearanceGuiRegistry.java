package team.creative.littletiles.api.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LittleElementAppearanceGuiRegistry {
    
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    
    public static void register(Provider provider) {
        if (provider != null)
            PROVIDERS.add(provider);
    }
    
    public static List<Control> getControls(Player player, ItemStack material, CompoundTag appearance, GuiPlacement placement) {
        if (material == null || material.isEmpty())
            return List.of();
        CompoundTag safeAppearance = appearance != null ? appearance.copy() : new CompoundTag();
        List<Control> controls = new ArrayList<>();
        for (Provider provider : PROVIDERS) {
            List<Control> provided = provider.getControls(player, material, safeAppearance.copy(), placement);
            if (provided != null)
                controls.addAll(provided);
        }
        return controls;
    }
    
    public enum GuiPlacement {
        PRIMARY,
        SECONDARY
    }
    
    public interface Provider {
        
        public default List<Control> getControls(Player player, ItemStack material, CompoundTag appearance, GuiPlacement placement) {
            return List.of();
        }
    }
    
    public static class Control {
        
        public final String key;
        public final Component title;
        public final List<Option> options;
        public final String selected;
        
        public Control(String key, Component title, List<Option> options, String selected) {
            this.key = key;
            this.title = title;
            this.options = options != null ? List.copyOf(options) : List.of();
            this.selected = selected != null ? selected : "";
        }
    }
    
    public static class Option {
        
        public final String value;
        public final Component title;
        public final ItemStack icon;
        
        public Option(String value, Component title, ItemStack icon) {
            this.value = value != null ? value : "";
            this.title = title != null ? title : Component.empty();
            this.icon = icon != null ? icon.copy() : ItemStack.EMPTY;
        }
    }
}
