package entity;

import core.GamePanel;
import vfx.DamageText;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;

public class Equinox extends Entity {
    
    public Player target;
    public double facingAngle = 0;
    public double kx = 0, ky = 0;
    
    public enum State { 
        IDLE, CHASE, 
        M1_WINDUP, M1_SLASH, 
        M2_WINDUP, M2_DASH, 
        SHIFT_WINDUP, SHIFT_DASH, 
        Q_WINDUP, Q_LEAP, 
        E_WINDUP, E_SWEEP,
        ULT_LIGHT_WINDUP, ULT_LIGHT_LEAP,
        ULT_DARK_WINDUP, ULT_DARK_TORNADO
    }
    public State currentState = State.CHASE;
    public int stateTimer = 0;
    public int actionCD = 60; 
    
    public double illuminaAngle = 0, darkheartAngle = 0;
    public double illuminaExt = 0, darkheartExt = 0; 
    public double visualScale = 1.0; 

    public int m1Step = 0; 
    public boolean hasHitThisMove = false;

    public boolean hasTriggeredUlt66 = false;
    public boolean hasTriggeredUlt33 = false;

    public ArrayList<SwordKi> swordAuras = new ArrayList<>();
    private double dashVx = 0, dashVy = 0;

    public Equinox(double x, double y, Player target) {
        super(x, y, 32, 1500); 
        this.target = target;
    }

    private double shake() { return Math.random() * 4 - 2; }

    @Override
    public void update() {
        kx = 0; ky = 0; 

        Iterator<SwordKi> it = swordAuras.iterator();
        while (it.hasNext()) {
            SwordKi ki = it.next();
            ki.update();
            if (ki.life <= 0) it.remove();
        }

        if (target == null || target.isDead) {
            currentState = State.IDLE;
        }

        if (actionCD > 0) actionCD--;

        if (currentState != State.IDLE && currentState != State.ULT_LIGHT_WINDUP && currentState != State.ULT_LIGHT_LEAP &&
            currentState != State.ULT_DARK_WINDUP && currentState != State.ULT_DARK_TORNADO) {
            
            if (!hasTriggeredUlt33 && hp <= 500) { 
                currentState = State.ULT_DARK_WINDUP;
                hasTriggeredUlt33 = true;
                facingAngle = Math.atan2(450 - y, 450 - x);
                return;
            }
            if (!hasTriggeredUlt66 && hp <= 1000) { 
                currentState = State.ULT_LIGHT_WINDUP;
                hasTriggeredUlt66 = true;
                facingAngle = Math.atan2(450 - y, 450 - x);
                return;
            }
        }

        if (currentState == State.IDLE) { visualScale = 1.0; } 
        
        else if (currentState == State.CHASE) {
            double dist = Math.hypot(target.x - x, target.y - y);
            facingAngle = Math.atan2(target.y - y, target.x - x);
            
            if (dist > 70) {
                double speed = 1.3; 
                x += Math.cos(facingAngle) * speed;
                y += Math.sin(facingAngle) * speed;
            }
            
            if (actionCD <= 0 && !target.isDead) {
                facingAngle = Math.atan2(target.y - y, target.x - x); 
                if (dist <= 85) {
                    if (Math.random() < 0.2) { currentState = State.SHIFT_WINDUP; stateTimer = 15; }
                    else { currentState = State.M1_WINDUP; stateTimer = 20; m1Step = 0; hasHitThisMove = false; }
                } else if (dist <= 180) {
                    if (Math.random() < 0.5) { currentState = State.M2_WINDUP; stateTimer = 25; }
                    else { currentState = State.Q_WINDUP; stateTimer = 30; }
                } else {
                    currentState = State.E_WINDUP; stateTimer = 40;
                }
            }
            
            illuminaAngle += (-Math.PI / 4 - illuminaAngle) * 0.2;
            darkheartAngle += (Math.PI / 4 - darkheartAngle) * 0.2;
            illuminaExt += (0 - illuminaExt) * 0.2;
            darkheartExt += (0 - darkheartExt) * 0.2;
        }
        else if (currentState == State.M1_WINDUP) {
            stateTimer--;
            if (stateTimer == 19 || stateTimer == 11) {
                hasHitThisMove = false; 
            }
            if (m1Step == 0) { illuminaAngle = Math.PI/2; illuminaExt = -15 + shake(); }
            else if (m1Step == 1) { darkheartAngle = -Math.PI/2; darkheartExt = -15 + shake(); }
            else { illuminaAngle = Math.PI/4; darkheartAngle = -Math.PI/4; illuminaExt = -15 + shake(); darkheartExt = -15 + shake(); }
            
            if (stateTimer <= 0) { currentState = State.M1_SLASH; stateTimer = 15; }
        }
        else if (currentState == State.M1_SLASH) {
            stateTimer--;
            if (stateTimer == 14) { 
                int dmg = 6;
                if (m1Step == 0) {
                    illuminaExt = 25;
                    swordAuras.add(new SwordKi(x, y, facingAngle, 65, new Color(255, 180, 200), 0));
                    strikePlayer(dmg, true, false, 75, Math.PI/3);
                } else if (m1Step == 1) {
                    darkheartExt = 25;
                    swordAuras.add(new SwordKi(x, y, facingAngle, 65, new Color(100, 0, 150), 0));
                    strikePlayer(dmg, false, true, 75, Math.PI/3);
                } else {
                    illuminaExt = 30; darkheartExt = 30;
                    swordAuras.add(new SwordKi(x, y, facingAngle - 0.2, 75, new Color(255, 180, 200), 0));
                    swordAuras.add(new SwordKi(x, y, facingAngle + 0.2, 75, new Color(100, 0, 150), 0));
                    strikePlayer((int)(dmg * 1.5), true, true, 85, Math.PI/2);
                }
            }
            if (stateTimer <= 0) {
                if (m1Step < 2) { m1Step++; currentState = State.M1_WINDUP; stateTimer = 12; } 
                else { currentState = State.CHASE; actionCD = 90; } 
            }
        }
        else if (currentState == State.M2_WINDUP) {
            stateTimer--; 
            illuminaAngle = 0; darkheartAngle = 0;
            illuminaExt = -15 + shake(); darkheartExt = -15 + shake();
            if (stateTimer <= 0) {
                currentState = State.M2_DASH; stateTimer = 15;
                hasHitThisMove = false; 
                dashVx = Math.cos(facingAngle) * 11; dashVy = Math.sin(facingAngle) * 11;
            }
        }
        else if (currentState == State.M2_DASH) {
            stateTimer--; x += dashVx; y += dashVy;
            illuminaExt = 30; darkheartExt = 30;
            if (stateTimer % 4 == 0) swordAuras.add(new SwordKi(x, y, facingAngle, 40, Color.WHITE, 0)); 
            strikePlayer(8, true, false, 60, Math.PI); 
            if (stateTimer <= 0) { currentState = State.CHASE; actionCD = 120; } 
        }
        else if (currentState == State.SHIFT_WINDUP) {
            stateTimer--;
            if (stateTimer <= 0) {
                currentState = State.SHIFT_DASH; stateTimer = 10;
                dashVx = Math.cos(facingAngle + Math.PI) * 12; dashVy = Math.sin(facingAngle + Math.PI) * 12;
            }
        }
        else if (currentState == State.SHIFT_DASH) {
            stateTimer--; x += dashVx; y += dashVy;
            if (stateTimer <= 0) { currentState = State.CHASE; actionCD = 45; }
        }
        else if (currentState == State.Q_WINDUP) {
            stateTimer--; visualScale = 0.8; 
            illuminaAngle = Math.PI; darkheartAngle = -Math.PI;
            if (stateTimer <= 0) {
                currentState = State.Q_LEAP; stateTimer = 30; hasHitThisMove = false; 
                dashVx = (target.x - x) / 30.0; dashVy = (target.y - y) / 30.0;
            }
        }
        else if (currentState == State.Q_LEAP) {
            stateTimer--; x += dashVx; y += dashVy;
            if (stateTimer > 15) visualScale += 0.04; else visualScale -= 0.04;
            illuminaAngle += 0.4; darkheartAngle -= 0.4;
            if (stateTimer <= 0) {
                visualScale = 1.0;
                swordAuras.add(new SwordKi(x, y, 0, 120, new Color(200, 0, 255), 1)); 
                strikePlayer(12, true, true, 120, Math.PI * 2); 
                currentState = State.CHASE; actionCD = 140; 
            }
        }
        else if (currentState == State.E_WINDUP) {
            stateTimer--;
            illuminaAngle = -Math.PI/2; darkheartAngle = Math.PI/2;
            illuminaExt = -10 + shake(); darkheartExt = -10 + shake();
            if (stateTimer <= 0) {
                currentState = State.E_SWEEP; stateTimer = 20; hasHitThisMove = false; 
                dashVx = Math.cos(facingAngle) * 14; dashVy = Math.sin(facingAngle) * 14;
            }
        }
        else if (currentState == State.E_SWEEP) {
            stateTimer--; x += dashVx; y += dashVy;
            illuminaAngle = 0; darkheartAngle = 0; illuminaExt = 40; darkheartExt = 40;
            if (stateTimer == 19) swordAuras.add(new SwordKi(x, y, facingAngle, 120, new Color(220, 200, 255), 0)); 
            strikePlayer(10, true, true, 120, Math.PI/2);
            if (stateTimer <= 0) { currentState = State.CHASE; actionCD = 160; } 
        }
        
        // ==========================================
        // ☀️ 66% 大招：光明隕落 (Illumina Slam)
        // ==========================================
        else if (currentState == State.ULT_LIGHT_WINDUP) {
            facingAngle = Math.atan2(450 - y, 450 - x);
            double distToCenter = Math.hypot(450 - x, 450 - y);
            if (distToCenter > 10) {
                x += Math.cos(facingAngle) * 5.0; y += Math.sin(facingAngle) * 5.0;
                illuminaExt = shake(); darkheartExt = shake();
            } else {
                x = 450; y = 450; currentState = State.ULT_LIGHT_LEAP; stateTimer = 60; 
            }
        }
        else if (currentState == State.ULT_LIGHT_LEAP) {
            stateTimer--; facingAngle = Math.PI / 2; 
            if (stateTimer > 30) visualScale += 0.05; else visualScale -= 0.05;
            illuminaAngle -= 0.3; darkheartAngle += 0.3;
            
            if (stateTimer == 0) { 
                visualScale = 1.0;
                swordAuras.add(new SwordKi(x, y, 0, 160, new Color(255, 255, 180), 1)); 
                
                core.GamePanel.screenShakeTimer = 35; 
                
                for (int i = 0; i < 60; i++) {
                    GamePanel.particles.add(new vfx.Particle(x, y, new Color(255, 245, 160)));
                }

                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    double tx = x + Math.cos(angle) * 100; double ty = y + Math.sin(angle) * 100;
                    GamePanel.projectiles.add(new entity.Projectile(x, y, tx, ty));
                }
                currentState = State.CHASE; actionCD = 90; 
            }
        }
        
        // ==========================================
        // 🌑 33% 大招：暗心龍捲 (Darkheart Tornado)
        // ==========================================
        else if (currentState == State.ULT_DARK_WINDUP) {
            facingAngle = Math.atan2(450 - y, 450 - x);
            double distToCenter = Math.hypot(450 - x, 450 - y);
            if (distToCenter > 10) {
                x += Math.cos(facingAngle) * 5.0; y += Math.sin(facingAngle) * 5.0;
            } else {
                x = 450; y = 450;
                currentState = State.ULT_DARK_TORNADO;
                stateTimer = 480; 
            }
        }
        else if (currentState == State.ULT_DARK_TORNADO) {
            stateTimer--;
            facingAngle += 0.18; 
            illuminaAngle = Math.PI / 2; darkheartAngle = -Math.PI / 2;
            
            if (stateTimer % 2 == 0) {
                GamePanel.particles.add(new vfx.Particle(x, y, new Color(70, 0, 110)));
                
                // ★ 海量方塊粒子：每 2 幀噴發 4 顆，半徑從 20 擴張到 380 邊緣
                for (int i = 0; i < 4; i++) {
                    double randRadius = 20 + Math.random() * 360; 
                    swordAuras.add(new SwordKi(x, y, 0, randRadius, Color.BLACK, 2)); 
                }
            }

            if (stateTimer % 12 == 0 && !target.isDead) {
                double distToPlayer = Math.hypot(target.x - 450, target.y - 450);
                // ★ 判定範圍擴大至 400，玩家必須躲在最角落
                if (distToPlayer < 400) {
                    // ★ 龍捲風套用難度倍率
                    int scaledDmg = (int)(5 * GamePanel.difficultyMultiplier);
                    int finalDmg = (target.glowingTimer > 0) ? scaledDmg * 2 : scaledDmg;
                    target.hp -= finalDmg;
                    GamePanel.dmgTexts.add(new DamageText(target.x, target.y - 20, finalDmg, DamageText.TextType.TAKEN));
                    
                    int heal = finalDmg * 2; 
                    this.hp = Math.min(1500, this.hp + heal);
                    GamePanel.dmgTexts.add(new DamageText(this.x, this.y - 40, heal, DamageText.TextType.HEAL));
                }
            }

            if (stateTimer <= 0) { currentState = State.CHASE; actionCD = 120; }
        }

        x = Math.max(radius + 15, Math.min(900 - radius - 15, x));
        y = Math.max(radius + 15, Math.min(900 - radius - 15, y));
    }

    // ★ 傷害判定全面套用 difficultyMultiplier
    private void strikePlayer(int baseDmg, boolean applyGlow, boolean applyLifesteal, double range, double arc) {
        if (target.isDead) return;
        if (hasHitThisMove) return; 
        
        double dist = Math.hypot(target.x - x, target.y - y);
        if (dist <= range) {
            double angleToTarget = Math.atan2(target.y - y, target.x - x);
            double angleDiff = Math.abs(angleToTarget - facingAngle);
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            angleDiff = Math.abs(angleDiff);
            
            if (angleDiff <= arc) {
                hasHitThisMove = true; 
                
                // ★ 常規攻擊套用難度倍率
                int scaledDmg = (int)(baseDmg * GamePanel.difficultyMultiplier);
                int finalDmg = (target.glowingTimer > 0) ? scaledDmg * 2 : scaledDmg;
                
                target.hp -= finalDmg;
                GamePanel.dmgTexts.add(new DamageText(target.x, target.y - 20, finalDmg, DamageText.TextType.TAKEN));
                
                if (applyGlow) target.glowingTimer = 600; 
                
                int universalHeal = (int)(finalDmg * 0.5);
                if (universalHeal > 0) {
                    this.hp = Math.min(1500, this.hp + universalHeal);
                    GamePanel.dmgTexts.add(new DamageText(this.x, this.y - 35, universalHeal, DamageText.TextType.HEAL));
                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        AffineTransform oldAT = g2d.getTransform();
        g2d.translate(x, y); g2d.scale(visualScale, visualScale); g2d.rotate(facingAngle);

        g2d.setColor(new Color(255, 230, 240)); g2d.fillArc(-radius, -radius, radius * 2, radius * 2, 90, 180);
        g2d.setColor(new Color(20, 20, 20)); g2d.fillArc(-radius, -radius, radius * 2, radius * 2, -90, 180);

        int headR = 16;
        g2d.setColor(new Color(255, 230, 240)); g2d.fillArc(-headR, -headR, headR * 2, headR * 2, 90, 180);
        g2d.setColor(new Color(20, 20, 20)); g2d.fillArc(-headR, -headR, headR * 2, headR * 2, -90, 180);

        AffineTransform swordL = g2d.getTransform(); g2d.translate(0, -22); g2d.rotate(illuminaAngle); g2d.translate(15 + illuminaExt, 0);
        g2d.setColor(new Color(255, 190, 215)); int[] ilX = {0, 45, 55, 45, 0}; int[] ilY = {-4, -3, 0, 3, 4};
        g2d.fillPolygon(ilX, ilY, 5); g2d.setColor(Color.WHITE); g2d.drawPolygon(ilX, ilY, 5); g2d.setTransform(swordL);

        AffineTransform swordR = g2d.getTransform(); g2d.translate(0, 22); g2d.rotate(darkheartAngle); g2d.translate(15 + darkheartExt, 0);
        g2d.setColor(new Color(40, 0, 60)); int[] dhX = {0, 45, 55, 45, 0}; int[] dhY = {-5, -5, 0, 5, 5}; 
        g2d.fillPolygon(dhX, dhY, 5); g2d.setColor(new Color(150, 0, 200)); g2d.drawPolygon(dhX, dhY, 5); g2d.setTransform(swordR);

        g2d.setColor(Color.BLACK); g2d.setStroke(new BasicStroke(3)); g2d.drawOval(-radius, -radius, radius * 2, radius * 2); g2d.drawOval(-headR, -headR, headR * 2, headR * 2); g2d.setStroke(new BasicStroke(1));
        g2d.setTransform(oldAT);

        for (SwordKi ki : swordAuras) ki.draw(g2d);
    }

    class SwordKi {
        double x, y, angle, radius; int life, maxLife; Color color; 
        int type; 
        double orbitAngle, orbitSpeed; int squareSize;
        
        SwordKi(double x, double y, double angle, double radius, Color c, int type) {
            this.x = x; this.y = y; this.angle = angle; this.radius = radius; this.color = c; this.type = type;
            this.maxLife = 15; this.life = 15;
            
            if (type == 2) {
                this.maxLife = 70 + (int)(Math.random() * 50); 
                this.life = this.maxLife;
                this.orbitAngle = Math.random() * Math.PI * 2; 
                this.orbitSpeed = 0.04 + Math.random() * 0.07; 
                this.squareSize = 5 + (int)(Math.random() * 14); 
            }
        }
        
        void update() { 
            life--; 
            if (type == 2) {
                orbitAngle += orbitSpeed; 
                radius += (Math.random() * 3 - 1.5); 
            }
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.max(0, (float)life / maxLife);
            
            if (type == 2) {
                g2d.setColor(new Color(12, 12, 12, (int)(alpha * 230)));
                double sqX = Equinox.this.x + Math.cos(orbitAngle) * radius;
                double sqY = Equinox.this.y + Math.sin(orbitAngle) * radius;
                g2d.fillRect((int)sqX - squareSize/2, (int)sqY - squareSize/2, squareSize, squareSize);
                
                g2d.setColor(new Color(140, 0, 240, (int)(alpha * 160)));
                g2d.drawRect((int)sqX - squareSize/2, (int)sqY - squareSize/2, squareSize, squareSize);
            } 
            else if (type == 1) { 
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 200)));
                g2d.setStroke(new BasicStroke(5));
                int r = (int)(radius * 2); int currentR = (int)(r * (1.0 - alpha) + r * 0.2); 
                g2d.drawOval((int)x - currentR/2, (int)y - currentR/2, currentR, currentR);
                g2d.setStroke(new BasicStroke(1));
            } 
            else { 
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 200)));
                g2d.setStroke(new BasicStroke(5));
                AffineTransform old = g2d.getTransform(); g2d.translate(x, y); g2d.rotate(angle); int r = (int)radius;
                g2d.drawArc(-r, -r, r*2, r*2, -60, 120); g2d.setTransform(old);
                g2d.setStroke(new BasicStroke(1));
            }
        }
    }
}