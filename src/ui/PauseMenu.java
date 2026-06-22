package ui;

import core.GamePanel;
import java.awt.*;

public class PauseMenu {
    private boolean hoverResume = false;
    private boolean hoverQuit = false;
    private boolean draggingVolume = false;

    public void update(int mouseX, int mouseY, int screenWidth) {
        hoverResume = (mouseX > 350 && mouseX < 550 && mouseY > 300 && mouseY < 350);
        hoverQuit = (mouseX > 350 && mouseX < 550 && mouseY > 400 && mouseY < 450);
    }

    // ★ 新增：處理滑鼠拖曳拉桿
    public void mouseDragged(int mouseX, int mouseY) {
        if (draggingVolume) {
            // 計算拉桿位置 (X: 300 ~ 600)
            int vol = (mouseX - 300) * 100 / 300;
            vol = Math.max(0, Math.min(100, vol)); // 限制在 0~100
            GamePanel.globalVolume = vol;
            GamePanel.applyVolume(); // 即時套用音量
        }
    }

    // ★ 接收座標來判斷點擊
    public void mousePressed(int mouseX, int mouseY, GamePanel panel) {
        if (hoverResume) {
            panel.currentState = GamePanel.GameState.PLAYING;
            panel.resumeBGM(); // 恢復音樂
        }
        if (hoverQuit) {
            panel.currentState = GamePanel.GameState.MENU;
            panel.stopAllBGM(); // 回主選單停止音樂
        }
        // 點擊音量拉桿區域
        if (mouseX >= 280 && mouseX <= 620 && mouseY >= 520 && mouseY <= 580) {
            draggingVolume = true;
            mouseDragged(mouseX, mouseY); 
        }
    }
    
    public void mouseReleased() {
        draggingVolume = false; // 放開滑鼠停止拖曳
    }

    public void draw(Graphics2D g2d, int width, int height) {
        // 半透明黑色背景
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 70));
        g2d.drawString("PAUSED", width/2 - 140, 200);

        // 繼續遊戲按鈕
        g2d.setColor(hoverResume ? new Color(100, 100, 100) : new Color(50, 50, 50));
        g2d.fillRect(350, 300, 200, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("RESUME", 398, 335);

        // 回主選單按鈕
        g2d.setColor(hoverQuit ? new Color(100, 100, 100) : new Color(50, 50, 50));
        g2d.fillRect(350, 400, 200, 50);
        g2d.setColor(Color.WHITE);
        g2d.drawString("MAIN MENU", 378, 435);

        // ==========================================
        // ★ 音量拉桿 UI
        // ==========================================
        g2d.drawString("VOLUME: " + GamePanel.globalVolume + "%", 365, 515);
        
        // 灰色底槽 (寬度300)
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(300, 540, 300, 12, 10, 10);
        
        // 藍色進度條
        g2d.setColor(new Color(0, 150, 255));
        int fillWidth = (int)(300 * (GamePanel.globalVolume / 100.0));
        g2d.fillRoundRect(300, 540, fillWidth, 12, 10, 10);

        // 白色拖曳圓鈕
        g2d.setColor(Color.WHITE);
        g2d.fillOval(300 + fillWidth - 12, 534, 24, 24);
    }
}