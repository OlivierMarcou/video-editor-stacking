package fr.videoeditor.ui;

import fr.videoeditor.model.VideoSegment;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panneau de timeline pour afficher et éditer les segments vidéo
 */
public class TimelinePanel extends JPanel {
    private List<VideoSegment> segments;
    private VideoSegment selectedSegment;
    private Point lastClickPos;
    
    private static final int SEGMENT_HEIGHT = 60;
    private static final int TIME_SCALE_HEIGHT = 30;
    private static final int CURSOR_WIDTH = 3;
    
    private PreviewCallback previewCallback;
    
    public interface PreviewCallback {
        void onPreviewRequest(VideoSegment segment, double time);
    }
    
    public TimelinePanel() {
        segments = new ArrayList<>();
        setPreferredSize(new Dimension(800, 150));
        setMinimumSize(new Dimension(400, 150));
        setBackground(new Color(45, 45, 45));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
    }
    
    public void setPreviewCallback(PreviewCallback callback) {
        this.previewCallback = callback;
    }
    
    public void addSegment(VideoSegment segment) {
        segments.add(segment);
        repaint();
    }
    
    public List<VideoSegment> getSegments() {
        return segments;
    }
    
    public VideoSegment getSelectedSegment() {
        return selectedSegment;
    }
    
    private void handleMousePressed(MouseEvent e) {
        int y = e.getY();
        if (y < TIME_SCALE_HEIGHT) return;
        
        double totalDuration = getTotalDuration();
        if (totalDuration == 0) return;
        
        double currentTime = 0;
        
        for (VideoSegment segment : segments) {
            double segmentDuration = segment.getDuration();
            double segmentStart = currentTime;
            double segmentEnd = currentTime + segmentDuration;
            
            int x1 = timeToX(segmentStart);
            int x2 = timeToX(segmentEnd);
            
            if (e.getX() >= x1 && e.getX() <= x2) {
                selectedSegment = segment;
                
                // Calculer la position relative dans le segment
                double relativePos = (e.getX() - x1) / (double)(x2 - x1);
                double timeInSegment = relativePos * segmentDuration;
                
                if (e.isShiftDown()) {
                    // Shift + clic = définir les curseurs d'offset
                    segment.setOffsetEnabled(true);
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        segment.setOffsetStart(timeInSegment);
                        if (segment.getOffsetEnd() <= segment.getOffsetStart()) {
                            segment.setOffsetEnd(Math.min(timeInSegment + 0.1, segmentDuration));
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        segment.setOffsetEnd(timeInSegment);
                        if (segment.getOffsetStart() >= segment.getOffsetEnd()) {
                            segment.setOffsetStart(Math.max(timeInSegment - 0.1, 0));
                        }
                    }
                } else {
                    // Clic normal = définir début/fin de coupe
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        segment.setStartTime(timeInSegment);
                        if (segment.getEndTime() <= segment.getStartTime()) {
                            segment.setEndTime(Math.min(segment.getStartTime() + 0.1, segment.getDuration()));
                        }
                        if (previewCallback != null) {
                            previewCallback.onPreviewRequest(segment, timeInSegment);
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        segment.setEndTime(timeInSegment);
                        if (segment.getStartTime() >= segment.getEndTime()) {
                            segment.setStartTime(Math.max(segment.getEndTime() - 0.1, 0));
                        }
                        if (previewCallback != null) {
                            previewCallback.onPreviewRequest(segment, timeInSegment);
                        }
                    }
                }
                
                repaint();
                return;
            }
            
            currentTime += segmentDuration;
        }
        
        selectedSegment = null;
        repaint();
    }
    
    private double getTotalDuration() {
        return segments.stream()
                .mapToDouble(VideoSegment::getDuration)
                .sum();
    }
    
    private int timeToX(double time) {
        double totalDuration = getTotalDuration();
        if (totalDuration == 0) return 0;
        return (int) ((time / totalDuration) * getWidth());
    }
    
    private double xToTime(int x) {
        double totalDuration = getTotalDuration();
        return (x / (double) getWidth()) * totalDuration;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (segments.isEmpty()) {
            g2d.setColor(Color.LIGHT_GRAY);
            String msg = "Aucune vidéo chargée. Cliquez sur 'Charger Vidéo' pour commencer.";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = (getHeight() + fm.getHeight()) / 2;
            g2d.drawString(msg, x, y);
            return;
        }
        
        drawTimeScale(g2d);
        drawSegments(g2d);
    }
    
    private void drawTimeScale(Graphics2D g2d) {
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRect(0, 0, getWidth(), TIME_SCALE_HEIGHT);
        
        double totalDuration = getTotalDuration();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        
        for (int i = 0; i <= 10; i++) {
            int x = (int) ((i / 10.0) * getWidth());
            double time = (i / 10.0) * totalDuration;
            g2d.drawLine(x, TIME_SCALE_HEIGHT - 5, x, TIME_SCALE_HEIGHT);
            String timeStr = String.format("%.1fs", time);
            g2d.drawString(timeStr, x - 15, TIME_SCALE_HEIGHT - 8);
        }
    }
    
    private void drawSegments(Graphics2D g2d) {
        double currentTime = 0;
        
        for (VideoSegment segment : segments) {
            double segmentDuration = segment.getDuration();
            int x1 = timeToX(currentTime);
            int x2 = timeToX(currentTime + segmentDuration);
            int y = TIME_SCALE_HEIGHT + 5;
            
            // Dessiner le segment complet (grisé)
            g2d.setColor(segment.getColor().darker().darker());
            g2d.fillRect(x1, y, x2 - x1, SEGMENT_HEIGHT);
            
            // Dessiner la partie sélectionnée (entre start et end)
            int startX = timeToX(currentTime + segment.getStartTime());
            int endX = timeToX(currentTime + segment.getEndTime());
            g2d.setColor(segment.getColor());
            g2d.fillRect(startX, y, endX - startX, SEGMENT_HEIGHT);
            
            // Bordure
            if (segment == selectedSegment) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
            } else {
                g2d.setColor(segment.getColor().darker());
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawRect(x1, y, x2 - x1, SEGMENT_HEIGHT);
            
            // Curseurs de début et fin (rouge et bleu)
            g2d.setColor(Color.RED);
            g2d.fillRect(startX - CURSOR_WIDTH/2, y, CURSOR_WIDTH, SEGMENT_HEIGHT);
            
            g2d.setColor(Color.BLUE);
            g2d.fillRect(endX - CURSOR_WIDTH/2, y, CURSOR_WIDTH, SEGMENT_HEIGHT);
            
            // Curseurs d'offset (orange) si activés
            if (segment.isOffsetEnabled()) {
                int offsetStartX = timeToX(currentTime + segment.getOffsetStart());
                int offsetEndX = timeToX(currentTime + segment.getOffsetEnd());
                
                // Zone d'offset en fond orange transparent
                g2d.setColor(new Color(255, 165, 0, 80));
                g2d.fillRect(offsetStartX, y, offsetEndX - offsetStartX, SEGMENT_HEIGHT);
                
                // Curseurs d'offset en orange vif
                g2d.setColor(new Color(255, 140, 0));
                g2d.fillRect(offsetStartX - CURSOR_WIDTH/2, y, CURSOR_WIDTH, SEGMENT_HEIGHT);
                g2d.fillRect(offsetEndX - CURSOR_WIDTH/2, y, CURSOR_WIDTH, SEGMENT_HEIGHT);
                
                // Label OFFSET
                g2d.setColor(new Color(255, 140, 0));
                g2d.setFont(new Font("Arial", Font.BOLD, 9));
                g2d.drawString("OFFSET", offsetStartX + 5, y + 50);
            }
            
            // Nom du fichier
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String name = segment.getVideoFile().getName();
            if (name.length() > 20) name = name.substring(0, 17) + "...";
            g2d.drawString(name, x1 + 15, y + 20);
            
            // Durée
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String duration = String.format("%.2fs - %.2fs", segment.getStartTime(), segment.getEndTime());
            g2d.drawString(duration, x1 + 15, y + 35);
            
            currentTime += segmentDuration;
        }
    }
}
