package tempo;

import core.GamePanel;
import entity.Dummy;
import entity.Entity;       // ★ 新增：匯入通用實體
import entity.Bloxxerman;   // ★ 新增：支援新王
import entity.Equinox;
import entity.Player;
import host.Invader;
import vfx.Particle;

import java.util.LinkedList;
import java.util.ArrayList;

public class Undo extends Tempo {
    
    private final int MAX_HISTORY = 300;
    private LinkedList<Snapshot> history = new LinkedList<>();
    
    public int effectTimer = 0;

    public Undo(Player player) {
        super(player, 6 * 60, 0); 
    }

    @Override
    public void update() {
        if (cooldown > 0) cooldown--;
        if (effectTimer > 0) effectTimer--;

        recordSnapshot();
    }

    private void recordSnapshot() {
        Snapshot snap = new Snapshot();
        snap.px = player.x; snap.py = player.y; snap.pAimAngle = player.aimAngle; snap.pHp = player.hp;
        
        if (player.currentHost instanceof Invader) {
            Invader inv = (Invader) player.currentHost;
            snap.slashCD = inv.slashCD; snap.spinCD = inv.spinCD; snap.dashCD = inv.dashCD;
            snap.empowerCD = inv.empowerCD; snap.empowerDuration = inv.empowerDuration;
        }
        
        for (Dummy d : GamePanel.dummies) {
            snap.dummySnaps.add(new DummySnapshot(d.x, d.y, d.hp, d.kx, d.ky, d.stunTimer, d.blinkTimer));
        }

        // ★ 核心修正：使用 instanceof 來判斷目前畫面上是哪隻王，再儲存對應的變數！
        if (GamePanel.boss != null) {
            if (GamePanel.boss instanceof Equinox) {
                Equinox eq = (Equinox) GamePanel.boss;
                snap.equinoxSnap = new EquinoxSnapshot(
                    eq.x, eq.y, eq.hp,
                    eq.currentState, eq.stateTimer, eq.actionCD,
                    eq.visualScale, eq.illuminaAngle, eq.darkheartAngle,
                    eq.illuminaExt, eq.darkheartExt, eq.m1Step,
                    eq.hasHitThisMove, eq.hasTriggeredUlt66, eq.hasTriggeredUlt33
                );
            } else if (GamePanel.boss instanceof Bloxxerman) {
                // Bloxxerman 比較單純，我們存他的通用屬性即可完美倒帶
                snap.basicBossSnap = new BossBasicSnapshot(GamePanel.boss.x, GamePanel.boss.y, GamePanel.boss.hp);
            }
        }
        
        history.addLast(snap);
        if (history.size() > MAX_HISTORY) history.removeFirst();
    }

    @Override
    public void activate() {
        if (cooldown <= 0 && !history.isEmpty()) {
            Snapshot snap = history.getFirst(); 

            player.x = snap.px; player.y = snap.py; player.aimAngle = snap.pAimAngle; player.hp = snap.pHp;
            if (player.currentHost instanceof Invader) {
                Invader inv = (Invader) player.currentHost;
                inv.slashCD = snap.slashCD; inv.spinCD = snap.spinCD; inv.dashCD = snap.dashCD;
                inv.empowerCD = snap.empowerCD; inv.empowerDuration = snap.empowerDuration;
            }

            GamePanel.dummies.clear();
            for (int i = 0; i < snap.dummySnaps.size(); i++) {
                DummySnapshot ds = snap.dummySnaps.get(i);
                Dummy d = new Dummy(ds.x, ds.y, Dummy.DummyType.IDLE, player); 
                d.hp = ds.hp; d.kx = ds.kx; d.ky = ds.ky; d.stunTimer = ds.stunTimer; d.blinkTimer = ds.blinkTimer;
                if (i == 1) d.type = Dummy.DummyType.ROAM;
                else if (i == 2) d.type = Dummy.DummyType.MELEE;
                else if (i == 3) d.type = Dummy.DummyType.RANGED;
                GamePanel.dummies.add(d);
            }

            // ★ 核心修正：還原 Boss 狀態時，根據 Snapshot 裡記錄的王來還原
            if (snap.equinoxSnap != null) {
                if (!(GamePanel.boss instanceof Equinox)) {
                    GamePanel.boss = new Equinox(snap.equinoxSnap.x, snap.equinoxSnap.y, player);
                }
                Equinox eq = (Equinox) GamePanel.boss;
                eq.x = snap.equinoxSnap.x;
                eq.y = snap.equinoxSnap.y;
                eq.hp = snap.equinoxSnap.hp;
                eq.currentState = snap.equinoxSnap.state;
                eq.stateTimer = snap.equinoxSnap.stateTimer;
                eq.actionCD = snap.equinoxSnap.actionCD;
                eq.visualScale = snap.equinoxSnap.visualScale;
                eq.illuminaAngle = snap.equinoxSnap.illuminaAngle;
                eq.darkheartAngle = snap.equinoxSnap.darkheartAngle;
                eq.illuminaExt = snap.equinoxSnap.illuminaExt;
                eq.darkheartExt = snap.equinoxSnap.darkheartExt;
                eq.m1Step = snap.equinoxSnap.m1Step;
                eq.hasHitThisMove = snap.equinoxSnap.hasHitThisMove;
                eq.hasTriggeredUlt66 = snap.equinoxSnap.hasTriggeredUlt66;
                eq.hasTriggeredUlt33 = snap.equinoxSnap.hasTriggeredUlt33;
                eq.swordAuras.clear(); // 清空大招方塊防呆
                
            } else if (snap.basicBossSnap != null) {
                if (!(GamePanel.boss instanceof Bloxxerman)) {
                    GamePanel.boss = new Bloxxerman(snap.basicBossSnap.x, snap.basicBossSnap.y, player);
                }
                GamePanel.boss.x = snap.basicBossSnap.x;
                GamePanel.boss.y = snap.basicBossSnap.y;
                GamePanel.boss.hp = snap.basicBossSnap.hp;
            } else {
                GamePanel.boss = null;
            }

            GamePanel.particles.clear(); GamePanel.trails.clear(); GamePanel.dmgTexts.clear(); GamePanel.projectiles.clear(); 
            GamePanel.dummyRespawnTimer = 0; GamePanel.padDamageCD = 0; GamePanel.padHealCD = 0; GamePanel.padKillCD = 0;
            GamePanel.screenShakeTimer = 0; 

            for(int i = 0; i < 30; i++) GamePanel.particles.add(new Particle(player.x, player.y));
            effectTimer = 12; cooldown = MAX_CD; history.clear(); 
        }
    }

    class Snapshot {
        double px, py, pAimAngle; int pHp;
        int slashCD, spinCD, dashCD, empowerCD, empowerDuration;
        ArrayList<DummySnapshot> dummySnaps = new ArrayList<>();
        EquinoxSnapshot equinoxSnap = null;   // ★ 區分不同王的快照
        BossBasicSnapshot basicBossSnap = null; 
    }
    
    class DummySnapshot {
        double x, y, kx, ky; int hp, stunTimer, blinkTimer;
        DummySnapshot(double x, double y, int hp, double kx, double ky, int st, int bt) {
            this.x = x; this.y = y; this.hp = hp; this.kx = kx; this.ky = ky; this.stunTimer = st; this.blinkTimer = bt;
        }
    }

    // ★ Equinox 專屬快照
    class EquinoxSnapshot {
        double x, y; int hp; Equinox.State state; int stateTimer; int actionCD;
        double visualScale; double illuminaAngle, darkheartAngle; double illuminaExt, darkheartExt; int m1Step; boolean hasHitThisMove;
        boolean hasTriggeredUlt66, hasTriggeredUlt33;

        EquinoxSnapshot(double x, double y, int hp, Equinox.State s, int sT, int aCD, double vS, double iA, double dA, double iE, double dE, int m1, boolean hH, boolean u66, boolean u33) {
            this.x = x; this.y = y; this.hp = hp; this.state = s; this.stateTimer = sT; this.actionCD = aCD;
            this.visualScale = vS; this.illuminaAngle = iA; this.darkheartAngle = dA; this.illuminaExt = iE; this.darkheartExt = dE;
            this.m1Step = m1; this.hasHitThisMove = hH; this.hasTriggeredUlt66 = u66; this.hasTriggeredUlt33 = u33;
        }
    }
    
    // ★ 供未來新王通用的基礎快照 (只倒帶位置與血量)
    class BossBasicSnapshot {
        double x, y; int hp;
        BossBasicSnapshot(double x, double y, int hp) {
            this.x = x; this.y = y; this.hp = hp;
        }
    }
}