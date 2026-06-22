package ui;

import core.GamePanel;
import java.awt.*;

public class StartMenu {
    
    public int hostIndex = 0;
    public int tempoIndex = 0; // 0:Halt, 1:Undo, 2:Zero, 3:Zion
    public int diffIndex = 1;  // 0:簡單, 1:普通, 2:困難

    public void update(int mouseX, int mouseY) { }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Microsoft JhengHei", Font.BOLD, 50));
        g2d.drawString("PROJECT: HOURS", 230, 200);

        g2d.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));

        // Host 選擇
        g2d.setColor(Color.WHITE); g2d.drawString("Host:", 280, 320);
        g2d.setColor(hostIndex == 0 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(380, 295, 120, 35); g2d.drawString("Invader", 400, 320);

        // ★ Tempo 選擇 (重新排版以容納 4 個技能)
        g2d.setColor(Color.WHITE); g2d.drawString("Tempo:", 250, 400);
        g2d.setColor(tempoIndex == 0 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(330, 375, 60, 35); g2d.drawString("Halt", 340, 400);
        
        g2d.setColor(tempoIndex == 1 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(400, 375, 60, 35); g2d.drawString("Undo", 405, 400);
        
        g2d.setColor(tempoIndex == 2 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(470, 375, 60, 35); g2d.drawString("Zero", 480, 400);

        g2d.setColor(tempoIndex == 3 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(540, 375, 60, 35); g2d.drawString("Zion", 550, 400);

        // 難度選擇
        g2d.setColor(Color.WHITE); g2d.drawString("難度:", 280, 480);
        g2d.setColor(diffIndex == 0 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(380, 455, 80, 35); g2d.drawString("簡單", 395, 480);
        g2d.setColor(diffIndex == 1 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(480, 455, 80, 35); g2d.drawString("普通", 495, 480);
        g2d.setColor(diffIndex == 2 ? Color.GREEN : Color.GRAY);
        g2d.drawRect(580, 455, 80, 35); g2d.drawString("困難", 595, 480);

        // Start 按鈕
        g2d.setColor(Color.WHITE);
        g2d.drawRect(350, 600, 200, 50);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("START", 408, 633);
    }

    public void mousePressed(int x, int y, GamePanel gp) {
        if (new Rectangle(380, 295, 120, 35).contains(x, y)) hostIndex = 0;
        
        if (new Rectangle(330, 375, 60, 35).contains(x, y)) tempoIndex = 0;
        if (new Rectangle(400, 375, 60, 35).contains(x, y)) tempoIndex = 1;
        if (new Rectangle(470, 375, 60, 35).contains(x, y)) tempoIndex = 2;
        if (new Rectangle(540, 375, 60, 35).contains(x, y)) tempoIndex = 3; // ★ 選中 Zion

        if (new Rectangle(380, 455, 80, 35).contains(x, y)) diffIndex = 0;
        if (new Rectangle(480, 455, 80, 35).contains(x, y)) diffIndex = 1;
        if (new Rectangle(580, 455, 80, 35).contains(x, y)) diffIndex = 2;

        if (new Rectangle(350, 600, 200, 50).contains(x, y)) {
            if (diffIndex == 0) GamePanel.difficultyMultiplier = 0.65;
            else if (diffIndex == 1) GamePanel.difficultyMultiplier = 1.0;
            else GamePanel.difficultyMultiplier = 1.35;
            
            gp.startGame(hostIndex, tempoIndex);
        }
    }
}