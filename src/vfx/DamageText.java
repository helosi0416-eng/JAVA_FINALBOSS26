package vfx;

import java.awt.*;

public class DamageText {
    // 內建定義五種文字類型屬性
    public enum TextType { DEALT, DEALT_CRIT, TAKEN, TAKEN_CRIT, HEAL }

    public double x, y;
    public int value;
    public int life = 50; 
    public TextType type;

    public DamageText(double x, double y, int value, TextType type) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.type = type;
    }

    public void update() {
        this.y -= 1.0; // 緩慢向上飄移
        this.life--;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (int)(Math.max(0, this.life * 5.1));
        if (alpha > 255) alpha = 255;
        
        Font font = new Font("Arial", Font.PLAIN, 18);
        Color color = Color.WHITE;
        String text = String.valueOf(this.value);

        // 根據類型，100% 還原你要求的顏色與粗體外觀
        switch (this.type) {
            case DEALT:
                font = new Font("Arial", Font.PLAIN, 18);
                color = new Color(255, 200, 0, alpha); // 造成傷害：黃色
                break;
            case DEALT_CRIT:
                font = new Font("Arial", Font.BOLD, 22);
                color = new Color(255, 200, 0, alpha); // 造成爆擊：粗體黃色
                break;
            case TAKEN:
                font = new Font("Arial", Font.PLAIN, 18);
                color = new Color(255, 0, 0, alpha); // 受到傷害：紅色
                break;
            case TAKEN_CRIT:
                font = new Font("Arial", Font.BOLD, 22);
                color = new Color(255, 0, 0, alpha); // 受到爆擊：粗體紅色
                break;
            case HEAL:
                font = new Font("Arial", Font.BOLD, 18);
                color = new Color(0, 128, 0, alpha); // 回復血量：深綠色
                text = "+" + this.value; // 自動加上正號
                break;
        }
        
        g2d.setFont(font);
        g2d.setColor(color);
        g2d.drawString(text, (int)this.x, (int)this.y);
    }
}