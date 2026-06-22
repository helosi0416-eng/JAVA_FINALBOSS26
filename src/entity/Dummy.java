package entity;

import java.awt.*;
import java.awt.geom.AffineTransform;
import core.GamePanel;
import vfx.DamageText;

public class Dummy extends Entity {

    public enum DummyType { IDLE, ROAM, MELEE, RANGED }
    public DummyType type;
    public Player target;

    public double vx, vy;
    public int aiTimer = 0;
    public boolean isMoving = false;
    public double facingAngle = Math.PI / 2; 

    public int blinkTimer = 0;
    public double kx = 0, ky = 0; 
    public int stunTimer = 0;

    public int attackCD = 0;
    public int attackAnim = 0;
    public int windupTimer = 0; 

    // ★ 新增右手座標變數，讓它由 update 控制，避免時停時亂抖
    public int rightHandX = -3;

    public Dummy(double x, double y, DummyType type, Player target) {
        super(x, y, 20, 300); 
        this.type = type;
        this.target = target;
    }

    @Override
    public void update() {
        x += kx;
        y += ky;
        kx *= 0.85; 
        ky *= 0.85;

        if (attackCD > 0) attackCD--;
        if (attackAnim > 0) attackAnim--;

        if (type != DummyType.ROAM) {
            facingAngle = Math.PI / 2; 
        }

        if (stunTimer > 0) {
            stunTimer--;
            isMoving = false; 
            vx = 0; vy = 0;
            windupTimer = 0; 
        } else {
            if (type == DummyType.ROAM) {
                aiTimer--;
                if (aiTimer <= 0) {
                    if (Math.random() < 0.6) { 
                        isMoving = true;
                        double moveAngle = Math.random() * Math.PI * 2;
                        double moveSpeed = 0.8 + Math.random() * 1.2; 
                        vx = Math.cos(moveAngle) * moveSpeed;
                        vy = Math.sin(moveAngle) * moveSpeed;
                        aiTimer = 50 + (int)(Math.random() * 70); 
                    } else { 
                        isMoving = false; vx = 0; vy = 0;
                        aiTimer = 30 + (int)(Math.random() * 50); 
                    }
                }
                if (isMoving) { 
                    x += vx; y += vy; 
                    facingAngle = Math.atan2(vy, vx); 
                }
            
            } else if (type == DummyType.MELEE) {
                if (windupTimer > 0) {
                    windupTimer--;
                    if (windupTimer <= 0) { 
                        attackAnim = 15; 
                        attackCD = 60; 
                        
                        double dist = Math.hypot(target.x - x, target.y - y);
                        double angleToTarget = Math.atan2(target.y - y, target.x - x);
                        double angleDiff = Math.abs(angleToTarget - facingAngle);
                        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                        angleDiff = Math.abs(angleDiff);

                        if (!target.isDead && dist <= 60 && angleDiff <= Math.PI / 4) {
                            target.hp -= 10;
                            GamePanel.dmgTexts.add(new DamageText(target.x, target.y - 20, 10, DamageText.TextType.TAKEN));
                        }
                    }
                } else if (attackCD <= 0) {
                    windupTimer = 180; 
                }
            
            } else if (type == DummyType.RANGED) {
                if (windupTimer > 0) {
                    windupTimer--;
                    if (windupTimer <= 0) {
                        attackAnim = 15; 
                        attackCD = 80; 
                        GamePanel.projectiles.add(new Projectile(x, y, x, y + 100));
                    }
                } else if (attackCD <= 0) {
                    windupTimer = 180; 
                }
            }
        }

        // ★ 把右手座標的計算搬進來 update，只要時停，這裡就不會執行，手就會完美凍結！
        if (windupTimer > 0) {
            rightHandX = -18 + (int)(Math.random() * 8 - 4); 
        } else if (attackAnim > 0) {
            rightHandX = 20; 
        } else {
            rightHandX = -3;
        }

        x = Math.max(radius + 15, Math.min(900 - radius - 15, x));
        y = Math.max(radius + 15, Math.min(900 - radius - 15, y));
        if (blinkTimer > 0) blinkTimer--;
    }

    @Override
    public void draw(Graphics2D g2d) {
        AffineTransform oldAT = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(facingAngle); 

        int handRadius = 6;
        g2d.setColor(Color.RED);

        // ★ 直接使用算好的 rightHandX
        g2d.fillOval(-3, -radius - handRadius, handRadius * 2, handRadius * 2); 
        g2d.fillOval(rightHandX, radius - handRadius, handRadius * 2, handRadius * 2);  

        g2d.fillOval(-radius, -radius, radius * 2, radius * 2);

        if (stunTimer > 0 || blinkTimer > 0) {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(7, -8, 13, -2); g2d.drawLine(13, -8, 7, -2);
            g2d.drawLine(7, 2, 13, 8); g2d.drawLine(13, 2, 7, 8);
            g2d.setStroke(new BasicStroke(1));
        } else {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(6, -9, 8, 8); g2d.fillOval(6, 1, 8, 8);  
            g2d.setColor(Color.BLACK);
            g2d.fillOval(10, -7, 4, 4); g2d.fillOval(10, 3, 4, 4);  
        }

        g2d.setTransform(oldAT);
        
        g2d.setColor(Color.BLACK);
        String title = "";
        if (type == DummyType.IDLE) title = "IDLE";
        else if (type == DummyType.ROAM) title = "ROAM";
        else if (type == DummyType.MELEE) title = "MELEE";
        else if (type == DummyType.RANGED) title = "RANGED";
        
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (int)x - fm.stringWidth(title)/2, (int)y - radius - 8);
        g2d.drawString("HP: " + hp, (int)x - 20, (int)y + radius + 15);
    }
}