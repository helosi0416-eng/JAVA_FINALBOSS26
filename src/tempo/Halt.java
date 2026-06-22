package tempo;

import core.GamePanel;
import entity.Player;

public class Halt extends Tempo {
    public Halt(Player player) {
        super(player, 15 * 60, 6 * 60); 
    }

    @Override
    public void update() {
        if (duration > 0) {
            duration--;
            GamePanel.isTimeStopped = true; 
            if (duration <= 0) {
                GamePanel.isTimeStopped = false; 
                cooldown = MAX_CD; 
            }
        } else if (cooldown > 0) {
            cooldown--;
        }
    }

    @Override
    public void activate() {
        if (cooldown <= 0 && duration <= 0) {
            duration = MAX_DURATION;
        }
    }
}