package nl.idsklijnsma.carlevel.ui.config;

public class LevelConfig {
    int offsetX;
    int offsetY;
    boolean invertX;
    boolean invertY;

    public LevelConfig(int offsetX, int offsetY, boolean invertX, boolean invertY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.invertX = invertX;
        this.invertY = invertY;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public boolean isInvertX() {
        return invertX;
    }

    public void setInvertX(boolean invertX) {
        this.invertX = invertX;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
    }
}

