package team.creative.littletiles.common.gui.premade.photo;

public enum PhotoImportSource {
    
    FILE("gui.photo_importer.source.file"),
    URL("gui.photo_importer.source.url");
    
    public final String translationKey;
    
    private PhotoImportSource(String translationKey) {
        this.translationKey = translationKey;
    }
    
}
