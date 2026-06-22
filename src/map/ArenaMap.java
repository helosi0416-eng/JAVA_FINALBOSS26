package map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class ArenaMap extends GameMap {

    public ArenaMap() {
        // 設定這個競技場的尺寸為 900x900
        super(900, 900); 
    }

    @Override
    public void draw(Graphics2D g2d) {
        // 1. 填滿全地圖的淺藍色地板
        g2d.setColor(new Color(200, 230, 255)); 
        g2d.fillRect(0, 0, width, height);

        // 2. 繪製邊界結界 (青藍色發光線條)
        g2d.setColor(new Color(0, 200, 255, 100));
        g2d.setStroke(new BasicStroke(15));
        g2d.drawRect(0, 0, width, height);
        
        g2d.setColor(new Color(0, 255, 255, 200)); 
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(7, 7, width - 14, height - 14);
        
        // 恢復預設筆刷粗細
        g2d.setStroke(new BasicStroke(1)); 
    }
}