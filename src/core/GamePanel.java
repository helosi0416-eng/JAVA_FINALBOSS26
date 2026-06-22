package core;

import entity.Dummy;
import entity.Equinox;
import entity.Bloxxerman; 
import entity.Entity;     
import entity.Player;
import entity.Projectile;
import host.Invader;
import map.ArenaMap;
import map.GameMap;
import tempo.Halt;
import tempo.Undo;
import tempo.Zero;
import tempo.Zion;
import ui.PauseMenu;
import ui.StartMenu;
import vfx.DamageText;
import vfx.Particle;
import vfx.TrailFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;

public class GamePanel extends JPanel implements Runnable, MouseListener, MouseMotionListener, KeyListener {

    public enum GameState { MENU, PLAYING, PAUSED }
    public GameState currentState = GameState.MENU;

    public enum Stage { TRAINING_GROUND, PEACOCK_PEAK, MIRROR_OF_FUTURE }
    public Stage currentStage = Stage.TRAINING_GROUND;

    Thread gameThread;
    final int FPS = 60;
    public static boolean isTimeStopped = false; 

    public String sceneTitle = "";
    public int sceneTitleTimer = 0;
    public static int screenShakeTimer = 0;

    public static double difficultyMultiplier = 1.0; 

    public static int padDamageCD = 0;
    public static int padHealCD = 0;
    public static int padKillCD = 0;
    public static int dummyRespawnTimer = 0;

    public static ArrayList<Projectile> projectiles = new ArrayList<>();
    public static ArrayList<Dummy> dummies = new ArrayList<>();
    public static ArrayList<Particle> particles = new ArrayList<>();
    public static ArrayList<TrailFrame> trails = new ArrayList<>();
    public static ArrayList<DamageText> dmgTexts = new ArrayList<>();
    public static ArrayList<RainDrop> rainDrops = new ArrayList<>();

    public static Entity boss = null; 

    public static Sound bgmEquinox = new Sound("assets/EquinoxBossTheme.wav");
    public static Sound bgmBloxxerman = new Sound("assets/BloxxermanBossTheme.wav");
    // ★ 預設音量從 80 調降為 35，保護耳朵
    public static int globalVolume = 35; 

    StartMenu startMenu;
    PauseMenu pauseMenu; 
    GameMap currentMap;
    Player player;

    Image imgSlash, imgSpin, imgDash, imgEmpower;
    final Color NEON_GREEN = new Color(57, 255, 20);

    public GamePanel() {
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);

        imgSlash = new ImageIcon("assets/InvaderSlash.png").getImage();
        imgSpin = new ImageIcon("assets/InvaderSpinSlash.png").getImage();
        imgDash = new ImageIcon("assets/InvaderDashSlash.png").getImage();
        imgEmpower = new ImageIcon("assets/InvaderEmpower.png").getImage();

        initGame();
        startGameThread();
    }

    public void initGame() {
        startMenu = new StartMenu();
        pauseMenu = new PauseMenu(); 
        currentMap = new ArenaMap(); 
        applyVolume(); 
    }

    public static void applyVolume() {
        bgmEquinox.setVolumePercentage(globalVolume);
        bgmBloxxerman.setVolumePercentage(globalVolume);
    }

    public void pauseBGM() {
        if (currentStage == Stage.PEACOCK_PEAK) bgmEquinox.pause();
        if (currentStage == Stage.MIRROR_OF_FUTURE) bgmBloxxerman.pause();
    }

    public void resumeBGM() {
        if (currentStage == Stage.PEACOCK_PEAK && !player.isDead) bgmEquinox.resumeLoop();
        if (currentStage == Stage.MIRROR_OF_FUTURE && !player.isDead) bgmBloxxerman.resumeLoop();
    }

    public void stopAllBGM() {
        bgmEquinox.stop();
        bgmBloxxerman.stop();
    }

    public void startGame(int hostIndex, int tempoIndex) {
        player = new Player(450, 450); 
        if (hostIndex == 0) player.setHost(new Invader(player));
        else player.setHost(new Invader(player));

        if (tempoIndex == 0) player.setTempo(new Halt(player));
        else if (tempoIndex == 1) player.setTempo(new Undo(player));
        else if (tempoIndex == 2) player.setTempo(new Zero(player));
        else if (tempoIndex == 3) player.setTempo(new Zion(player));
        else player.setTempo(new Halt(player)); 

        stopAllBGM();

        currentStage = Stage.TRAINING_GROUND; boss = null;
        dummies.clear(); spawnDummies(); 

        projectiles.clear(); particles.clear(); trails.clear(); dmgTexts.clear(); rainDrops.clear();
        isTimeStopped = false; 
        padDamageCD = 0; padHealCD = 0; padKillCD = 0; dummyRespawnTimer = 0; screenShakeTimer = 0;
        
        sceneTitle = "假人訓練場"; sceneTitleTimer = 240;
        currentState = GameState.PLAYING;
    }

    private void spawnDummies() {
        dummies.add(new Dummy(300, 300, Dummy.DummyType.IDLE, player));
        dummies.add(new Dummy(600, 300, Dummy.DummyType.ROAM, player));
        dummies.add(new Dummy(300, 600, Dummy.DummyType.MELEE, player));
        dummies.add(new Dummy(600, 600, Dummy.DummyType.RANGED, player));
    }

    public void startGameThread() { gameThread = new Thread(this); gameThread.start(); }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS; double nextDrawTime = System.nanoTime() + drawInterval;
        while (gameThread != null) {
            update(); repaint();
            try {
                double remainingTime = nextDrawTime - System.nanoTime(); remainingTime = remainingTime / 1000000;
                if (remainingTime < 0) remainingTime = 0; Thread.sleep((long) remainingTime); nextDrawTime += drawInterval;
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void update() {
        if (currentState == GameState.MENU || currentState == GameState.PAUSED) return; 

        if (currentState == GameState.PLAYING && player != null) {
            
            // ★ 核心修復：無條件呼叫音樂 update()，不管 Boss 死沒死，都能正確計算每一幀的淡出音量。
            bgmEquinox.update();
            bgmBloxxerman.update();

            if (player.hp > 0 && player.isDead) player.isDead = false;
            if (!isTimeStopped && sceneTitleTimer > 0) sceneTitleTimer--;

            if (!isTimeStopped && screenShakeTimer > 0) screenShakeTimer--;

            if (currentStage == Stage.TRAINING_GROUND && !player.isDead) {
                if (player.y - player.radius <= 25 && player.x >= 390 && player.x <= 510) {
                    currentStage = Stage.PEACOCK_PEAK;
                    dummies.clear(); projectiles.clear(); dmgTexts.clear(); trails.clear(); particles.clear();
                    player.x = 450; player.y = 820;
                    sceneTitle = "孔雀山峰"; sceneTitleTimer = 240;

                    if (player.currentTempo != null) {
                        player.currentTempo.duration = 0; player.currentTempo.cooldown = player.currentTempo.MAX_CD; 
                    }
                    GamePanel.isTimeStopped = false;
                    boss = new Equinox(450, 150, player);
                    applyVolume();
                    bgmEquinox.loop();
                }
            }
            
            if (currentStage == Stage.PEACOCK_PEAK && boss == null && !player.isDead) {
                if (player.y - player.radius <= 25 && player.x >= 390 && player.x <= 510) {
                    currentStage = Stage.MIRROR_OF_FUTURE;
                    dummies.clear(); projectiles.clear(); dmgTexts.clear(); trails.clear(); particles.clear(); rainDrops.clear();
                    
                    player.x = 450; player.y = 820;
                    sceneTitle = "未來之鏡"; sceneTitleTimer = 240;

                    if (player.currentTempo != null) {
                        player.currentTempo.duration = 0; player.currentTempo.cooldown = player.currentTempo.MAX_CD; 
                    }
                    GamePanel.isTimeStopped = false;
                    boss = new Bloxxerman(450, 200, player); 
                    bgmEquinox.stop(); 
                    
                    applyVolume();
                    bgmBloxxerman.loop();
                }
            }

            if (currentStage == Stage.TRAINING_GROUND) {
                if (padDamageCD > 0) padDamageCD--; if (padHealCD > 0) padHealCD--; if (padKillCD > 0) padKillCD--;
                if (!player.isDead) {
                    if (padDamageCD == 0 && Math.hypot(player.x - 250, player.y - 150) < player.radius + 30) {
                        player.hp -= 50; dmgTexts.add(new DamageText(player.x, player.y - 20, 50, DamageText.TextType.TAKEN)); padDamageCD = 30; 
                    }
                    if (padHealCD == 0 && Math.hypot(player.x - 450, player.y - 150) < player.radius + 30) {
                        player.hp = Math.min(player.maxHp, player.hp + 20); dmgTexts.add(new DamageText(player.x, player.y - 35, 20, DamageText.TextType.HEAL)); padHealCD = 30;
                    }
                    if (padKillCD == 0 && Math.hypot(player.x - 650, player.y - 150) < player.radius + 30) {
                        for (Dummy d : dummies) { for(int i = 0; i < 30; i++) particles.add(new Particle(d.x, d.y, Color.RED)); }
                        dummies.clear(); dummyRespawnTimer = 180; padKillCD = 180; 
                    }
                }
                if (dummyRespawnTimer > 0) { dummyRespawnTimer--; if (dummyRespawnTimer == 0) spawnDummies(); }
            }

            if (player.hp <= 0 && !player.isDead) {
                for(int i = 0; i < 40; i++) particles.add(new Particle(player.x, player.y, NEON_GREEN));
                player.isDead = true; player.deathTimer = 180; 
                stopAllBGM(); 
            }

            if (player.isDead) {
                player.deathTimer--;
                if (player.deathTimer <= 0) { currentState = GameState.MENU; return; }
            }

            player.update(); 

            if (!isTimeStopped) {
                if (currentStage == Stage.MIRROR_OF_FUTURE) {
                    int spawnCount = (int)(3 * player.timeMultiplier); 
                    for (int i = 0; i < spawnCount; i++) {
                        rainDrops.add(new RainDrop());
                    }
                    
                    Iterator<RainDrop> rIt = rainDrops.iterator();
                    while (rIt.hasNext()) {
                        RainDrop r = rIt.next();
                        if (r.update(player.timeMultiplier)) {
                            particles.add(new Particle(r.x, r.y, new Color(150, 200, 255)));
                            rIt.remove();
                        } else if (r.y > 1000 || r.x > 1000) {
                            rIt.remove(); 
                        }
                    }
                }

                if (boss != null) {
                    boss.update();
                    if (boss.hp <= 0) {
                        for(int i = 0; i < 100; i++) particles.add(new Particle(boss.x, boss.y, Color.WHITE));
                        
                        // ★ 改成傳入「花費 120 幀(2秒) 來淡出」，這樣就算聲音大也能平滑消音！
                        if (boss instanceof Equinox) bgmEquinox.fadeOut(300);
                        if (boss instanceof Bloxxerman) bgmBloxxerman.fadeOut(300);
                        
                        boss = null; 
                    }
                }

                if (currentStage == Stage.TRAINING_GROUND) {
                    Iterator<Dummy> dIt = dummies.iterator();
                    while (dIt.hasNext()) {
                        Dummy d = dIt.next(); d.update();
                        if (d.hp <= 0) {
                            for(int i = 0; i < 30; i++) particles.add(new Particle(d.x, d.y, Color.RED));
                            dIt.remove();
                        }
                    }
                }

                Iterator<Projectile> projIt = projectiles.iterator();
                while (projIt.hasNext()) {
                    Projectile proj = projIt.next(); proj.update();
                    double dist = Math.hypot(player.x - proj.x, player.y - proj.y);
                    if (!player.isDead && dist < player.radius + 5) {
                        int scaledDmg = (int)(proj.damage * difficultyMultiplier);
                        int finalDmg = player.glowingTimer > 0 ? scaledDmg * 2 : scaledDmg;
                        player.hp -= finalDmg;
                        dmgTexts.add(new DamageText(player.x, player.y - 20, finalDmg, DamageText.TextType.TAKEN));
                        
                        if (boss != null && boss instanceof Equinox) {
                            if (((Equinox)boss).currentState == Equinox.State.ULT_LIGHT_LEAP) {
                                player.glowingTimer = 600; 
                            }
                        }
                        proj.active = false;
                    }
                    if (!proj.active) projIt.remove();
                }

                Iterator<DamageText> textIt = dmgTexts.iterator();
                while (textIt.hasNext()) {
                    DamageText d = textIt.next(); d.update(); if (d.life <= 0) textIt.remove();
                }
            }

            Iterator<Particle> pIt = particles.iterator();
            while (pIt.hasNext()) { Particle p = pIt.next(); p.update(); if (p.life <= 0) pIt.remove(); }

            Iterator<TrailFrame> tIt = trails.iterator();
            while (tIt.hasNext()) { TrailFrame t = tIt.next(); t.update(); if (t.life <= 0) tIt.remove(); }
        }
    }

    private int getOffsetX() { return (getWidth() - 900) / 2; }
    private int getOffsetY() { return (getHeight() - 900) / 2; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); Graphics2D g2d = (Graphics2D) g; g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(30, 35, 40)); g2d.fillRect(0, 0, getWidth(), getHeight());

        int ox = getOffsetX(); int oy = getOffsetY();
        boolean isUndoEffect = false;
        if (player != null && player.currentTempo instanceof Undo) {
            if (((Undo) player.currentTempo).effectTimer > 0) isUndoEffect = true;
        }
        
        if (screenShakeTimer > 0 || isUndoEffect) {
            ox += (int)(Math.random() * 24 - 12); oy += (int)(Math.random() * 24 - 12);
        }
        g2d.translate(ox, oy);

        if (currentState == GameState.MENU) {
            startMenu.draw(g2d); 
        } else if ((currentState == GameState.PLAYING || currentState == GameState.PAUSED) && player != null) {
            
            if (player.currentTempo instanceof Zion) {
                Zion zion = (Zion) player.currentTempo;
                if (zion.fadeAlpha > 0) { 
                    double cycle = Math.sin(zion.skyAngle); 
                    int alpha = (int)(Math.abs(cycle) * 75 * zion.fadeAlpha); 
                    int baseNightAlpha = (int)(30 * zion.fadeAlpha);
                    if (cycle > 0) g2d.setColor(new Color(255, 120, 40, alpha)); 
                    else g2d.setColor(new Color(0, 0, 80, alpha + baseNightAlpha)); 
                    g2d.fillRect(-1000, -1000, 3000, 3000);

                    for (int i = 0; i < zion.stars.length; i++) {
                        double sx = zion.stars[i][0]; double sy = zion.stars[i][1]; double phase = zion.stars[i][2];
                        int starAlpha = (int)((Math.sin(phase) * 0.5 + 0.5) * 255 * zion.fadeAlpha);
                        g2d.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, starAlpha))));
                        int size = (i % 3 == 0) ? 4 : 2; g2d.fillRect((int)sx, (int)sy, size, size);
                    }
                    
                    double sunX = 450 + Math.cos(zion.skyAngle) * 600; double sunY = 450 + Math.sin(zion.skyAngle) * 600;
                    double moonX = 450 + Math.cos(zion.skyAngle + Math.PI) * 600; double moonY = 450 + Math.sin(zion.skyAngle + Math.PI) * 600;

                    g2d.setColor(new Color(255, 255, 200, (int)(220 * zion.fadeAlpha))); g2d.fillOval((int)sunX - 100, (int)sunY - 100, 200, 200);
                    g2d.setColor(new Color(255, 200, 50, (int)(100 * zion.fadeAlpha))); g2d.fillOval((int)sunX - 160, (int)sunY - 160, 320, 320);

                    g2d.setColor(new Color(180, 220, 255, (int)(220 * zion.fadeAlpha))); g2d.fillOval((int)moonX - 70, (int)moonY - 70, 140, 140);
                    g2d.setColor(new Color(100, 150, 255, (int)(80 * zion.fadeAlpha))); g2d.fillOval((int)moonX - 110, (int)moonY - 110, 220, 220);
                }
            }

            currentMap.draw(g2d); 

            if (currentStage == Stage.MIRROR_OF_FUTURE) {
                g2d.setColor(new Color(15, 25, 45, 120)); 
                g2d.fillRect(-1000, -1000, 3000, 3000);
            }

            if (currentStage == Stage.TRAINING_GROUND) {
                g2d.setColor(new Color(150, 0, 0, 100)); if (padDamageCD > 0) g2d.setColor(new Color(100, 100, 100, 100));
                g2d.fillOval(250 - 30, 150 - 30, 60, 60); g2d.setColor(Color.RED); g2d.drawOval(250 - 30, 150 - 30, 60, 60);
                g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 12)); g2d.drawString("-50 HP", 250 - 18, 150 + 5);

                g2d.setColor(new Color(0, 150, 0, 100)); if (padHealCD > 0) g2d.setColor(new Color(100, 100, 100, 100));
                g2d.fillOval(450 - 30, 150 - 30, 60, 60); g2d.setColor(Color.GREEN); g2d.drawOval(450 - 30, 150 - 30, 60, 60);
                g2d.setColor(Color.WHITE); g2d.drawString("+20 HP", 450 - 18, 150 + 5);

                g2d.setColor(new Color(100, 0, 150, 100)); if (padKillCD > 0) g2d.setColor(new Color(100, 100, 100, 100));
                g2d.fillOval(650 - 30, 150 - 30, 60, 60); g2d.setColor(Color.MAGENTA); g2d.drawOval(650 - 30, 150 - 30, 60, 60);
                g2d.setColor(Color.WHITE); g2d.drawString("CLEAR", 650 - 18, 150 + 5);

                g2d.setColor(new Color(255, 255, 220)); g2d.fillRect(390, 0, 120, 20);
                g2d.setColor(new Color(0, 255, 255, 180)); g2d.setStroke(new BasicStroke(4)); g2d.drawRect(390, 0, 120, 20);
            }

            if (currentStage == Stage.PEACOCK_PEAK && boss == null) {
                g2d.setColor(new Color(255, 255, 255, 220)); g2d.fillRect(390, 0, 120, 20);
                g2d.setColor(new Color(150, 0, 255, 180)); g2d.setStroke(new BasicStroke(4)); g2d.drawRect(390, 0, 120, 20);
            }

            if (boss != null && boss instanceof Equinox) {
                Equinox eq = (Equinox) boss;
                if (eq.currentState == Equinox.State.ULT_DARK_TORNADO) {
                    int radius = 400; 
                    g2d.setColor(new Color(20, 0, 40, 75)); 
                    g2d.fillOval(450 - radius, 450 - radius, radius * 2, radius * 2);
                    g2d.setColor(new Color(130, 0, 220, 130));
                    g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{20, 20}, eq.stateTimer));
                    g2d.drawOval(450 - radius, 450 - radius, radius * 2, radius * 2);
                    g2d.setStroke(new BasicStroke(1));
                }
            }

            for (Projectile proj : projectiles) proj.draw(g2d);
            for (Dummy dummy : dummies) dummy.draw(g2d);
            if (boss != null) boss.draw(g2d);
            for (Particle p : particles) p.draw(g2d);
            for (TrailFrame t : trails) t.draw(g2d);

            if (!player.isDead) player.draw(g2d); 

            if (currentStage == Stage.MIRROR_OF_FUTURE) {
                for (RainDrop r : rainDrops) r.draw(g2d);
            }

            if (player.currentTempo instanceof Zero && player.currentTempo.duration > 0) {
                g2d.setXORMode(Color.WHITE); g2d.fillRect(-2000, -2000, 5000, 5000); g2d.setPaintMode(); 
            }

            if (!player.isDead && player.glowingTimer > 0) {
                g2d.setColor(new Color(255, 255, 200, 35)); g2d.fillRect(0, 0, 900, 900);
            }

            for (DamageText d : dmgTexts) d.draw(g2d);

            if (isTimeStopped) {
                g2d.setColor(new Color(0, 0, 50, 40)); g2d.fillRect(0, 0, 900, 900);
            }
            if (isUndoEffect) {
                g2d.setColor(Color.BLACK); g2d.fillRect(-1000, -1000, 3000, 3000); 
            }

            if (sceneTitleTimer > 0) {
                int alpha = 255; if (sceneTitleTimer < 60) alpha = (int)((sceneTitleTimer / 60.0) * 255);
                g2d.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alpha)))); g2d.setFont(new Font("Microsoft JhengHei", Font.BOLD, 90)); 
                FontMetrics titleFm = g2d.getFontMetrics(); g2d.drawString(sceneTitle, 450 - titleFm.stringWidth(sceneTitle) / 2, 450);
            }

            drawHUD(g2d); 
            
            if (boss != null) {
                int barW = 600; int barH = 20; int bX = 450 - barW / 2; int bY = -40; 
                g2d.setColor(new Color(50, 0, 0, 200)); g2d.fillRect(bX, bY, barW, barH);
                g2d.setColor(new Color(200, 30, 30)); int fillW = (int)((boss.hp / 1500.0) * barW); g2d.fillRect(bX, bY, Math.max(0, fillW), barH);
                g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(2)); g2d.drawRect(bX, bY, barW, barH);
                g2d.setFont(new Font("Arial", Font.BOLD, 18)); FontMetrics fm = g2d.getFontMetrics(); 
                
                String bName = "BOSS";
                if (boss instanceof Equinox) bName = "EQUINOX";
                else if (boss instanceof Bloxxerman) bName = "BLOXXERMAN";
                
                g2d.drawString(bName, 450 - fm.stringWidth(bName) / 2, bY - 8);
            }

            if (currentState == GameState.PLAYING && player.isDead) {
                g2d.setColor(new Color(255, 0, 0, 100)); g2d.fillRect(0, 0, 900, 900);
                FontMetrics fm = g2d.getFontMetrics(new Font("Arial", Font.BOLD, 50));
                if (player.currentTempo instanceof Undo) {
                    if (player.deathTimer % 30 > 10) { 
                        g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 50)); g2d.drawString("PRESS SPACE TO UNDO!", 450 - fm.stringWidth("PRESS SPACE TO UNDO!") / 2, 450);
                    }
                } else {
                    g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 50)); g2d.drawString("YOU DIED", 450 - fm.stringWidth("YOU DIED") / 2, 450);
                }
            }
            if (currentState == GameState.PAUSED) pauseMenu.draw(g2d, 900, 900);
        }
        g2d.dispose();
    }

    private void drawHUD(Graphics2D g2d) {
        int uiBoxSize = 60; int totalHeight = uiBoxSize * 4; int rightX = 900 + 15; int rightY = 900 - totalHeight; 
        int sCD = 0, spCD = 0, dCD = 0, eCD = 0, eDur = 0; boolean isEmp = false;
        if (player.currentHost instanceof Invader) {
            Invader inv = (Invader) player.currentHost; sCD = inv.slashCD; spCD = inv.spinCD; dCD = inv.dashCD; eCD = inv.empowerCD; eDur = inv.empowerDuration; isEmp = inv.isEmpowered;
        }
        drawSkillUI(g2d, rightX, rightY, imgSlash, "M1", sCD, Color.GRAY); drawSkillUI(g2d, rightX, rightY + uiBoxSize, imgSpin, "M2", spCD, Color.ORANGE);
        drawSkillUI(g2d, rightX, rightY + uiBoxSize*2, imgDash, "SHIFT", dCD, Color.CYAN); drawSkillUI(g2d, rightX, rightY + uiBoxSize*3, imgEmpower, "Q", eCD, NEON_GREEN);

        if (isEmp) {
            int qBoxY = rightY + uiBoxSize * 3; g2d.setColor(Color.WHITE); g2d.fillRect(rightX - 15, qBoxY, 10, uiBoxSize);
            g2d.setColor(NEON_GREEN); int barHeight = (int)((eDur / 120.0) * uiBoxSize); g2d.fillRect(rightX - 15, qBoxY + (uiBoxSize - barHeight), 10, barHeight);
        }
        int barWidth = 30; int hpX = -15 - barWidth; int tempoX = hpX - 10 - barWidth; int leftY = rightY; double tpRatio = 1.0; Color tpColor = new Color(0, 150, 255); String tpText = "TP";
        if (player.currentTempo != null) {
            if (player.currentTempo.duration > 0) { tpRatio = (double) player.currentTempo.duration / player.currentTempo.MAX_DURATION; tpColor = new Color(255, 215, 0); tpText = String.format("%.1f", player.currentTempo.duration / 60.0); }
            else if (player.currentTempo.cooldown > 0) { tpRatio = 1.0 - ((double) player.currentTempo.cooldown / player.currentTempo.MAX_CD); tpColor = new Color(0, 100, 180); tpText = String.format("%.1f", player.currentTempo.cooldown / 60.0); }
        }
        g2d.setColor(new Color(0, 0, 50, 180)); g2d.fillRect(tempoX, leftY, barWidth, totalHeight); g2d.setColor(tpColor); int tpFillHeight = (int) (tpRatio * totalHeight); g2d.fillRect(tempoX, leftY + (totalHeight - tpFillHeight), barWidth, tpFillHeight); g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(2)); g2d.drawRect(tempoX, leftY, barWidth, totalHeight); g2d.setFont(new Font("Arial", Font.BOLD, 12)); FontMetrics fm = g2d.getFontMetrics(); g2d.drawString("SPC", tempoX + (barWidth - fm.stringWidth("SPC")) / 2, leftY - 10); g2d.setFont(new Font("Arial", Font.BOLD, 14)); g2d.drawString(tpText, tempoX + (barWidth - fm.stringWidth(tpText)) / 2, leftY + totalHeight / 2);
        g2d.setColor(new Color(50, 0, 0, 180)); g2d.fillRect(hpX, leftY, barWidth, totalHeight); g2d.setColor(new Color(220, 20, 20)); int hpFillHeight = (int) ((Math.max(0, player.hp) / (double)player.maxHp) * totalHeight); g2d.fillRect(hpX, leftY + (totalHeight - hpFillHeight), barWidth, hpFillHeight); g2d.setColor(Color.WHITE); g2d.drawRect(hpX, leftY, barWidth, totalHeight); g2d.drawString("HP", hpX + 4, leftY - 10); String hpText = String.valueOf(Math.max(0, player.hp)); g2d.drawString(hpText, hpX + (barWidth - fm.stringWidth(hpText)) / 2, leftY + totalHeight / 2);
    }

    private void drawSkillUI(Graphics2D g2d, int x, int y, Image img, String keybind, int currentCd, Color fallbackColor) {
        int size = 60; if (img != null && img.getWidth(null) > 0) g2d.drawImage(img, x, y, size, size, null); else { g2d.setColor(fallbackColor); g2d.fillRect(x, y, size, size); }
        g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(2)); g2d.drawRect(x, y, size, size); g2d.setFont(new Font("Arial", Font.BOLD, 14)); g2d.setColor(Color.WHITE); g2d.drawString(keybind, x + 5, y + 15);
        if (currentCd > 0) {
            g2d.setColor(new Color(0, 0, 0, 180)); g2d.fillRect(x, y, size, size); g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 20)); String cdText = String.format("%.1f", currentCd / 60.0); FontMetrics fm = g2d.getFontMetrics(); g2d.drawString(cdText, x + (size - fm.stringWidth(cdText)) / 2, y + 35);
        }
    }

    @Override public void mouseMoved(MouseEvent e) { 
        int adjX = e.getX() - getOffsetX(); int adjY = e.getY() - getOffsetY(); 
        if (currentState == GameState.MENU) startMenu.update(adjX, adjY); 
        else if (currentState == GameState.PAUSED) pauseMenu.update(adjX, adjY, 900); 
        else if (player != null && !player.isDead) { player.mouseX = adjX; player.mouseY = adjY; } 
    }
    @Override public void mousePressed(MouseEvent e) { 
        int adjX = e.getX() - getOffsetX(); int adjY = e.getY() - getOffsetY(); 
        if (currentState == GameState.MENU) startMenu.mousePressed(adjX, adjY, this); 
        else if (currentState == GameState.PAUSED) pauseMenu.mousePressed(adjX, adjY, this); 
        else if (currentState == GameState.PLAYING && player != null && !player.isDead && player.currentHost != null) player.currentHost.mousePressed(e); 
    }
    @Override public void mouseDragged(MouseEvent e) { 
        int adjX = e.getX() - getOffsetX(); int adjY = e.getY() - getOffsetY();
        if (currentState == GameState.PAUSED) pauseMenu.mouseDragged(adjX, adjY);
        else mouseMoved(e); 
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (currentState == GameState.PAUSED) pauseMenu.mouseReleased();
    }
    
    @Override public void keyPressed(KeyEvent e) { 
        if (e.getKeyCode() == KeyEvent.VK_M) { 
            if (currentState == GameState.PLAYING) {
                currentState = GameState.PAUSED;
                pauseBGM();
            } else if (currentState == GameState.PAUSED) {
                currentState = GameState.PLAYING;
                resumeBGM();
            }
        } 
        if (currentState == GameState.PLAYING && player != null) { 
            if (e.getKeyCode() == KeyEvent.VK_SPACE && player.currentTempo != null) player.currentTempo.activate(); 
            if (!player.isDead && player.currentHost != null) player.currentHost.keyPressed(e); 
        } 
    }
    
    @Override public void keyReleased(KeyEvent e) { if (currentState == GameState.PLAYING && player != null && !player.isDead && player.currentHost != null) player.currentHost.keyReleased(e); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    class RainDrop {
        double x, y, vx, vy;
        float alpha;
        double targetY; 

        RainDrop() {
            this.x = Math.random() * 1400 - 300; 
            this.y = -50 - Math.random() * 200;
            double z = 0.5 + Math.random(); 
            this.vx = 2.5 * z;   
            this.vy = 22 * z;    
            this.alpha = (float) (0.15 + 0.35 * z); 
            this.targetY = 200 + Math.random() * 700;
        }

        boolean update(double timeMultiplier) {
            x += vx * timeMultiplier;
            y += vy * timeMultiplier;
            return y >= targetY; 
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(170, 200, 255, (int)(alpha * 255)));
            g2d.setStroke(new BasicStroke(alpha > 0.4f ? 2 : 1)); 
            g2d.drawLine((int)x, (int)y, (int)(x - vx * 1.5), (int)(y - vy * 1.5));
            g2d.setStroke(new BasicStroke(1));
        }
    }
}