package core;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("HOURS: Reborn");
        GamePanel game = new GamePanel();
        
        frame.add(game);
        // ★ 拿掉 frame.pack()，改為自動最大化全螢幕
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}