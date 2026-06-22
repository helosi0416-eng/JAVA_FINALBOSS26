package map;

import java.awt.Graphics2D;

public abstract class GameMap {
    public int width;
    public int height;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // 規定所有繼承這個類別的地圖，都必須實作自己的畫圖方法
    public abstract void draw(Graphics2D g2d);
}