package fr.videoeditor.export;

import fr.videoeditor.model.VideoSegment;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour stacker les images d'un segment vidéo
 */
public class ImageStacker {
    
    public interface ProgressListener {
        void onProgress(int current, int total, String message);
        void onComplete(boolean success, String message);
    }
    
    public static void stackSegment(VideoSegment segment, File outputFile, 
                                   String format, ProgressListener listener) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                FFmpegFrameGrabber grabber = null;
                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                Java2DFrameConverter imageConverter = new Java2DFrameConverter();
                
                try {
                    grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                    grabber.start();
                    
                    long startTimestamp = (long) (segment.getStartTime() * 1_000_000);
                    long endTimestamp = (long) (segment.getEndTime() * 1_000_000);
                    
                    grabber.setTimestamp(startTimestamp);
                    
                    List<Mat> frames = new ArrayList<>();
                    int frameCount = 0;
                    
                    publish("Collecte des frames...");
                    
                    while (true) {
                        Frame frame = grabber.grabImage();
                        if (frame == null) break;
                        
                        long timestamp = grabber.getTimestamp();
                        if (timestamp > endTimestamp) break;
                        
                        Mat mat = matConverter.convert(frame);
                        if (mat != null) {
                            frames.add(mat.clone());
                            frameCount++;
                            
                            if (frameCount % 10 == 0) {
                                final int current = frameCount;
                                publish(String.format("Frames collectées: %d", current));
                                if (listener != null) {
                                    SwingUtilities.invokeLater(() -> 
                                        listener.onProgress(current, -1, 
                                            String.format("Frames collectées: %d", current)));
                                }
                            }
                        }
                    }
                    
                    grabber.stop();
                    grabber.release();
                    
                    if (frames.isEmpty()) {
                        publish("Aucune frame trouvée");
                        matConverter.close();
                        imageConverter.close();
                        return false;
                    }
                    
                    publish(String.format("Stacking de %d frames...", frames.size()));
                    
                    // Stacker les images
                    Mat result = stackImages(frames);
                    
                    // Nettoyer les frames
                    for (Mat mat : frames) {
                        mat.release();
                    }
                    frames.clear();
                    
                    // Convertir en BufferedImage
                    Frame resultFrame = matConverter.convert(result);
                    BufferedImage image = imageConverter.convert(resultFrame);
                    result.release();
                    
                    // Sauvegarder
                    publish("Sauvegarde de l'image...");
                    ImageIO.write(image, format, outputFile);
                    
                    matConverter.close();
                    imageConverter.close();
                    
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
                    matConverter.close();
                    imageConverter.close();
                    return false;
                }
            }
            
            private Mat stackImages(List<Mat> frames) {
                if (frames.isEmpty()) return null;
                
                Mat result = new Mat();
                frames.get(0).copyTo(result);
                result.convertTo(result, CV_32F);
                
                // Moyenne des frames
                for (int i = 1; i < frames.size(); i++) {
                    Mat temp = new Mat();
                    frames.get(i).convertTo(temp, CV_32F);
                    add(result, temp, result);
                    temp.release();
                }
                
                divide(result, new Mat(result.size(), result.type(), 
                      Scalar.all(frames.size())), result);
                
                Mat output = new Mat();
                result.convertTo(output, CV_8U);
                result.release();
                
                return output;
            }
            
            @Override
            protected void process(List<String> chunks) {
                // Messages déjà gérés dans doInBackground
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (listener != null) {
                        if (success) {
                            listener.onComplete(true, 
                                "Image stackée sauvegardée:\n" + 
                                outputFile.getAbsolutePath());
                        } else {
                            listener.onComplete(false, "Échec du stacking");
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
