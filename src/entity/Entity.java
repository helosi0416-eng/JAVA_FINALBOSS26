package entity;

import java.awt.Graphics2D;

public abstract class Entity {
    public double x, y;
    public int radius; // 碰撞半徑
    public int hp, maxHp;

    public Entity(double x, double y, int radius, int maxHp) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    // 規定所有繼承 Entity 的生物，都必須實作自己的更新與繪製邏輯
    public abstract void update();
    public abstract void draw(Graphics2D g2d);
}