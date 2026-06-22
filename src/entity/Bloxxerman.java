package entity;

import core.GamePanel;
import vfx.DamageText;
import vfx.Particle;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Field;

public class Bloxxerman extends Entity {
    
    public Player target;
    public double aimAngle = 0;
    
    public double z = 1500; 
    
    public enum State { 
        SPAWNING,      
        IDLE, KITE, 
        NAIL_SPLASH_WINDUP, NAIL_SPLASH_FIRE, 
        IMPALE_WINDUP, IMPALE_FIRE,           
        JUMP_UP, JUMP_HOVER, JUMP_DOWN,       
        SUPER_SPLASH_WINDUP, SUPER_SPLASH_FIRE,
        DEATH_DEFIANCE,        
        PHASE2_JUMP_UP,        
        PHASE2_JUMP_DOWN,      
        PHASE2_ENDLESS_SPLASH, 
        DEATH_FLYING           
    }
    public State currentState = State.SPAWNING;
    public int stateTimer = 180; 
    
    public int splashCD = 120;
    public int impaleCD = 240;
    public int jumpCD = 400;
    public int superSplashCD = 600;

    private double leftArmAngle = -0.5;
    private double rightArmAngle = 0.5;
    private double leftArmDist = 45; 
    private double rightArmDist = 45;
    private boolean showGiantNail = false; 

    private boolean hasTriggeredPhase2 = false;
    private double dashVx = 0, dashVy = 0;

    private int playerKbTimer = 0;
    private double playerKbVx = 0;
    private double playerKbVy = 0;

    private ArrayList<RingWave> rings = new ArrayList<>();
    private ArrayList<BloxxerMinion> minions = new ArrayList<>();
    private ArrayList<Tripwire> tripwires = new ArrayList<>(); 

    public Bloxxerman(double x, double y, Player target) {
        super(450, 450, 32, 1500); 
        this.target = target;
        this.z = 1500; 
    }

    @Override
    public void update() {
        if (target == null || target.isDead || GamePanel.isTimeStopped) return;

        if (playerKbTimer > 0) {
            playerKbTimer--;
            target.x += playerKbVx;
            target.y += playerKbVy;
            target.x = Math.max(target.radius + 15, Math.min(900 - target.radius - 15, target.x));
            target.y = Math.max(target.radius + 15, Math.min(900 - target.radius - 15, target.y));
        }

        if (currentState == State.DEATH_DEFIANCE || currentState == State.PHASE2_JUMP_UP || currentState == State.PHASE2_JUMP_DOWN) {
            if (hp <= 0) hp = 1; 
        } 
        else if (hp <= 0 && currentState != State.DEATH_FLYING) {
            if (!hasTriggeredPhase2) {
                hp = 1;
                hasTriggeredPhase2 = true;
                currentState = State.DEATH_DEFIANCE;
                stateTimer = 180; 
                minions.clear(); 
            } else {
                hp = 1;
                currentState = State.DEATH_FLYING;
                double flyAngle = Math.atan2(y - target.y, x - target.x); 
                dashVx = Math.cos(flyAngle) * 35; 
                dashVy = Math.sin(flyAngle) * 35;
            }
        }

        double dist = Math.hypot(target.x - x, target.y - y);
        if (currentState != State.SPAWNING && currentState != State.JUMP_UP && currentState != State.JUMP_HOVER && currentState != State.PHASE2_JUMP_UP && currentState != State.PHASE2_JUMP_DOWN) {
            aimAngle = Math.atan2(target.y - y, target.x - x); 
        }

        updateRings();
        updateMinions();
        updateTripwires(); 

        if (splashCD > 0) splashCD--;
        if (impaleCD > 0) impaleCD--;
        if (jumpCD > 0) jumpCD--;
        if (superSplashCD > 0) superSplashCD--;

        if (currentState == State.DEATH_FLYING) {
            x += dashVx; 
            y += dashVy;
            aimAngle += 0.5; 
            if (x < 30 || x > 870 || y < 30 || y > 870) {
                GamePanel.screenShakeTimer = 50;
                for(int i=0; i<150; i++) GamePanel.particles.add(new Particle(x, y, new Color(255, 60, 0)));
                hp = 0; 
            }
            return; 
        }

        if (currentState == State.DEATH_DEFIANCE) {
            stateTimer--;
            leftArmAngle = -0.5; rightArmAngle = 0.5;
            if (stateTimer <= 0) {
                hp = 200; 
                currentState = State.PHASE2_JUMP_UP; 
                stateTimer = 30;
            }
        }
        else if (currentState == State.PHASE2_JUMP_UP) {
            stateTimer--; z += 50;
            if (stateTimer <= 0) {
                x = 450; y = 450; 
                currentState = State.PHASE2_JUMP_DOWN; 
                stateTimer = 120; 
            }
        }
        else if (currentState == State.PHASE2_JUMP_DOWN) {
            stateTimer--; z = (stateTimer / 120.0) * 1500;
            if (stateTimer <= 0) {
                z = 0;
                triggerMassiveLandingEffect(x, y); 
                generateTripwires(15); 
                currentState = State.PHASE2_ENDLESS_SPLASH;
            }
        }
        else if (currentState == State.PHASE2_ENDLESS_SPLASH) {
            stateTimer++;
            leftArmAngle = -0.1; rightArmAngle = 0.1; leftArmDist = 55; rightArmDist = 55;
            if (stateTimer % 5 == 0) {
                double offset = (Math.random() - 0.5) * 90; 
                double spawnX = x + Math.cos(aimAngle + Math.PI/2) * offset;
                double spawnY = y + Math.sin(aimAngle + Math.PI/2) * offset;
                fireNailFrom(spawnX, spawnY, aimAngle, 3);
            }
            return; 
        }

        if (currentState == State.SPAWNING) {
            stateTimer--;
            z = (stateTimer / 180.0) * 1500; 
            if (stateTimer <= 0) {
                z = 0;
                triggerMassiveLandingEffect(x, y); 
                currentState = State.KITE;
                stateTimer = 30; 
            }
            return;
        }

        if (currentState == State.IDLE || currentState == State.KITE) {
            double speed = 1.6;
            if (dist < 250) { x -= Math.cos(aimAngle) * speed; y -= Math.sin(aimAngle) * speed; } 
            else if (dist > 450) { x += Math.cos(aimAngle) * speed; y += Math.sin(aimAngle) * speed; } 
            else { x += Math.cos(aimAngle + Math.PI / 2) * speed; y += Math.sin(aimAngle + Math.PI / 2) * speed; }

            x = Math.max(radius + 15, Math.min(900 - radius - 15, x)); y = Math.max(radius + 15, Math.min(900 - radius - 15, y));
            leftArmAngle = -0.5; rightArmAngle = 0.5; leftArmDist = 45; rightArmDist = 45;
            showGiantNail = false;

            if (dist < 150 && impaleCD <= 0 && Math.random() < 0.05) { 
                currentState = State.IMPALE_WINDUP; stateTimer = 45; 
            } else if (superSplashCD <= 0 && dist < 500 && Math.random() < 0.02) { 
                currentState = State.SUPER_SPLASH_WINDUP; stateTimer = 120; 
            } else if (jumpCD <= 0 && Math.random() < 0.03) { 
                currentState = State.JUMP_UP; stateTimer = 30;
            } else if (splashCD <= 0 && dist < 600 && Math.random() < 0.08) { 
                currentState = State.NAIL_SPLASH_WINDUP; stateTimer = 60; 
            }
        }

        else if (currentState == State.NAIL_SPLASH_WINDUP) {
            stateTimer--;
            if (stateTimer > 30) { leftArmAngle = -1.0; rightArmAngle = 1.0; } 
            else { leftArmAngle = -0.2; rightArmAngle = 0.2; }
            if (stateTimer <= 0) { currentState = State.NAIL_SPLASH_FIRE; stateTimer = 20; } 
        }
        else if (currentState == State.NAIL_SPLASH_FIRE) {
            stateTimer--; leftArmAngle = -0.1; rightArmAngle = 0.1; 
            if (stateTimer % 2 == 0) fireNailFrom(x, y, aimAngle, 2);
            if (stateTimer <= 0) { splashCD = 90; currentState = State.KITE; }
        }

        else if (currentState == State.IMPALE_WINDUP) {
            stateTimer--; double progress = 1.0 - (stateTimer / 45.0);
            rightArmAngle = 0.5 + (1.2 * progress); rightArmDist = 45 - (15 * progress); showGiantNail = true;                   
            if (stateTimer <= 0) { currentState = State.IMPALE_FIRE; stateTimer = 30; } 
        }
        else if (currentState == State.IMPALE_FIRE) {
            stateTimer--; rightArmAngle = -0.4; rightArmDist = 85; 
            x += Math.cos(aimAngle) * 6; y += Math.sin(aimAngle) * 6; 
            if (Math.hypot(target.x - x, target.y - y) < radius + target.radius + 35) {
                target.hp -= 15; 
                GamePanel.dmgTexts.add(new DamageText(target.x, target.y - 20, 15, DamageText.TextType.TAKEN));
                target.x += Math.cos(aimAngle) * 40; target.y += Math.sin(aimAngle) * 40; 
                
                if (target.currentHost != null && target.currentHost.getClass().getSimpleName().equals("Invader")) {
                    try {
                        Object inv = target.currentHost;
                        inv.getClass().getField("slashCD").setInt(inv, 180);
                        inv.getClass().getField("spinCD").setInt(inv, Math.max(180, inv.getClass().getField("spinCD").getInt(inv)));
                        inv.getClass().getField("dashCD").setInt(inv, Math.max(180, inv.getClass().getField("dashCD").getInt(inv)));
                        inv.getClass().getField("empowerCD").setInt(inv, Math.max(180, inv.getClass().getField("empowerCD").getInt(inv)));
                        Field f = inv.getClass().getDeclaredField("moveStunTimer"); f.setAccessible(true); f.set(inv, 180);
                    } catch (Exception e) {}
                }
                currentState = State.KITE; impaleCD = 240; showGiantNail = false;
            }
            if (stateTimer <= 0) { impaleCD = 240; currentState = State.KITE; showGiantNail = false; }
        }

        else if (currentState == State.JUMP_UP) {
            stateTimer--; z += 50; 
            if (stateTimer <= 0) {
                currentState = State.JUMP_HOVER; stateTimer = 30;
                x = 150 + Math.random() * 600; y = 150 + Math.random() * 600;
                double hpPercent = (double)hp / maxHp; int minionCount = 0;
                if (hpPercent <= 0.33) minionCount = 2; else if (hpPercent <= 0.66) minionCount = 1;
                for (int i = 0; i < minionCount; i++) {
                    double mx = 150 + Math.random() * 600; double my = 150 + Math.random() * 600;
                    minions.add(new BloxxerMinion(mx, my));
                }
            }
        }
        else if (currentState == State.JUMP_HOVER) {
            stateTimer--; if (stateTimer <= 0) { currentState = State.JUMP_DOWN; stateTimer = 60; }
        }
        else if (currentState == State.JUMP_DOWN) {
            stateTimer--; z = (stateTimer / 60.0) * 1500; 
            if (stateTimer <= 0) {
                z = 0; GamePanel.screenShakeTimer = 10;
                for (int i = 0; i < 30; i++) GamePanel.particles.add(new Particle(x, y, new Color(255, 140, 0)));
                jumpCD = 480; currentState = State.KITE;
            }
        }

        else if (currentState == State.SUPER_SPLASH_WINDUP) {
            stateTimer--; double progress = 1.0 - (stateTimer / 120.0);
            leftArmAngle = -0.5 + (0.4 * progress); rightArmAngle = 0.5 - (0.4 * progress); 
            leftArmDist = 45 + (10 * progress); rightArmDist = 45 + (10 * progress);
            if (stateTimer <= 0) { currentState = State.SUPER_SPLASH_FIRE; stateTimer = 240; } 
        }
        else if (currentState == State.SUPER_SPLASH_FIRE) {
            stateTimer--; leftArmAngle = -0.1; rightArmAngle = 0.1; leftArmDist = 55; rightArmDist = 55;
            if (stateTimer % 4 == 0) {
                double offset = (Math.random() - 0.5) * 90; 
                double spawnX = x + Math.cos(aimAngle + Math.PI/2) * offset;
                double spawnY = y + Math.sin(aimAngle + Math.PI/2) * offset;
                fireNailFrom(spawnX, spawnY, aimAngle, 2);
            }
            if (stateTimer <= 0) { superSplashCD = 720; currentState = State.KITE; }
        }
    }

    private void fireNailFrom(double sx, double sy, double baseAngle, int damage) {
        double scatter = (Math.random() * 0.2) - 0.1; 
        double finalAngle = baseAngle + scatter;
        double tx = sx + Math.cos(finalAngle) * 600;
        double ty = sy + Math.sin(finalAngle) * 600;
        OrangeNail nail = new OrangeNail(sx, sy, tx, ty, finalAngle);
        nail.damage = damage;
        GamePanel.projectiles.add(nail);
    }

    private void triggerMassiveLandingEffect(double lx, double ly) {
        GamePanel.screenShakeTimer = 30;
        rings.add(new RingWave(lx, ly, 0, true));
        rings.add(new RingWave(lx, ly, 15, true)); 
        rings.add(new RingWave(lx, ly, 30, true)); 
        for (int i = 0; i < 80; i++) GamePanel.particles.add(new Particle(lx, ly, new Color(255, 140, 0))); 
        
        double dist = Math.hypot(target.x - lx, target.y - ly);
        if (dist < 400) { 
            target.hp -= 40;
            GamePanel.dmgTexts.add(new DamageText(target.x, target.y - 20, 40, DamageText.TextType.TAKEN));
            
            double pushAngle = Math.atan2(target.y - ly, target.x - lx);
            double pushForce = 85 * (1.0 - (dist / 400.0)); 
            
            playerKbTimer = 20; 
            playerKbVx = Math.cos(pushAngle) * (pushForce / 20.0);
            playerKbVy = Math.sin(pushAngle) * (pushForce / 20.0);
            
            if (target.currentHost != null && target.currentHost.getClass().getSimpleName().equals("Invader")) {
                try {
                    Object inv = target.currentHost;
                    Field f = inv.getClass().getDeclaredField("moveStunTimer"); 
                    f.setAccessible(true); 
                    f.set(inv, 60);
                } catch (Exception e) {}
            }
        }
    }

    private double[] getRandomPointOnEdge(int edge) {
        if (edge == 0) return new double[]{Math.random()*900, 0};       
        if (edge == 1) return new double[]{900, Math.random()*900};     
        if (edge == 2) return new double[]{Math.random()*900, 900};     
        return new double[]{0, Math.random()*900};                      
    }

    private void generateTripwires(int count) {
        tripwires.clear();
        for(int i = 0; i < count; i++) {
            int edge1 = (int)(Math.random() * 4);
            int edge2 = (edge1 + 1 + (int)(Math.random() * 3)) % 4; 
            double[] p1 = getRandomPointOnEdge(edge1);
            double[] p2 = getRandomPointOnEdge(edge2);
            tripwires.add(new Tripwire(p1[0], p1[1], p2[0], p2[1]));
        }
    }

    private void updateTripwires() {
        for (Tripwire t : tripwires) {
            if (t.cooldownTimer > 0) { t.cooldownTimer--; continue; }

            if (!t.triggered) {
                double l2 = Math.pow(t.x1 - t.x2, 2) + Math.pow(t.y1 - t.y2, 2);
                double t_param = Math.max(0, Math.min(1, ((target.x - t.x1)*(t.x2 - t.x1) + (target.y - t.y1)*(t.y2 - t.y1)) / l2));
                double projX = t.x1 + t_param * (t.x2 - t.x1); double projY = t.y1 + t_param * (t.y2 - t.y1);
                if (Math.hypot(target.x - projX, target.y - projY) < target.radius) {
                    t.triggered = true; t.timer = 30; 
                }
            } else {
                t.timer--;
                if (t.timer <= 0) {
                    double a1 = Math.atan2(target.y - t.y1, target.x - t.x1);
                    double a2 = Math.atan2(target.y - t.y2, target.x - t.x2);
                    for(int i = 0; i < 5; i++) {
                        fireNailFrom(t.x1, t.y1, a1, 3);
                        fireNailFrom(t.x2, t.y2, a2, 3);
                    }
                    t.triggered = false; t.cooldownTimer = 300; 
                }
            }
        }
    }

    private void updateRings() {
        Iterator<RingWave> it = rings.iterator();
        while (it.hasNext()) {
            RingWave r = it.next();
            if (r.delay > 0) r.delay--;
            else { r.radius += r.massive ? 15 : 8; r.alpha = Math.max(0, r.alpha - (r.massive ? 4 : 8)); }
            if (r.alpha <= 0) it.remove();
        }
    }

    private void updateMinions() {
        Iterator<BloxxerMinion> it = minions.iterator();
        while (it.hasNext()) {
            BloxxerMinion m = it.next(); m.update();
            if (m.isDead) it.remove();
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        for (Tripwire t : tripwires) {
            if (t.cooldownTimer > 0) {
                g2d.setColor(new Color(150, 150, 150, 60)); g2d.setStroke(new BasicStroke(1));
            } else {
                g2d.setColor(t.triggered ? new Color(255, 100, 0, 200) : new Color(255, 140, 0, 80));
                g2d.setStroke(new BasicStroke(t.triggered ? 6 : 2)); 
            }
            g2d.drawLine((int)t.x1, (int)t.y1, (int)t.x2, (int)t.y2);
        }
        g2d.setStroke(new BasicStroke(1));

        if (z > 0) {
            double shadowRatio = 1.0 - (z / 1500.0); if (shadowRatio < 0) shadowRatio = 0;
            int shadowSize = (int)(radius * 1.5 + (radius * 1.5 * shadowRatio)); 
            g2d.setColor(new Color(0, 0, 0, (int)(120 * shadowRatio))); g2d.fillOval((int)x - shadowSize/2, (int)y - shadowSize/2, shadowSize, shadowSize);
        }
        for (BloxxerMinion m : minions) {
            if (m.z > 0) {
                double sRatio = 1.0 - (m.z / 1500.0); if (sRatio < 0) sRatio = 0;
                int sSize = (int)(radius * 1.5 + (radius * 1.5 * sRatio));
                g2d.setColor(new Color(0, 0, 0, (int)(120 * sRatio))); g2d.fillOval((int)m.x - sSize/2, (int)m.y - sSize/2, sSize, sSize);
            }
        }

        for (RingWave r : rings) {
            if (r.delay <= 0) {
                g2d.setColor(new Color(255, 140, 0, r.alpha)); g2d.setStroke(new BasicStroke(r.massive ? 12 : 4)); 
                g2d.drawOval((int)(r.x - r.radius), (int)(r.y - r.radius), (int)r.radius*2, (int)r.radius*2);
            }
        }
        g2d.setStroke(new BasicStroke(1));

        for (BloxxerMinion m : minions) m.draw(g2d);

        AffineTransform oldAT = g2d.getTransform();
        double drawX = x; double drawY = y;
        if (currentState == State.DEATH_DEFIANCE) {
            drawX += Math.random() * 6 - 3; drawY += Math.random() * 6 - 3;
        }
        g2d.translate(drawX, drawY - z); g2d.rotate(aimAngle);

        if (currentState == State.DEATH_DEFIANCE || currentState == State.PHASE2_ENDLESS_SPLASH) {
            g2d.setColor(new Color(255, 200, 0, 100)); g2d.fillOval(-45, -45, 90, 90);
        }

        drawMooMooBody(g2d, false); 
        g2d.setTransform(oldAT);
    }

    private void drawMooMooBody(Graphics2D g2d, boolean isSilhouette) {
        Color skinColor = isSilhouette ? new Color(255, 140, 0, 200) : new Color(170, 170, 170);
        Color hatColor = isSilhouette ? new Color(255, 140, 0, 200) : new Color(255, 140, 0);
        Color sunglassColor = isSilhouette ? new Color(200, 100, 0, 200) : Color.BLACK;
        Color weaponColor = isSilhouette ? new Color(255, 140, 0, 200) : new Color(255, 140, 0);

        AffineTransform armL = g2d.getTransform();
        g2d.translate(Math.cos(leftArmAngle - Math.PI/2) * leftArmDist, Math.sin(leftArmAngle - Math.PI/2) * leftArmDist);
        g2d.rotate(leftArmAngle * 0.4); 
        g2d.setColor(weaponColor); g2d.fillRect(0, -6, 35, 12); g2d.setColor(Color.BLACK); g2d.drawRect(0, -6, 35, 12); 
        g2d.setColor(isSilhouette ? weaponColor : Color.GRAY); g2d.fillRect(25, -16, 16, 32); g2d.setColor(Color.BLACK); g2d.drawRect(25, -16, 16, 32);
        g2d.setColor(skinColor); g2d.fillOval(-12, -12, 24, 24); g2d.setColor(Color.BLACK); g2d.drawOval(-12, -12, 24, 24);
        g2d.setTransform(armL);

        AffineTransform armR = g2d.getTransform();
        g2d.translate(Math.cos(rightArmAngle + Math.PI/2) * rightArmDist, Math.sin(rightArmAngle + Math.PI/2) * rightArmDist);
        g2d.rotate(rightArmAngle * 0.4); 
        if (showGiantNail && !isSilhouette) {
            g2d.setColor(weaponColor); int[] nx = {0, 150, 150, 0}; int[] ny = {-10, -4, 4, 10};
            g2d.fillPolygon(nx, ny, 4); g2d.fillOval(-15, -16, 30, 32); 
            g2d.setColor(Color.BLACK); g2d.drawPolygon(nx, ny, 4); g2d.drawOval(-15, -16, 30, 32);
        } else {
            g2d.setColor(weaponColor); int[] nx = {0, 35, 35, 0}; int[] ny = {-4, -1, 1, 4};
            g2d.fillPolygon(nx, ny, 4); g2d.fillOval(-6, -8, 12, 16);
            g2d.setColor(Color.BLACK); g2d.drawPolygon(nx, ny, 4); g2d.drawOval(-6, -8, 12, 16);
        }
        g2d.setColor(skinColor); g2d.fillOval(-12, -12, 24, 24); g2d.setColor(Color.BLACK); g2d.drawOval(-12, -12, 24, 24);
        g2d.setTransform(armR);

        g2d.setColor(skinColor); g2d.fillOval(-32, -32, 64, 64);
        g2d.setColor(Color.BLACK); g2d.drawOval(-32, -32, 64, 64);

        g2d.setColor(sunglassColor); g2d.fillRoundRect(14, -20, 10, 40, 6, 6);

        g2d.setColor(hatColor); g2d.fillRect(-12, -32, 12, 64); g2d.fillArc(-34, -28, 44, 56, 90, 180); 
        g2d.setColor(Color.BLACK); g2d.fillRect(-38, -6, 8, 12); g2d.drawRect(-12, -32, 12, 64); g2d.drawArc(-34, -28, 44, 56, 90, 180);
    }

    public static class OrangeNail extends Projectile {
        double angle;
        double speed = 6.8; 
        double vx, vy;

        public OrangeNail(double x, double y, double tx, double ty, double angle) { 
            super(x, y, tx, ty); 
            this.angle = angle; 
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
        }

        @Override 
        public void update() {
            x += vx; y += vy;
            if (x < -100 || x > 1000 || y < -100 || y > 1000) { active = false; }
        }

        @Override 
        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform(); g2d.translate(x, y); g2d.rotate(angle);
            g2d.setColor(new Color(255, 140, 0)); int[] nx = {-10, 15, 15, -10}; int[] ny = {-3, -1, 1, 3}; 
            g2d.fillPolygon(nx, ny, 4); g2d.fillOval(-14, -6, 8, 12); 
            g2d.setColor(Color.BLACK); g2d.drawPolygon(nx, ny, 4); g2d.drawOval(-14, -6, 8, 12); g2d.setTransform(old);
        }
    }

    class RingWave {
        double x, y; float radius = 0; int alpha = 255; int delay; boolean massive;
        RingWave(double x, double y, int delay, boolean massive) { this.x = x; this.y = y; this.delay = delay; this.massive = massive; }
    }

    class Tripwire {
        double x1, y1, x2, y2; boolean triggered = false; int timer = 0; int cooldownTimer = 0; boolean isDead = false;
        Tripwire(double x1, double y1, double x2, double y2) { this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; }
    }

    class BloxxerMinion {
        double x, y, z = 1500; double mAngle = 0; int state = 0; int timer = 60; boolean isDead = false;
        BloxxerMinion(double x, double y) { this.x = x; this.y = y; }

        void update() {
            if (target.currentHost != null && target.currentHost.getClass().getSimpleName().equals("Invader")) {
                try {
                    Object inv = target.currentHost;
                    if (inv.getClass().getField("isSlashing").getBoolean(inv) || inv.getClass().getField("isSpinning").getBoolean(inv) || inv.getClass().getField("isDashing").getBoolean(inv)) {
                        double hitRange = (inv.getClass().getField("isSpinning").getBoolean(inv) || inv.getClass().getField("isDashing").getBoolean(inv)) ? 95 : 75; 
                        if (Math.hypot(target.x - x, target.y - y) < hitRange) {
                            isDead = true; GamePanel.dmgTexts.add(new DamageText(x, y - 20, 1, DamageText.TextType.TAKEN));
                            for(int i=0; i<20; i++) GamePanel.particles.add(new Particle(x, y, new Color(255, 140, 0))); return;
                        }
                    }
                } catch (Exception e) {}
            }

            if (state == 0) { 
                timer--; z = (timer / 60.0) * 1500;
                if (timer <= 0) { 
                    z = 0; GamePanel.particles.add(new Particle(x, y, new Color(255, 140, 0))); 
                    state = 1; timer = 60 + (int)(Math.random() * 180); 
                } 
            } 
            else if (state == 1) { 
                timer--; mAngle = Math.atan2(target.y - y, target.x - x); x += Math.cos(mAngle + Math.PI/2) * 1.5; y += Math.sin(mAngle + Math.PI/2) * 1.5;
                if (timer <= 0) { state = 2; timer = 60; } 
            }
            else if (state == 2) { 
                timer--; mAngle = Math.atan2(target.y - y, target.x - x); if (timer <= 0) { state = 3; timer = 24; }
            }
            else if (state == 3) { 
                // ★ 修正：小兵發射時使用正確的 x, y 座標，之前錯用了迴圈變數導致編譯錯誤
                timer--; if (timer % 2 == 0) fireNailFrom(x, y, mAngle, 3); 
                if (timer <= 0) { state = 4; isDead = true; for(int i=0; i<20; i++) GamePanel.particles.add(new Particle(x, y, new Color(255, 140, 0))); } 
            }
        }

        void draw(Graphics2D g2d) {
            if (z > 0) {
                double sRatio = 1.0 - (z / 1500.0); if (sRatio < 0) sRatio = 0;
                // ★ 修正：小兵影子繪製使用正確的 x, y 座標
                int sSize = (int)(radius * 1.5 + (radius * 1.5 * sRatio));
                g2d.setColor(new Color(0, 0, 0, (int)(120 * sRatio))); 
                g2d.fillOval((int)x - sSize/2, (int)y - sSize/2, sSize, sSize);
            }
            AffineTransform oldAT = g2d.getTransform(); g2d.translate(x, y - z); g2d.rotate(mAngle); drawMooMooBody(g2d, true); g2d.setTransform(oldAT);
        }
    }
}