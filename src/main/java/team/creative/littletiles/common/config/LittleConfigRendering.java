package team.creative.littletiles.common.config;

import team.creative.creativecore.Side;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.api.ICreativeConfig;
import team.creative.littletiles.client.render.cache.build.RenderingThread;

public class LittleConfigRendering implements ICreativeConfig {
    
    @CreativeConfig
    public boolean useQuadCache = false;
    
    @CreativeConfig
    public boolean useCubeCache = true;
    
    @CreativeConfig
    @CreativeConfig.IntRange(slider = false, min = 1, max = 1024)
    public int renderingThreadCount = 2;
    
    @CreativeConfig
    public boolean highlightStructureBox = true;
    
    @CreativeConfig(hideFromGUI = true)
    public boolean previewLines = false;
    
    @CreativeConfig(hideFromGUI = true)
    public double previewLineThickness = 2;
    
    public boolean darkerPreviewBoxShading = false;

    @CreativeConfig
    public ToolPreview toolPreview = new ToolPreview();
    
    @CreativeConfig
    public boolean enableRandomDisplayTick = false;
    
    @CreativeConfig
    public boolean uploadToVBODirectly = true;
    
    @CreativeConfig
    public boolean showTooltip = true;
    
    @CreativeConfig
    public int itemCacheDuration = 5000;
    
    @CreativeConfig
    public int itemLowResolutionBoxCount = 1000;

    @CreativeConfig
    public boolean showToolPreviewInHand = false;
    
    @CreativeConfig
    public int entityCacheBuildThreads = 1;
    
    @CreativeConfig
    public int connectedShapeBlocksLimit = 128;
    
    @CreativeConfig
    public int wrenchInfoRange = 5;
    
    @CreativeConfig
    public double wrenchInfoRefresh = 1;
    
    @Override
    public void configured(Side side) {
        if (side.isClient())
            RenderingThread.initThreads(renderingThreadCount);
    }

    public static class ToolPreview {

        @CreativeConfig
        public boolean renderOnTop = true;

        @CreativeConfig
        public Appearance defaultTool = new Appearance();

        @CreativeConfig
        public Appearance placer = new Appearance();

        @CreativeConfig
        public Appearance blueprint = new Appearance();

        @CreativeConfig
        public Appearance chisel = new Appearance();

        @CreativeConfig
        public Appearance glove = new Appearance();

        @CreativeConfig
        public Appearance selection = new Appearance();

        @CreativeConfig
        public Appearance shaper = new Appearance();

        @CreativeConfig
        public Appearance wrench = new Appearance(true, false);

        @CreativeConfig
        public Appearance measure = new Appearance(false, true);
    }

    public static class Appearance {

        @CreativeConfig
        public boolean filled = true;

        @CreativeConfig
        public boolean lines = true;

        @CreativeConfig
        public boolean throughBlocks = true;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int filledRed = 255;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int filledGreen = 255;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int filledBlue = 255;

        @CreativeConfig
        @CreativeConfig.DecimalRange(slider = true, min = 0, max = 1)
        public double filledAlpha = 0.5;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int lineRed = 0;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int lineGreen = 0;

        @CreativeConfig
        @CreativeConfig.IntRange(slider = true, min = 0, max = 255)
        public int lineBlue = 0;

        @CreativeConfig
        @CreativeConfig.DecimalRange(slider = true, min = 0, max = 1)
        public double lineAlpha = 0.4;

        @CreativeConfig
        @CreativeConfig.DecimalRange(slider = false, min = 0.25, max = 16)
        public double lineWidth = 2;

        public Appearance() {}

        public Appearance(boolean filled, boolean lines) {
            this.filled = filled;
            this.lines = lines;
        }
    }
    
}
