package fr.videoeditor.export;

import fr.videoeditor.model.VideoSegment;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Classe pour extraire et sauvegarder les frames d'un segment vidéo
 */
public class FrameExtractor {
    
    public interface ProgressListener {
        void onProgress(int current, int total, String message);
        void onComplete(boolean success, String message);
    }
    
    public static void extractFrames(VideoSegment segment, File outputDir, 
                                     String format, ProgressListener listener) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                FFmpegFrameGrabber grabber = null;
                Java2DFrameConverter converter = new Java2DFrameConverter();
                
                try {
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }
                    
                    grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                    grabber.start();
                    
                    long startTimestamp = (long) (segment.getStartTime() * 1_000_000);
                    long endTimestamp = (long) (segment.getEndTime() * 1_000_000);
                    
                    grabber.setTimestamp(startTimestamp);
                    
                    double frameRate = grabber.getFrameRate();
                    double duration = segment.getSegmentDuration();
                    int totalFrames = (int) (duration * frameRate);
                    
                    int frameCount = 0;
                    String videoName = segment.getVideoFile().getName();
                    videoName = videoName.substring(0, videoName.lastIndexOf('.'));
                    
                    publish(String.format("Extraction des frames de %s...", 
                                        segment.getVideoFile().getName()));
                    
                    while (true) {
                        Frame frame = grabber.grabImage();
                        if (frame == null) break;
                        
                        long timestamp = grabber.getTimestamp();
                        if (timestamp > endTimestamp) break;
                        
                        // Convertir en BufferedImage
                        BufferedImage image = converter.convert(frame);
                        if (image != null) {
                            // Nom de fichier avec timestamp
                            String fileName = String.format("%s_frame_%05d.%s", 
                                                          videoName, frameCount, format);
                            File outputFile = new File(outputDir, fileName);
                            
                            // Sauvegarder l'image
                            ImageIO.write(image, format, outputFile);
                            frameCount++;
                            
                            if (frameCount % 10 == 0) {
                                final int current = frameCount;
                                publish(String.format("Frames extraites: %d/%d", 
                                                    current, totalFrames));
                                if (listener != null) {
                                    SwingUtilities.invokeLater(() -> 
                                        listener.onProgress(current, totalFrames, 
                                            String.format("Frames extraites: %d/%d", 
                                                        current, totalFrames)));
                                }
                            }
                        }
                    }
                    
                    publish(String.format("Extraction terminée: %d frames sauvegardées", 
                                        frameCount));
                    
                    grabber.stop();
                    grabber.release();
                    converter.close();
                    
                    return true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Erreur: " + e.getMessage());
                    if (grabber != null) {
                        try {
                            grabber.stop();
                            grabber.release();
                        } catch (Exception ex) {}
                    }
                    converter.close();
                    return false;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                // Messages déjà gérés dans doInBackground
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (listener != null) {
                        if (success) {
                            listener.onComplete(true, 
                                "Frames extraites avec succès dans:\n" + 
                                outputDir.getAbsolutePath());
                        } else {
                            listener.onComplete(false, "Échec de l'extraction");
                        }
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onComplete(false, "Erreur: " + e.getMessage());
                    }
                }
            }
        };
        
        worker.execute();
    }
}
