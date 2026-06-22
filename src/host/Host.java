package host;

import entity.Player;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public abstract class Host {
    public Player player; // 綁定操作此機甲的玩家

    public Host(Player player) {
        this.player = player;
    }

    // 規定所有繼承此類別的機甲都必須實作以下方法
    public abstract void update();
    public abstract void draw(Graphics2D g2d);
    public abstract void mousePressed(MouseEvent e);
    public abstract void keyPressed(KeyEvent e);
    public abstract void keyReleased(KeyEvent e);
}