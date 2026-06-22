package tempo;

import core.GamePanel;
import core.Sound;
import entity.Player;
import host.Invader; 
import vfx.Particle;

import java.awt.Color;

public class Zero extends Tempo {

    public static Sound sfxActivate = new Sound("assets/HaltActivate.wav");

    public Zero(Player player) {
        // 直接透過 super 傳遞冷卻(720)與持續時間(180)，這樣就不會觸發 final 變數報錯了
        super(player, 720, 320); 
    }

    @Override
    public void activate() {
        if (cooldown > 0 || duration > 0) return;

        duration = MAX_DURATION;
        
        GamePanel.screenShakeTimer = 20; 
        sfxActivate.play();
        
        for(int i = 0; i < 50; i++) {
            GamePanel.particles.add(new Particle(player.x, player.y, Color.WHITE));
        }
    }

    @Override
    public void update() {
        if (duration > 0) {
            duration--;

            // 在 Zero 的持續時間裡，所有技能冷卻變成 0.2 秒 (12幀)
            if (player.currentHost instanceof Invader) {
                Invader host = (Invader) player.currentHost;
                
                if (host.slashCD > 6) host.slashCD = 6;
                if (host.spinCD > 6) host.spinCD = 6;
                if (host.dashCD > 6) host.dashCD = 6;
                if (host.empowerCD > 6) host.empowerCD = 6;
            }

            if (duration <= 0) { 
                cooldown = MAX_CD;
            }
        } else if (cooldown > 0) {
            cooldown--;
        }
    }
}