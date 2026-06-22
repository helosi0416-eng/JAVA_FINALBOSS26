package vfx;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class TrailFrame {
    public double px, py, aimAngle, extraSpinAngle;
    public double curLeftX, curLeftY, curLeftAngle;
    public double curRightX, curRightY, curRightAngle;
    public int life = 20; 

    public TrailFrame(double px, double py, double aimAngle, double extraSpinAngle, 
                      double curLeftX, double curLeftY, double curLeftAngle, 
                      double curRightX, double curRightY, double curRightAngle) {
        this.px = px; this.py = py; this.aimAngle = aimAngle; this.extraSpinAngle = extraSpinAngle;
        this.curLeftX = curLeftX; this.curLeftY = curLeftY; this.curLeftAngle = curLeftAngle;
        this.curRightX = curRightX; this.curRightY = curRightY; this.curRightAngle = curRightAngle;
    }

    public void update() {
        this.life--;
    }

    public void draw(Graphics2D g2d) {
        float alpha = this.life / 20f; 
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        
        // 畫筆沾上「半透明」顏料
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.7f)); 

        AffineTransform oldAT = g2d.getTransform();
        g2d.translate(this.px, this.py);
        g2d.rotate(this.aimAngle + this.extraSpinAngle);

        int[] sx = {0, 15, 45, 45, 15, 0};
        int[] sy = {-3, -7, -3, 3, 7, 3};
        Color trailColor = new Color(210, 210, 210); 

        AffineTransform atL = g2d.getTransform();
        g2d.translate(this.curLeftX, this.curLeftY);
        g2d.rotate(this.curLeftAngle);
        g2d.setColor(trailColor);
        g2d.fillPolygon(sx, sy, 6);
        g2d.setTransform(atL);

        g2d.translate(this.curRightX, this.curRightY);
        g2d.rotate(this.curRightAngle);
        g2d.setColor(trailColor);
        g2d.fillPolygon(sx, sy, 6);
        
        g2d.setTransform(oldAT);
        
        // ★ 修復 BUG：畫完殘影後，立刻把畫筆洗乾淨，恢復 100% 不透明！
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}