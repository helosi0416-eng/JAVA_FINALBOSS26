package core;

import javax.sound.sampled.*;
import java.io.File;

public class Sound {
    
    private Clip clip;
    private FloatControl volumeControl;

    public float currentVolumePercent = 35f;
    public float targetVolumePercent = 35f;
    private float fadeStep = 1.0f; 

    public Sound(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                clip = AudioSystem.getClip();
                clip.open(ais);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                }
            } else {
                System.out.println("找不到音效檔：" + filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip == null) return;
        clip.setFramePosition(0); 
        clip.start();
    }

    public void loop() {
        if (clip == null) return;
        this.currentVolumePercent = GamePanel.globalVolume;
        this.targetVolumePercent = GamePanel.globalVolume;
        setVolumePercentage((int)currentVolumePercent);
        
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void resumeLoop() {
        if (clip != null && !clip.isRunning()) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        if (clip == null) return;
        clip.stop();
    }

    // ★ 升級：直接傳入「要花幾幀淡出完畢」 (120幀 = 2秒)
    public void fadeOut(int frames) {
        if (frames <= 0) frames = 1;
        this.fadeStep = this.currentVolumePercent / frames;
        this.targetVolumePercent = 0f;
    }

    public void update() {
        if (clip == null || !clip.isRunning()) return;

        if (currentVolumePercent > targetVolumePercent) {
            currentVolumePercent = Math.max(targetVolumePercent, currentVolumePercent - fadeStep);
            setVolumePercentage((int)currentVolumePercent);
            
            if (currentVolumePercent <= 0) {
                stop();
            }
        } else if (currentVolumePercent < targetVolumePercent) {
            currentVolumePercent = Math.min(targetVolumePercent, currentVolumePercent + fadeStep);
            setVolumePercentage((int)currentVolumePercent);
        }
    }

    public void setVolumePercentage(int percent) {
        if (volumeControl != null) {
            if (percent <= 0) {
                volumeControl.setValue(-80.0f); 
            } else {
                // ★ 加上 -5.0f 的底噪衰減，讓整體音樂柔和不刺耳
                float db = (float) (Math.log10(percent / 100.0) * 20.0f) - 5.0f;
                db = Math.max(-80.0f, Math.min(volumeControl.getMaximum(), db));
                volumeControl.setValue(db);
            }
        }
    }
}