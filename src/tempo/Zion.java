package tempo;

import core.GamePanel;
import entity.Player;
import vfx.Particle;
import java.awt.Color;

public class Zion extends Tempo {

    public double skyAngle = 0; 
    public double fadeAlpha = 0; // ★ 控制天體與濾鏡的淡入淡出透明度 (0.0 ~ 1.0)
    public double[][] stars = new double[150][3]; // ★ 星星陣列：[x, y, 閃爍相位]

    public Zion(Player player) {
        // 冷卻 45 秒 (2700 幀)，持續 30 秒 (1800 幀)
        super(player, 45 * 60, 30 * 60); 
        
        // ★ 初始化 150 顆隨機分佈在廣大宇宙的星星
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = Math.random() * 3000 - 1000;
            stars[i][1] = Math.random() * 3000 - 1000;
            stars[i][2] = Math.random() * Math.PI * 2; // 隨機閃爍起點
        }
    }

    @Override
    public void update() {
        if (duration > 0) {
            duration--; 
            fadeAlpha = Math.min(1.0, fadeAlpha + 0.05); // 啟動時快速淡入 (約 20 幀)
            
            double mult = 1.0;
            
            if (duration > 1320) {
                // 前 8 秒：從 1.0 加速到 3.0
                mult = 1.0 + 2.0 * ((1800.0 - duration) / 480.0);
            } 
            else if (duration > 480) {
                // 中間 14 秒：維持巔峰 3.0 倍速
                mult = 3.0;
            } 
            else {
                // 後 8 秒：慢慢衰退回 1.0
                mult = 1.0 + 2.0 * (duration / 480.0);
            }
            
            player.timeMultiplier = mult;

            // 時間倍率越高，太陽與月亮的自轉就越瘋狂
            skyAngle += 0.02 * Math.pow(mult, 2.2);

            // 加速時間殘影
            if (Math.random() < 0.15 * mult) {
                GamePanel.particles.add(new Particle(
                    player.x + Math.random()*40-20, 
                    player.y + Math.random()*40-20, 
                    new Color(0, 255, 255)
                ));
            }
            
        } else {
            // ★ 技能結束，重置倍率並開始冷卻
            player.timeMultiplier = 1.0;
            if (cooldown > 0) cooldown--; 
            
            // ★ 淡出特效引擎：技能結束後，背景天體花約 1.5 秒慢慢消失
            fadeAlpha = Math.max(0.0, fadeAlpha - 0.015); 
            if (fadeAlpha > 0) {
                skyAngle += 0.01; // 淡出時保留一點點公轉的「殘留慣性」
            }
        }

        // ★ 更新星星的閃爍頻率 (隨著時間加速，星星會閃得越來越快)
        if (fadeAlpha > 0) {
            for (int i = 0; i < stars.length; i++) {
                stars[i][2] += 0.03 * player.timeMultiplier;
            }
        }
    }

    @Override
    public void activate() {
        if (cooldown <= 0 && duration <= 0) {
            duration = MAX_DURATION;
            cooldown = MAX_CD;
            skyAngle = 0; 
        }
    }
}