package vfx;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Particle {
    public double x, y, vx, vy, angle; 
    public int life;
    private Color color;

    // 預設為主角的螢光綠
    public Particle(double startX, double startY) {
        this(startX, startY, new Color(57, 255, 20)); 
    }

    // ★ 新增：可以自訂顏色的建構子
    public Particle(double startX, double startY, Color color) {
        this.x = startX + (Math.random() * 20 - 10);
        this.y = startY + (Math.random() * 20 - 10);
        this.angle = Math.random() * Math.PI * 2;
        double speed = Math.random() * 3 + 2; 
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.life = (int)(Math.random() * 15 + 10);
        this.color = color;
    }

    public void update() {
        this.x += this.vx;
        this.y += this.vy;
        this.life--;
    }

    public void draw(Graphics2D g2d) {
        float alpha = this.life / 20f; 
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        AffineTransform pat = g2d.getTransform();
        g2d.translate(this.x, this.y);
        g2d.rotate(this.angle); 
        
        g2d.setColor(this.color); // 使用自訂顏色
        g2d.fillRect(-4, -2, 8, 4); 
        
        g2d.setTransform(pat);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}