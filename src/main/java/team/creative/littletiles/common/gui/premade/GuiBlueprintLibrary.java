package team.creative.littletiles.common.gui.premade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiLayer;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.parent.GuiLeftRightBox;
import team.creative.creativecore.common.gui.control.parent.GuiLabeledControl;
import team.creative.creativecore.common.gui.control.simple.GuiButton;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.creativecore.common.gui.control.simple.GuiTextfield;
import team.creative.creativecore.common.gui.control.tree.GuiTree;
import team.creative.creativecore.common.gui.control.tree.GuiTree.GuiTreeSelectionChanged;
import team.creative.creativecore.common.gui.control.tree.GuiTreeItem;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.sync.GuiSyncLocal;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.convertion.OldLittleTilesDataParser;
import team.creative.littletiles.common.convertion.OldLittleTilesDataParser.LittleConvertException;
import team.creative.littletiles.common.grid.LittleGridException;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public class GuiBlueprintLibrary extends GuiLayer {
    
    private static final String EXTENSION = ".ltb";
    private static final int MAX_VISIBLE_ENTRIES = 12;
    private GuiTree localTree;
    private GuiTextfield localName;
    private GuiTextfield serverName;
    private GuiLabel selectedLabel;
    private GuiLabel localStatus;
    private GuiLabel serverStatus;
    private String selectedLocalId;
    
    public final GuiSyncLocal<CompoundTag> IMPORT_LOCAL = getSyncHolder().register("import_local", nbt -> {
        if (!getPlayer().level().isClientSide)
            importToMainHand(getPlayer(), nbt);
    });
    
    public final GuiSyncLocal<CompoundTag> SERVER_ACTION = getSyncHolder().register("server_action", tag -> {
        if (!getPlayer().level().isClientSide && getPlayer() instanceof ServerPlayer player)
            handleServerAction(player, tag);
    });
    
    public GuiBlueprintLibrary() {
        super("blueprint_library", 310, 230);
        flow = GuiFlow.STACK_Y;
        align = Align.STRETCH;
    }
    
    @Override
    public void create() {
        add(new GuiLabel("title").setTranslate("gui.blueprint_library"));
        
        GuiParent content = new GuiParent(GuiFlow.STACK_X);
        add(content.setExpandable());
        
        GuiParent left = new GuiParent(GuiFlow.STACK_Y);
        left.align = Align.STRETCH;
        content.add(left.setDim(145, 185));
        left.add(new GuiLabel("local_title").setTranslate("gui.blueprint_library.local"));
        localTree = new GuiTree("local_tree", true).setRootVisibility(false);
        left.add(localTree.setExpandable());
        
        GuiParent right = new GuiParent(GuiFlow.STACK_Y);
        right.align = Align.STRETCH;
        content.add(right.setDim(145, 185));
        createActions(right);
        
        registerEventChanged(x -> {
            if (x instanceof GuiTreeSelectionChanged changed && changed.selected instanceof BlueprintTreeItem item && !item.folder)
                selectLocal(item.id);
        });
        
        refreshLocalList();
    }
    
    private void createActions(GuiParent parent) {
        parent.add(selectedLabel = (GuiLabel) new GuiLabel("selected").setTitle(Component.translatable("gui.blueprint_library.selected_none")));
        
        localName = new GuiTextfield("local_name");
        localName.setSuggestion("example/chair");
        parent.add(new GuiLabeledControl("gui.blueprint_library.entry", localName.setExpandableX()));
        
        GuiLeftRightBox localActions = new GuiLeftRightBox();
        parent.add(localActions.setExpandableX());
        localActions.addLeft(new GuiButton("refresh_local", x -> refreshLocalList()).setTranslate("gui.refresh"));
        localActions.addLeft(new GuiButton("export_local", x -> exportLocal()).setTranslate("gui.blueprint_library.export"));
        localActions.addRight(new GuiButton("import_local", x -> importLocal()).setTranslate("gui.blueprint_library.import"));
        parent.add(localStatus = (GuiLabel) new GuiLabel("local_status").setTitle(Component.empty()));
        
        parent.add(new GuiLabel("server_title").setTranslate("gui.blueprint_library.server"));
        serverName = new GuiTextfield("server_name");
        serverName.setSuggestion("example/chair");
        parent.add(new GuiLabeledControl("gui.blueprint_library.entry", serverName.setExpandableX()));
        
        GuiLeftRightBox actions = new GuiLeftRightBox();
        parent.add(actions.setExpandableX());
        actions.addLeft(new GuiButton("server_list", x -> sendServerAction("list")).setTranslate("gui.blueprint_library.list"));
        actions.addLeft(new GuiButton("server_upload", x -> sendServerAction("upload")).setTranslate("gui.blueprint_library.upload"));
        actions.addRight(new GuiButton("server_download", x -> sendServerAction("download")).setTranslate("gui.blueprint_library.download"));
        actions.addRight(new GuiButton("server_delete", x -> sendServerAction("delete")).setTranslate("gui.delete"));
        
        parent.add(serverStatus = (GuiLabel) new GuiLabel("server_status").setTitle(Component.translatable("gui.blueprint_library.server_hint")).setDim(140, 55));
    }
    
    private void exportLocal() {
        try {
            String id = localId();
            CompoundTag content = getBlueprintContent(getPlayer().getMainHandItem());
            if (content == null) {
                localStatus.setTitle(Component.translatable("gui.blueprint_library.no_blueprint"));
                return;
            }
            Path file = localFile(id);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
            localStatus.setTitle(Component.translatable("gui.blueprint_library.saved", id));
            refreshLocalList();
        } catch (Exception e) {
            localStatus.setTitle(Component.literal(e.getMessage()));
        }
    }
    
    private void importLocal() {
        try {
            String id = localId();
            CompoundTag nbt = readBlueprintFile(localFile(id));
            validateImport(getPlayer(), nbt);
            IMPORT_LOCAL.send(nbt);
            localStatus.setTitle(Component.translatable("gui.blueprint_library.imported", id));
        } catch (Exception e) {
            localStatus.setTitle(Component.literal(e.getMessage()));
        }
    }
    
    private void refreshLocalList() {
        try {
            Path root = localRoot();
            Files.createDirectories(root);
            List<String> entries = listBlueprints(root);
            localTree.root().clearItems();
            for (String entry : entries)
                addTreeEntry(entry);
            localTree.updateTree();
            if (entries.isEmpty())
                localStatus.setTitle(Component.translatable("gui.blueprint_library.empty"));
        } catch (IOException e) {
            localStatus.setTitle(Component.literal(e.getMessage()));
        }
    }
    
    private void sendServerAction(String action) {
        CompoundTag tag = new CompoundTag();
        tag.putString("action", action);
        tag.putString("id", serverName.getText());
        SERVER_ACTION.send(tag);
        serverStatus.setTitle(Component.translatable("gui.blueprint_library.server_sent"));
    }
    
    private void handleServerAction(ServerPlayer player, CompoundTag tag) {
        String action = tag.getString("action");
        try {
            if ("list".equals(action)) {
                sendServerList(player);
                return;
            }
            
            String id = sanitizeId(tag.getString("id"));
            Path file = serverFile(player.server, id);
            if ("upload".equals(action)) {
                CompoundTag content = getBlueprintContent(player.getMainHandItem());
                if (content == null) {
                    player.sendSystemMessage(Component.translatable("gui.blueprint_library.no_blueprint"));
                    return;
                }
                validateImport(player, content);
                Files.createDirectories(file.getParent());
                Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
                player.sendSystemMessage(Component.translatable("gui.blueprint_library.server_uploaded", id));
            } else if ("download".equals(action)) {
                CompoundTag content = readBlueprintFile(file);
                importToMainHand(player, content);
                player.sendSystemMessage(Component.translatable("gui.blueprint_library.server_downloaded", id));
            } else if ("delete".equals(action)) {
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.translatable("gui.blueprint_library.no_permission"));
                    return;
                }
                Files.deleteIfExists(file);
                player.sendSystemMessage(Component.translatable("gui.blueprint_library.server_deleted", id));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal(e.getMessage()));
        }
    }
    
    private void sendServerList(ServerPlayer player) throws IOException {
        Path root = serverRoot(player.server);
        Files.createDirectories(root);
        List<String> entries = listBlueprints(root);
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.blueprint_library.empty"));
            return;
        }
        player.sendSystemMessage(Component.translatable("gui.blueprint_library.server_entries"));
        for (int i = 0; i < Math.min(entries.size(), MAX_VISIBLE_ENTRIES); i++)
            player.sendSystemMessage(Component.literal(entries.get(i)));
    }
    
    private void importToMainHand(Player player, CompoundTag nbt) {
        try {
            validateImport(player, nbt);
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                if (!player.isCreative())
                    return;
                stack = new ItemStack(LittleTilesRegistry.BLUEPRINT.value());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            }
            if (!(stack.getItem() instanceof ItemLittleBlueprint)) {
                player.sendSystemMessage(Component.translatable("gui.blueprint_library.no_blueprint"));
                return;
            }
            CompoundTag stackTag = ILittleTool.getData(stack);
            stackTag.put(ItemLittleBlueprint.CONTENT_KEY, nbt.copy());
            ILittleTool.setData(stack, stackTag);
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal(e.getMessage()));
        }
    }
    
    private static void validateImport(Player player, CompoundTag nbt) throws LittleGridException, LittleConvertException {
        CompoundTag check = nbt.copy();
        if (OldLittleTilesDataParser.isOld(check))
            check = OldLittleTilesDataParser.convert(check);
        List<Component> errors = new ArrayList<>();
        if (!GuiImport.checkImport(errors, player, LittleGroup.load(check)))
            throw new IllegalArgumentException(errors.isEmpty() ? "Invalid blueprint" : errors.get(0).getString());
    }
    
    private static CompoundTag getBlueprintContent(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemLittleBlueprint blueprint) || !blueprint.hasTiles(stack))
            return null;
        return ItemLittleBlueprint.getContent(stack).copy();
    }
    
    private static CompoundTag readBlueprintFile(Path file) throws IOException, CommandSyntaxException {
        return TagParser.parseTag(Files.readString(file, StandardCharsets.UTF_8));
    }
    
    private String localId() {
        String text = localName.getText();
        if ((text == null || text.isBlank()) && selectedLocalId != null)
            return selectedLocalId;
        return sanitizeId(text);
    }
    
    private static String sanitizeId(String text) {
        String id = text == null ? "" : text.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (id.endsWith(EXTENSION))
            id = id.substring(0, id.length() - EXTENSION.length());
        if (id.isBlank())
            throw new IllegalArgumentException("Missing blueprint id");
        if (id.startsWith("/") || id.contains("..") || !id.matches("[a-z0-9_\\-/]+"))
            throw new IllegalArgumentException("Invalid blueprint id");
        return id;
    }
    
    private static List<String> listBlueprints(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).filter(x -> x.getFileName().toString().endsWith(EXTENSION)).map(root::relativize).map(x -> x.toString().replace('\\', '/'))
                    .map(x -> x.substring(0, x.length() - EXTENSION.length())).sorted(Comparator.naturalOrder()).toList();
        }
    }
    
    private static Path localRoot() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("littletiles").resolve("blueprints");
    }
    
    private static Path localFile(String id) {
        return localRoot().resolve(id + EXTENSION).normalize();
    }
    
    private static Path serverRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("littletiles").resolve("blueprints");
    }
    
    private static Path serverFile(MinecraftServer server, String id) {
        return serverRoot(server).resolve(id + EXTENSION).normalize();
    }
    
    private void addTreeEntry(String id) {
        String[] parts = id.split("/");
        GuiTreeItem parent = localTree.root();
        String path = "";
        for (int i = 0; i < parts.length; i++) {
            boolean folder = i + 1 < parts.length;
            path = path.isEmpty() ? parts[i] : path + "/" + parts[i];
            GuiTreeItem existing = findChild(parent, parts[i]);
            if (existing == null) {
                existing = new BlueprintTreeItem(parts[i], localTree, folder ? null : path, folder);
                parent.addItem(existing);
            }
            parent = existing;
        }
    }
    
    private static GuiTreeItem findChild(GuiTreeItem parent, String name) {
        for (GuiTreeItem item : parent.items())
            if (item instanceof BlueprintTreeItem blueprint && blueprint.name.equals(name))
                return item;
        return null;
    }
    
    private void selectLocal(String id) {
        selectedLocalId = id;
        localName.setText(id);
        serverName.setText(id);
        selectedLabel.setTitle(Component.translatable("gui.blueprint_library.selected", id));
    }
    
    private static class BlueprintTreeItem extends GuiTreeItem {
        
        public final String name;
        public final String id;
        public final boolean folder;
        
        public BlueprintTreeItem(String name, GuiTree tree, String id, boolean folder) {
            super(name, tree);
            this.name = name;
            this.id = id;
            this.folder = folder;
            setTitle(Component.literal(name));
        }
        
    }
    
}
