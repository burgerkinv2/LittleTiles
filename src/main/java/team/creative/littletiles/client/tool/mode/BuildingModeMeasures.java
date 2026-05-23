package team.creative.littletiles.client.tool.mode;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.settings.KeyModifier;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.premade.KeyConfig;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleMeasure;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.client.render.overlay.OverlayRenderer;
import team.creative.littletiles.client.render.overlay.OverlayRenderer.OverlayGuiLayer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.tool.mode.BuildingModeTopBar.BuildingModeInfo;
import team.creative.littletiles.common.item.component.MeasurementsComponent;
import team.creative.littletiles.common.math.measure.LittleMeasurement;

public class BuildingModeMeasures extends BuildingModeFeature implements BuildingModeInfo {

    @CreativeConfig
    public KeyConfig key = new KeyConfig(InputConstants.KEY_M, KeyModifier.NONE);

    @CreativeConfig
    public Mode defaultMode = Mode.OFF;

    private BoxRenderResult result;
    private List<MeasurementSet> tapes = new ArrayList<>();
    private int inventoryChanged = -1;
    private List<MeasurementsComponent> tapeSnapshot = List.of();
    private Mode mode = defaultMode;
    private boolean active;
    private GuiLabel label;

    private List<LittleMeasurement> toolMeasurements;

    public void updateMeasureTapes(PreviewRenderer renderer) {
        inventoryChanged = renderer.player().getInventory().getTimesChanged();
        tapeSnapshot = collectTapeSnapshot(renderer);
        removeCache();
        tapes.clear();
        List<LittleMeasurement> allMeasurements = new ArrayList<>();
        for (ItemStack stack : renderer.player().getInventory().items) {
            if (!(stack.getItem() instanceof ILittleMeasure m))
                continue;

            List<LittleMeasurement> measurements = m.getMeasurements(stack);
            if (measurements.isEmpty())
                continue;

            tapes.add(new MeasurementSet(measurements, MeasurementsComponent.getUnit(stack)));
            allMeasurements.addAll(measurements);
        }
        result = buildTapes(renderer, allMeasurements);
    }

    private void removeCache() {
        if (result != null) {
            result.close();
            result = null;
        }
    }

    @Override
    public void tick(PreviewRenderer renderer) {
        updateIfChanged(renderer);
    }

    @Override
    public boolean render(PreviewRenderer renderer, PoseStack pose, Vec3 cam, boolean lines) {
        if (!lines || !isVisibleInBuildingMode())
            return false;

        renderTapes(renderer, cam, lines);

        return false;
    }

    @Override
    public void renderGui(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam) {
        if (isVisibleInBuildingMode())
            renderTapeLabels(renderer, overlay, cam);
    }

    public void renderGlobal(PreviewRenderer renderer, PoseStack pose, OverlayRenderer overlay, Vec3 cam) {
        if (mode != Mode.GLOBAL)
            return;

        renderTapes(renderer, cam, true);
        renderTapeLabels(renderer, overlay, cam);
    }

    private boolean isVisibleInBuildingMode() {
        return mode == Mode.DISPLAY || mode == Mode.GLOBAL;
    }

    private void updateIfChanged(PreviewRenderer renderer) {
        if (renderer.player().getInventory().getTimesChanged() != inventoryChanged || !tapeSnapshot.equals(collectTapeSnapshot(renderer)))
            updateMeasureTapes(renderer);
    }

    private void renderTapes(PreviewRenderer renderer, Vec3 cam, boolean lines) {
        updateIfChanged(renderer);

        renderer.setupPreviewRenderer(true);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (result != null)
            renderer.renderSeethroughLines(cam, lines, result.pos(), result.data(), -1);

        toolMeasurements = renderer.manager.tool() != null ? renderer.manager.tool().measurements() : null;
        if (toolMeasurements != null) {
            BoxRenderResult toolResult = buildTapes(renderer, toolMeasurements);
            if (toolResult != null) {
                renderer.renderSeethroughLines(cam, lines, toolResult.pos(), toolResult.data(), -1);
                toolResult.close();
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableCull();
        RenderSystem.applyModelViewMatrix();
    }

    private void renderTapeLabels(PreviewRenderer renderer, OverlayRenderer overlay, Vec3 cam) {
        updateIfChanged(renderer);

        for (MeasurementSet set : tapes)
            for (LittleMeasurement measurement : set.measurements)
                measurement.overlay(renderer, overlay, cam, set.unit);
        if (toolMeasurements != null)
            for (LittleMeasurement measurement : toolMeasurements)
                measurement.overlay(renderer, overlay, cam);
    }

    private BoxRenderResult buildTapes(PreviewRenderer renderer, List<LittleMeasurement> measurements) {
        if (measurements.isEmpty())
            return null;

        ByteBufferBuilder buffer = renderer.createBuffer();
        var builder = renderer.createBuilder(buffer, true);

        BlockPos pos = renderer.player().blockPosition();

        PoseStack pose = new PoseStack();
        pose.translate(-pos.getX(), -pos.getY(), -pos.getZ());

        for (LittleMeasurement measurement : measurements)
            measurement.build(renderer, pose, builder);

        var mesh = builder.build();
        if (mesh instanceof MeshDataExtender m)
            m.keepAlive(true);
        return new BoxRenderResult(null, pos, buffer, mesh);
    }

    private List<MeasurementsComponent> collectTapeSnapshot(PreviewRenderer renderer) {
        List<MeasurementsComponent> snapshot = new ArrayList<>();
        for (ItemStack stack : renderer.player().getInventory().items)
            if (stack.getItem() instanceof ILittleMeasure)
                snapshot.add(stack.get(LittleTilesRegistry.MEASUREMENTS));
        return snapshot;
    }

    @Override
    public void unloadLevel() {
        tapes.clear();
        tapeSnapshot = List.of();
        removeCache();
    }

    @Override
    public void create(OverlayGuiLayer gui, LittleTool tool, List<BuildingModeFeature> allFeatures) {
        active = true;
        updateTitle();
    }

    @Override
    public void remove(OverlayGuiLayer gui) {
        active = false;
    }

    @Override
    public void createInfo(GuiParent parent) {
        parent.add(label = new GuiLabel("measurements"));
        updateTitle();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int action, int modifiers) {
        if (active && key.matchesPress(keyCode, action)) {
            setMode(mode.next());
            return true;
        }
        return false;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : defaultMode;
        updateTitle();
    }

    private void updateTitle() {
        if (label != null)
            label.setTitle(Component.translatable("building.measurements.info", Component.translatable(mode.key()), key.getTranslatedKeyMessage()));
    }

    public enum Mode {
        OFF("building.measurements.off"),
        DISPLAY("building.measurements.display"),
        GLOBAL("building.measurements.global");

        private final String key;

        private Mode(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public Component translatable() {
            return Component.translatable(key);
        }

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static record MeasurementSet(List<LittleMeasurement> measurements, String unit) {}
}
