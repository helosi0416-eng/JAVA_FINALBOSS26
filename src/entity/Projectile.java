package entity;

import java.awt.*;

public class Projectile {
    public double x, y, vx, vy;
    public int life = 180; 
    public int damage = 10;
    public boolean active = true;

    public Projectile(double x, double y, double targetX, double targetY) {
        this.x = x; 
        this.y = y;
        double angle = Math.atan2(targetY - y, targetX - x);
        double speed = 6.0; // 石頭飛行速度
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
    }

    public void update() {
        x += vx; 
        y += vy; 
        life--;
        if (life <= 0) active = false;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(100, 100, 100)); // 深灰色石頭
        g2d.fillOval((int)x - 6, (int)y - 6, 12, 12);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval((int)x - 6, (int)y - 6, 12, 12);
    }
}