package tempo;

import entity.Player;

public abstract class Tempo {
    public Player player;
    public int cooldown = 0;
    public int duration = 0;
    public final int MAX_CD;
    public final int MAX_DURATION;

    public Tempo(Player player, int maxCd, int maxDuration) {
        this.player = player;
        this.MAX_CD = maxCd;
        this.MAX_DURATION = maxDuration;
    }

    public abstract void update();
    public abstract void activate();
}