package fr.videoeditor.ui;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Panneau de prévisualisation vidéo
 */
public class VideoPreviewPanel extends JPanel {
    private BufferedImage currentFrame;
    private String statusMessage = "Aucune vidéo chargée";
    
    public VideoPreviewPanel() {
        setPreferredSize(new Dimension(640, 360));
        setBackground(Color.BLACK);
    }
    
    public void loadFrame(File videoFile, double timeInSeconds) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
                    grabber.start();
                    
                    // Convertir le temps en microsecondes
                    long timestamp = (long) (timeInSeconds * 1_000_000);
                    grabber.setTimestamp(timestamp);
                    
                    Frame frame = grabber.grabImage();
                    if (frame != null) {
                        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                            return converter.convert(frame);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        currentFrame = image;
                        statusMessage = String.format("Frame à %.2fs", timeInSeconds);
                    } else {
                        statusMessage = "Erreur de chargement";
                    }
                    repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    statusMessage = "Erreur: " + e.getMessage();
                    repaint();
                }
            }
        };
        worker.execute();
    }
    
    public void clearFrame() {
        currentFrame = null;
        statusMessage = "Aucune vidéo chargée";
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        if (currentFrame != null) {
            // Calculer les dimensions pour maintenir le ratio
            double imgRatio = (double) currentFrame.getWidth() / currentFrame.getHeight();
            double panelRatio = (double) getWidth() / getHeight();
            
            int drawWidth, drawHeight, x, y;
            
            if (imgRatio > panelRatio) {
                drawWidth = getWidth();
                drawHeight = (int) (getWidth() / imgRatio);
                x = 0;
                y = (getHeight() - drawHeight) / 2;
            } else {
                drawWidth = (int) (getHeight() * imgRatio);
                drawHeight = getHeight();
                x = (getWidth() - drawWidth) / 2;
                y = 0;
            }
            
            g2d.drawImage(currentFrame, x, y, drawWidth, drawHeight, null);
        } else {
            // Afficher un message
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(statusMessage);
            int msgX = (getWidth() - msgWidth) / 2;
            int msgY = getHeight() / 2;
            g2d.drawString(statusMessage, msgX, msgY);
        }
    }
}
