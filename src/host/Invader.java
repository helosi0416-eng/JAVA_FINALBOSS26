package host;

import core.GamePanel;
import entity.Dummy;
import entity.Entity; // ★ 新增：匯入通用實體
import entity.Equinox;
import entity.Player;
import vfx.DamageText;
import vfx.Particle;
import vfx.TrailFrame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class Invader extends Host {

    private double curLeftX = 10, curLeftY = -28, curLeftAngle = 0;
    private double curRightX = 10, curRightY = 28, curRightAngle = 0;
    private double extraSpinAngle = 0; 

    public boolean isSlashing = false;
    private int slashTimer = 0;
    public int slashCD = 0;
    private final int MAX_SLASH_CD = 30; 
    private boolean slashHasHit = false;
    private boolean nextSwingIsLeft = true; 
    private int moveStunTimer = 0; 

    public boolean isSpinning = false;
    private int spinTimer = 0;
    public int spinCD = 0;
    private final int MAX_SPIN_CD = 360; 
    private final int spinRadius = 90;

    public boolean isDashing = false;
    private int dashTimer = 0;
    public int dashCD = 0;
    private final int MAX_DASH_CD = 600; 
    private double dashVx, dashVy;
    private boolean dashHasHit = false;

    public boolean isEmpowered = false;
    public int empowerDuration = 0;
    public int empowerCD = 0;
    private final int MAX_EMPOWER_CD = 1200; 
    private final int MAX_EMPOWER_DURATION = 120; 

    public Invader(Player player) {
        super(player);
    }

    @Override
    public void update() {
        double prevLeftX = curLeftX, prevLeftY = curLeftY, prevLeftAngle = curLeftAngle;
        double prevRightX = curRightX, prevRightY = curRightY, prevRightAngle = curRightAngle;

        if (!isDashing) {
            double targetAngle = Math.atan2(player.mouseY - player.y, player.mouseX - player.x);
            double angleDiff = targetAngle - player.aimAngle;
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
            player.aimAngle += angleDiff * 0.1; 
        }

        if (moveStunTimer > 0) moveStunTimer--;

        if (isDashing) {
            player.x += dashVx;
            player.y += dashVy;
        } else if (moveStunTimer <= 0) {
            if (player.w) player.y -= player.speed;
            if (player.s) player.y += player.speed;
            if (player.a) player.x -= player.speed;
            if (player.d) player.x += player.speed;
        }
        
        player.x = Math.max(player.radius, Math.min(900 - player.radius, player.x));
        player.y = Math.max(player.radius, Math.min(900 - player.radius, player.y));

        if (slashCD > 0) slashCD--;
        if (spinCD > 0) spinCD--;
        if (dashCD > 0) dashCD--;
        if (empowerCD > 0) empowerCD--;

        if (isEmpowered) {
            empowerDuration--;
            if (empowerDuration <= 0) isEmpowered = false;
            GamePanel.particles.add(new Particle(player.x, player.y));
            GamePanel.particles.add(new Particle(player.x, player.y));
        }

        double tgtLeftX = 5, tgtLeftY = -28, tgtLeftAngle = 0;
        double tgtRightX = 5, tgtRightY = 28, tgtRightAngle = 0;
        double lerpSpeed = 0.4; 

        if (isDashing) {
            tgtLeftX = 25;  tgtLeftY = 5;  tgtLeftAngle = Math.toRadians(45);  
            tgtRightX = 25; tgtRightY = -5; tgtRightAngle = Math.toRadians(-45); 
        } else if (isSpinning) {
            tgtLeftX = 0;  tgtLeftY = -45; tgtLeftAngle = Math.toRadians(-90); 
            tgtRightX = 0; tgtRightY = 45; tgtRightAngle = Math.toRadians(90);  
        } else if (isSlashing) {
            if (nextSwingIsLeft) {
                if (slashTimer > 12) { lerpSpeed = 0.25; tgtLeftX = 5; tgtLeftY = -2; tgtLeftAngle = Math.toRadians(70); } 
                else { lerpSpeed = 0.8; tgtLeftX = 40; tgtLeftY = -45; tgtLeftAngle = Math.toRadians(-70); }
                tgtRightX = -5; tgtRightY = 25; tgtRightAngle = 0;
            } else {
                if (slashTimer > 12) { lerpSpeed = 0.25; tgtRightX = 5; tgtRightY = 2; tgtRightAngle = Math.toRadians(-70); } 
                else { lerpSpeed = 0.8; tgtRightX = 40; tgtRightY = 45; tgtRightAngle = Math.toRadians(70); }
                tgtLeftX = -5; tgtLeftY = -25; tgtLeftAngle = 0;
            }
        }

        curLeftX += (tgtLeftX - curLeftX) * lerpSpeed; curLeftY += (tgtLeftY - curLeftY) * lerpSpeed; curLeftAngle += (tgtLeftAngle - curLeftAngle) * lerpSpeed;
        curRightX += (tgtRightX - curRightX) * lerpSpeed; curRightY += (tgtRightY - curRightY) * lerpSpeed; curRightAngle += (tgtRightAngle - curRightAngle) * lerpSpeed;

        boolean shouldDrawTrail = isSpinning || isDashing || (isSlashing && slashTimer <= 12);
        if (shouldDrawTrail) {
            int steps = (isSlashing) ? 4 : 2; 
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                double interpLeftX = prevLeftX + (curLeftX - prevLeftX) * t; double interpLeftY = prevLeftY + (curLeftY - prevLeftY) * t; double interpLeftAngle = prevLeftAngle + (curLeftAngle - prevLeftAngle) * t;
                double interpRightX = prevRightX + (curRightX - prevRightX) * t; double interpRightY = prevRightY + (curRightY - prevRightY) * t; double interpRightAngle = prevRightAngle + (curRightAngle - prevRightAngle) * t;
                GamePanel.trails.add(new TrailFrame(player.x, player.y, player.aimAngle, extraSpinAngle, interpLeftX, interpLeftY, interpLeftAngle, interpRightX, interpRightY, interpRightAngle));
            }
        }

        // --- 技能碰撞判定 ---
        if (isSlashing) {
            slashTimer--;
            if (slashTimer <= 12 && !slashHasHit) {
                boolean hitAnything = false; 
                // 1. 打假人
                for (Dummy dummy : GamePanel.dummies) {
                    double dist = Math.hypot(dummy.x - player.x, dummy.y - player.y);
                    double angleToEnemy = Math.atan2(dummy.y - player.y, dummy.x - player.x);
                    double angleDiff = Math.abs(angleToEnemy - player.aimAngle);
                    while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                    if (dist <= 70 + dummy.radius && Math.abs(angleDiff) <= Math.toRadians(50)) {
                        dealDamage(dummy, 8, true); hitAnything = true;
                    }
                }
                // 2. 打 Boss
                if (GamePanel.boss != null) {
                    double dist = Math.hypot(GamePanel.boss.x - player.x, GamePanel.boss.y - player.y);
                    double angleToEnemy = Math.atan2(GamePanel.boss.y - player.y, GamePanel.boss.x - player.x);
                    double angleDiff = Math.abs(angleToEnemy - player.aimAngle);
                    while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                    if (dist <= 70 + GamePanel.boss.radius && Math.abs(angleDiff) <= Math.toRadians(50)) {
                        dealDamageToBoss(GamePanel.boss, 10, true); hitAnything = true;
                    }
                }
                if (hitAnything) {
                    spinCD = Math.max(0, spinCD - 60); dashCD = Math.max(0, dashCD - 60); empowerCD = Math.max(0, empowerCD - 60);
                }
                slashHasHit = true; 
            }
            if (slashTimer <= 0) isSlashing = false;
        }

        if (isSpinning) {
            spinTimer--; extraSpinAngle += 0.5; 
            if (spinTimer <= 0) { isSpinning = false; extraSpinAngle = 0; }
            if (spinTimer % 12 == 0) {
                for (Dummy dummy : GamePanel.dummies) {
                    if (Math.hypot(dummy.x - player.x, dummy.y - player.y) <= spinRadius + dummy.radius) dealDamage(dummy, 4, false); 
                }
                if (GamePanel.boss != null && Math.hypot(GamePanel.boss.x - player.x, GamePanel.boss.y - player.y) <= spinRadius + GamePanel.boss.radius) {
                    dealDamageToBoss(GamePanel.boss, 6, false);
                }
            }
        }

        if (isDashing) {
            dashTimer--;
            if (dashTimer <= 0) isDashing = false;
            if (!dashHasHit) {
                for (Dummy dummy : GamePanel.dummies) {
                    if (Math.hypot(dummy.x - player.x, dummy.y - player.y) <= player.radius + dummy.radius + 15) {
                        dealDamage(dummy, 24, false); dashHasHit = true; 
                    }
                }
                if (GamePanel.boss != null && Math.hypot(GamePanel.boss.x - player.x, GamePanel.boss.y - player.y) <= player.radius + GamePanel.boss.radius + 15) {
                    dealDamageToBoss(GamePanel.boss, 36, false); dashHasHit = true; 
                }
            }
        }
    }

    private void dealDamage(Dummy dummy, int baseDamage, boolean isM1) {
        int finalDamage = baseDamage; boolean isCrit = false;
        if (isEmpowered) {
            finalDamage = (int)(baseDamage * 1.5); isCrit = true; 
            if (isM1) empowerDuration = Math.min(MAX_EMPOWER_DURATION, empowerDuration + 60);
            int healAmount = (int)(finalDamage * 0.2);
            if (healAmount > 0) {
                player.hp = Math.min(player.maxHp, player.hp + healAmount);
                GamePanel.dmgTexts.add(new DamageText(player.x + (Math.random() * 20 - 10), player.y - 35, healAmount, DamageText.TextType.HEAL));
            }
        }
        dummy.hp -= finalDamage; dummy.blinkTimer = 30;
        if (isM1 && Math.random() < 0.15) {
            double kbAngle = Math.atan2(dummy.y - player.y, dummy.x - player.x);
            dummy.kx = Math.cos(kbAngle) * 13; dummy.ky = Math.sin(kbAngle) * 13; dummy.stunTimer = 60; 
        }
        GamePanel.dmgTexts.add(new DamageText(dummy.x + (Math.random() * 20 - 10), dummy.y - 20, finalDamage, isCrit ? DamageText.TextType.DEALT_CRIT : DamageText.TextType.DEALT));
    }

    // ★ 核心修正：括號裡的 Equinox 改成了 Entity，這樣就能打所有 Boss 了！
    private void dealDamageToBoss(Entity boss, int baseDamage, boolean isM1) {
        int finalDamage = baseDamage; boolean isCrit = false;
        if (isEmpowered) {
            finalDamage = (int)(baseDamage * 1.5); isCrit = true; 
            if (isM1) empowerDuration = Math.min(MAX_EMPOWER_DURATION, empowerDuration + 60);
            int healAmount = (int)(finalDamage * 0.2);
            if (healAmount > 0) {
                player.hp = Math.min(player.maxHp, player.hp + healAmount);
                GamePanel.dmgTexts.add(new DamageText(player.x + (Math.random() * 20 - 10), player.y - 35, healAmount, DamageText.TextType.HEAL));
            }
        }
        boss.hp -= finalDamage; 
        // 絕對霸體：不賦予 kx, ky，也不賦予 stunTimer
        GamePanel.dmgTexts.add(new DamageText(boss.x + (Math.random() * 20 - 10), boss.y - 40, finalDamage, isCrit ? DamageText.TextType.DEALT_CRIT : DamageText.TextType.DEALT));
    }

    @Override
    public void draw(Graphics2D g2d) {
        AffineTransform oldAT = g2d.getTransform();
        g2d.translate(player.x, player.y);
        g2d.rotate(player.aimAngle + extraSpinAngle);

        if (isEmpowered) {
            g2d.setColor(new Color(57, 255, 20, 60));
            g2d.fillOval(-45, -45, 90, 90);
        }

        int[] sx = {0, 15, 45, 45, 15, 0};
        int[] sy = {-3, -7, -3, 3, 7, 3};

        AffineTransform atLeft = g2d.getTransform();
        g2d.translate(curLeftX, curLeftY); g2d.rotate(curLeftAngle); g2d.setColor(new Color(130, 130, 130)); g2d.fillPolygon(sx, sy, 6); g2d.setColor(new Color(30, 180, 50)); g2d.fillOval(-10, -10, 20, 20); g2d.setTransform(atLeft); 

        AffineTransform atRight = g2d.getTransform();
        g2d.translate(curRightX, curRightY); g2d.rotate(curRightAngle); g2d.setColor(new Color(130, 130, 130)); g2d.fillPolygon(sx, sy, 6); g2d.setColor(new Color(30, 180, 50)); g2d.fillOval(-10, -10, 20, 20); g2d.setTransform(atRight); 

        g2d.setColor(new Color(180, 230, 50));
        int[] sp1x = {-15, -35, -15}; int[] sp1y = {-10, -15, -5}; g2d.fillPolygon(sp1x, sp1y, 3);
        int[] sp2x = {-20, -40, -20}; int[] sp2y = {-2, 0, 2}; g2d.fillPolygon(sp2x, sp2y, 3);
        int[] sp3x = {-15, -35, -15}; int[] sp3y = {5, 15, 10}; g2d.fillPolygon(sp3x, sp3y, 3);

        g2d.setColor(new Color(34, 139, 34)); g2d.fillOval(-player.radius, -player.radius, player.radius * 2, player.radius * 2);

        g2d.setColor(new Color(180, 230, 50));
        g2d.fillRect(2, -15, 4, 30); g2d.fillRect(2, -15, 15, 4); g2d.fillRect(2, -2, 15, 4); g2d.fillRect(2, 11, 15, 4);   
        
        g2d.setTransform(oldAT); 
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && !isSlashing && !isDashing && !isSpinning && slashCD <= 0) {
            isSlashing = true; slashTimer = 30; slashCD = MAX_SLASH_CD; slashHasHit = false; nextSwingIsLeft = !nextSwingIsLeft; moveStunTimer = 18; 
        }
        if (e.getButton() == MouseEvent.BUTTON3 && !isSpinning && !isSlashing && !isDashing && spinCD <= 0) {
            isSpinning = true; spinTimer = 48; spinCD = MAX_SPIN_CD; 
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) player.w = true; if (key == KeyEvent.VK_S) player.s = true; if (key == KeyEvent.VK_A) player.a = true; if (key == KeyEvent.VK_D) player.d = true;
        
        if (key == KeyEvent.VK_SHIFT && !isDashing && !isSlashing && !isSpinning && dashCD <= 0) {
            isDashing = true; dashTimer = 15; dashCD = MAX_DASH_CD; dashHasHit = false;
            dashVx = Math.cos(player.aimAngle) * player.speed * 4.8; dashVy = Math.sin(player.aimAngle) * player.speed * 4.8;
        }

        if (key == KeyEvent.VK_Q && !isEmpowered && empowerCD <= 0) {
            isEmpowered = true; empowerDuration = MAX_EMPOWER_DURATION; empowerCD = MAX_EMPOWER_CD; player.hp -= 10;
            GamePanel.dmgTexts.add(new DamageText(player.x + (Math.random() * 20 - 10), player.y - 20, 10, DamageText.TextType.TAKEN));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) player.w = false; if (key == KeyEvent.VK_S) player.s = false; if (key == KeyEvent.VK_A) player.a = false; if (key == KeyEvent.VK_D) player.d = false; 
    }
}