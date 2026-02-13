package fr.videoeditor.export;

import fr.videoeditor.model.VideoSegment;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import nom.tam.fits.*;
import nom.tam.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
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
                                   String format, double brightnessMultiplier, ProgressListener listener) {
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
                    List<Mat> offsetFrames = new ArrayList<>();
                    int frameCount = 0;
                    
                    publish("Collecte des frames...");
                    
                    // Collecter les frames d'offset si activé
                    if (segment.isOffsetEnabled()) {
                        publish("Collecte des frames d'offset (dark frames)...");
                        long offsetStartTimestamp = (long) (segment.getOffsetStart() * 1_000_000);
                        long offsetEndTimestamp = (long) (segment.getOffsetEnd() * 1_000_000);
                        
                        grabber.setTimestamp(offsetStartTimestamp);
                        
                        while (true) {
                            Frame frame = grabber.grabImage();
                            if (frame == null) break;
                            
                            long timestamp = grabber.getTimestamp();
                            if (timestamp > offsetEndTimestamp) break;
                            
                            Mat mat = matConverter.convert(frame);
                            if (mat != null) {
                                offsetFrames.add(mat.clone());
                            }
                        }
                        
                        publish(String.format("Frames d'offset collectées: %d", offsetFrames.size()));
                        
                        // Revenir au début du segment principal
                        grabber.setTimestamp(startTimestamp);
                    }
                    
                    // Collecter les frames principales
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
                        for (Mat mat : offsetFrames) {
                            mat.release();
                        }
                        matConverter.close();
                        imageConverter.close();
                        return false;
                    }
                    
                    publish(String.format("Stacking de %d frames...", frames.size()));
                    
                    // Stacker les images avec offset si disponible
                    Mat result;
                    if (!offsetFrames.isEmpty()) {
                        publish("Application de la soustraction d'offset...");
                        result = stackImagesWithOffset(frames, offsetFrames);
                    } else {
                        result = stackImages(frames);
                    }
                    
                    // Nettoyer les frames
                    for (Mat mat : frames) {
                        mat.release();
                    }
                    frames.clear();
                    
                    for (Mat mat : offsetFrames) {
                        mat.release();
                    }
                    offsetFrames.clear();
                    
                    // Appliquer la luminosité
                    if (brightnessMultiplier != 1.0) {
                        multiply(result, new Mat(result.size(), result.type(), 
                            Scalar.all(brightnessMultiplier)), result);
                    }
                    
                    // Sauvegarder selon le format
                    publish("Sauvegarde de l'image...");
                    if (format.equalsIgnoreCase("fits")) {
                        saveFits(result, outputFile);
                    } else {
                        // Convertir en 8U pour PNG/JPG
                        Mat output8U = new Mat();
                        result.convertTo(output8U, CV_8U);
                        
                        Frame resultFrame = matConverter.convert(output8U);
                        BufferedImage image = imageConverter.convert(resultFrame);
                        ImageIO.write(image, format, outputFile);
                        
                        output8U.release();
                    }
                    
                    result.release();
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
            
            private void saveFits(Mat stackedMat, File outputFile) throws Exception {
                // Convertir Mat en float 32 bits
                Mat floatMat = new Mat();
                stackedMat.convertTo(floatMat, CV_32F);
                
                int height = floatMat.rows();
                int width = floatMat.cols();
                int channels = floatMat.channels();
                
                if (channels == 3) {
                    // Image couleur BGR - sauvegarder 3 plans
                    MatVector bgr = new MatVector();
                    split(floatMat, bgr);
                    
                    float[][][] data = new float[3][height][width];
                    
                    // Extraire les données de chaque canal
                    for (int c = 0; c < 3; c++) {
                        Mat channel = bgr.get(c);
                        
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                data[c][y][x] = channel.ptr(y).getFloat(x * 4);
                            }
                        }
                        channel.release();
                    }
                    bgr.close();
                    
                    // Créer le fichier FITS
                    Fits fits = new Fits();
                    ImageHDU hdu = (ImageHDU) Fits.makeHDU(data);
                    
                    // Ajouter des métadonnées
                    Header header = hdu.getHeader();
                    header.addValue("COMMENT", "Stacked image from video frames", "");
                    header.addValue("BITPIX", -32, "32-bit floating point");
                    header.addValue("NAXIS", 3, "3 axes (color)");
                    
                    fits.addHDU(hdu);
                    
                    // Sauvegarder
                    try (BufferedDataOutputStream dos = new BufferedDataOutputStream(
                            new FileOutputStream(outputFile))) {
                        fits.write(dos);
                    }
                } else {
                    // Image en niveaux de gris
                    float[][] data = new float[height][width];
                    
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            data[y][x] = floatMat.ptr(y).getFloat(x * 4);
                        }
                    }
                    
                    // Créer le fichier FITS
                    Fits fits = new Fits();
                    ImageHDU hdu = (ImageHDU) Fits.makeHDU(data);
                    
                    // Ajouter des métadonnées
                    Header header = hdu.getHeader();
                    header.addValue("COMMENT", "Stacked image from video frames", "");
                    header.addValue("BITPIX", -32, "32-bit floating point");
                    
                    fits.addHDU(hdu);
                    
                    // Sauvegarder
                    try (BufferedDataOutputStream dos = new BufferedDataOutputStream(
                            new FileOutputStream(outputFile))) {
                        fits.write(dos);
                    }
                }
                
                floatMat.release();
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
                
                return result;
            }
            
            private Mat stackImagesWithOffset(List<Mat> frames, List<Mat> offsetFrames) {
                if (frames.isEmpty()) return null;
                
                // Créer le master dark (moyenne des frames d'offset)
                Mat masterDark = new Mat();
                offsetFrames.get(0).copyTo(masterDark);
                masterDark.convertTo(masterDark, CV_32F);
                
                for (int i = 1; i < offsetFrames.size(); i++) {
                    Mat temp = new Mat();
                    offsetFrames.get(i).convertTo(temp, CV_32F);
                    add(masterDark, temp, masterDark);
                    temp.release();
                }
                
                divide(masterDark, new Mat(masterDark.size(), masterDark.type(), 
                      Scalar.all(offsetFrames.size())), masterDark);
                
                // Appliquer la soustraction d'offset et stacker
                Mat result = new Mat();
                Mat correctedFrame = new Mat();
                
                for (int i = 0; i < frames.size(); i++) {
                    Mat frame32F = new Mat();
                    frames.get(i).convertTo(frame32F, CV_32F);
                    
                    // Soustraire le master dark
                    subtract(frame32F, masterDark, correctedFrame);
                    
                    if (i == 0) {
                        correctedFrame.copyTo(result);
                    } else {
                        add(result, correctedFrame, result);
                    }
                    
                    frame32F.release();
                }
                
                // Moyenne
                divide(result, new Mat(result.size(), result.type(), 
                      Scalar.all(frames.size())), result);
                
                masterDark.release();
                correctedFrame.release();
                
                return result;
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
