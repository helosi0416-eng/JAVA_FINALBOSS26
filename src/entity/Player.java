package entity;

import host.Host;
import tempo.Tempo;
import java.awt.Graphics2D;

public class Player extends Entity {
    
    public double speed = 2.8;
    public boolean w, a, s, d;
    public double mouseX, mouseY;
    public double aimAngle = 0;
    
    public Host currentHost; 
    public Tempo currentTempo;

    public boolean isDead = false;
    public int deathTimer = 0;
    public int glowingTimer = 0;

    // ★ 新增：時間加速控制系統 (供 Zion 等技能使用)
    public double timeMultiplier = 1.0;
    public double timeAccumulator = 0.0;

    public Player(double x, double y) {
        super(x, y, 25, 300); 
    }

    public void setHost(Host host) { this.currentHost = host; }
    public void setTempo(Tempo tempo) { this.currentTempo = tempo; }

    @Override
    public void update() {
        // ★ Tempo 的計時永遠維持正常 1 倍速流動（這樣技能持續時間才不會跟著變快）
        if (currentTempo != null) currentTempo.update(); 

        // ★ 核心黑科技：子幀微積分器
        // 根據加速倍率，決定這個 frame 要讓玩家本體執行幾次 update()
        timeAccumulator += timeMultiplier;
        while (timeAccumulator >= 1.0) {
            
            if (!isDead && currentHost != null) currentHost.update();
            if (glowingTimer > 0) glowingTimer--;
            
            timeAccumulator -= 1.0; // 消耗掉一次執行的時間
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (!isDead && currentHost != null) currentHost.draw(g2d);
    }
}