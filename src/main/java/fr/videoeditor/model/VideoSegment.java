package fr.videoeditor.model;

import java.awt.*;
import java.io.File;

/**
 * Représente un segment de vidéo dans la timeline
 */
public class VideoSegment {
    private File videoFile;
    private double startTime;  // en secondes
    private double endTime;    // en secondes
    private double duration;   // durée totale de la vidéo
    private Color color;
    private boolean stackingEnabled;
    private double stackingStart;
    private double stackingEnd;
    private double offsetStart;  // début offset pour dark frames
    private double offsetEnd;    // fin offset pour dark frames
    private boolean offsetEnabled;
    
    public VideoSegment(File videoFile, double duration) {
        this.videoFile = videoFile;
        this.startTime = 0;
        this.endTime = duration;
        this.duration = duration;
        this.color = generateRandomColor();
        this.stackingEnabled = false;
        this.offsetEnabled = false;
        this.offsetStart = 0;
        this.offsetEnd = 0;
    }
    
    private Color generateRandomColor() {
        int r = 100 + (int)(Math.random() * 155);
        int g = 100 + (int)(Math.random() * 155);
        int b = 100 + (int)(Math.random() * 155);
        return new Color(r, g, b);
    }
    
    public File getVideoFile() {
        return videoFile;
    }
    
    public double getStartTime() {
        return startTime;
    }
    
    public void setStartTime(double startTime) {
        this.startTime = Math.max(0, Math.min(startTime, duration));
        if (this.startTime >= this.endTime) {
            this.startTime = this.endTime - 0.1;
        }
    }
    
    public double getEndTime() {
        return endTime;
    }
    
    public void setEndTime(double endTime) {
        this.endTime = Math.max(0, Math.min(endTime, duration));
        if (this.endTime <= this.startTime) {
            this.endTime = this.startTime + 0.1;
        }
    }
    
    public double getDuration() {
        return duration;
    }
    
    public double getSegmentDuration() {
        return endTime - startTime;
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isStackingEnabled() {
        return stackingEnabled;
    }
    
    public void setStackingEnabled(boolean enabled) {
        this.stackingEnabled = enabled;
    }
    
    public double getStackingStart() {
        return stackingStart;
    }
    
    public void setStackingStart(double stackingStart) {
        this.stackingStart = stackingStart;
    }
    
    public double getStackingEnd() {
        return stackingEnd;
    }
    
    public void setStackingEnd(double stackingEnd) {
        this.stackingEnd = stackingEnd;
    }
    
    public double getOffsetStart() {
        return offsetStart;
    }
    
    public void setOffsetStart(double offsetStart) {
        this.offsetStart = Math.max(0, Math.min(offsetStart, duration));
    }
    
    public double getOffsetEnd() {
        return offsetEnd;
    }
    
    public void setOffsetEnd(double offsetEnd) {
        this.offsetEnd = Math.max(0, Math.min(offsetEnd, duration));
    }
    
    public boolean isOffsetEnabled() {
        return offsetEnabled;
    }
    
    public void setOffsetEnabled(boolean enabled) {
        this.offsetEnabled = enabled;
    }
    
    @Override
    public String toString() {
        return videoFile.getName() + " [" + 
               String.format("%.2f", startTime) + "s - " + 
               String.format("%.2f", endTime) + "s]";
    }
}
