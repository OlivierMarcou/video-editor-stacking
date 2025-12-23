package fr.videoeditor.repair;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.*;
import java.util.List;

/**
 * Reconstruit un fichier MP4 valide à partir de NAL units H.264 extraites
 * Similaire à Digital Video Repair - approche hybride:
 * 1. Extraction directe des NAL units du flux corrompu
 * 2. Reconstruction d'un flux H.264 brut valide
 * 3. Remuxage en MP4 avec FFmpeg
 */
public class MP4Rebuilder {
    
    public interface ProgressCallback {
        void onProgress(String message);
    }
    
    /**
     * Répare un fichier vidéo corrompu sans vidéo de référence
     * Approche Digital Video Repair
     */
    public static File repairWithoutReference(File damagedFile, File outputFile, ProgressCallback callback) 
            throws Exception {
        
        if (callback != null) {
            callback.onProgress("═══ RÉPARATION STYLE DIGITAL VIDEO REPAIR ═══");
            callback.onProgress("Aucune vidéo de référence nécessaire");
            callback.onProgress("");
        }
        
        // Étape 1: Analyser le fichier pour trouver les paramètres vidéo
        if (callback != null) {
            callback.onProgress("Étape 1/4: Analyse du flux H.264...");
        }
        
        H264RawParser.VideoInfo videoInfo = H264RawParser.analyzeFile(damagedFile, 
            new H264RawParser.ProgressCallback() {
                @Override
                public void onProgress(String message) {
                    if (callback != null) {
                        callback.onProgress("  " + message);
                    }
                }
            });
        
        if (!videoInfo.found) {
            throw new Exception(
                "Impossible de trouver les paramètres vidéo (SPS) dans le fichier.\n" +
                "Le fichier ne contient peut-être pas de flux H.264 valide."
            );
        }
        
        if (callback != null) {
            callback.onProgress("");
            callback.onProgress("✓ Paramètres vidéo détectés:");
            callback.onProgress("  Résolution: " + videoInfo.width + "x" + videoInfo.height);
            callback.onProgress("  Framerate: " + String.format("%.2f", videoInfo.frameRate) + " fps");
            callback.onProgress("  Profile: " + videoInfo.profile + ", Level: " + videoInfo.level);
            callback.onProgress("");
        }
        
        // Étape 2: Extraire toutes les NAL units
        if (callback != null) {
            callback.onProgress("Étape 2/4: Extraction des NAL units...");
        }
        
        List<H264RawParser.NALUnit> nalUnits = H264RawParser.extractNALUnits(damagedFile,
            new H264RawParser.ProgressCallback() {
                @Override
                public void onProgress(String message) {
                    if (callback != null) {
                        callback.onProgress("  " + message);
                    }
                }
            });
        
        if (nalUnits.isEmpty()) {
            throw new Exception("Aucune NAL unit trouvée dans le fichier");
        }
        
        // Compter les frames
        int frameCount = 0;
        int idrCount = 0;
        for (H264RawParser.NALUnit nal : nalUnits) {
            if (nal.isFrame()) frameCount++;
            if (nal.isIDR()) idrCount++;
        }
        
        if (callback != null) {
            callback.onProgress("");
            callback.onProgress("✓ Extraction terminée:");
            callback.onProgress("  NAL units totales: " + nalUnits.size());
            callback.onProgress("  Frames détectées: " + frameCount);
            callback.onProgress("  I-frames (IDR): " + idrCount);
            callback.onProgress("");
        }
        
        if (frameCount == 0) {
            throw new Exception(
                "Aucune frame vidéo détectée.\n" +
                "Le fichier pourrait être trop corrompu ou ne pas contenir de vidéo H.264."
            );
        }
        
        // Étape 3: Reconstruire un fichier H.264 brut valide
        if (callback != null) {
            callback.onProgress("Étape 3/4: Reconstruction du flux H.264...");
        }
        
        File h264File = File.createTempFile("reconstructed_", ".h264");
        h264File.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(h264File)) {
            int writtenFrames = 0;
            
            for (H264RawParser.NALUnit nal : nalUnits) {
                // Écrire start code + NAL unit
                fos.write(new byte[]{0x00, 0x00, 0x00, 0x01});
                fos.write(nal.data);
                
                if (nal.isFrame()) {
                    writtenFrames++;
                    if (writtenFrames % 100 == 0 && callback != null) {
                        callback.onProgress("  Frames écrites: " + writtenFrames + "/" + frameCount);
                    }
                }
            }
        }
        
        if (callback != null) {
            callback.onProgress("  ✓ Flux H.264 reconstruit: " + h264File.getAbsolutePath());
            callback.onProgress("");
        }
        
        // Étape 4: Convertir le H.264 brut en MP4 avec moov atom
        if (callback != null) {
            callback.onProgress("Étape 4/4: Création du fichier MP4 avec moov atom...");
        }
        
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            // Ouvrir le fichier H.264 reconstruit
            grabber = new FFmpegFrameGrabber(h264File);
            grabber.setFormat("h264");
            grabber.setFrameRate(videoInfo.frameRate);
            grabber.setImageWidth(videoInfo.width);
            grabber.setImageHeight(videoInfo.height);
            grabber.start();
            
            // Créer le fichier MP4 de sortie
            recorder = new FFmpegFrameRecorder(outputFile, videoInfo.width, videoInfo.height, 0);
            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(videoInfo.frameRate);
            recorder.setVideoBitrate(5000000);
            recorder.setVideoOption("preset", "medium");
            recorder.start();
            
            // Copier les frames
            int processedFrames = 0;
            Frame frame;
            
            while ((frame = grabber.grabImage()) != null) {
                recorder.record(frame);
                processedFrames++;
                
                if (processedFrames % 50 == 0 && callback != null) {
                    callback.onProgress("  Frames traitées: " + processedFrames);
                }
            }
            
            grabber.stop();
            grabber.release();
            
            recorder.stop();
            recorder.release();
            
            if (callback != null) {
                callback.onProgress("");
                callback.onProgress("══════════════════════════════════════");
                callback.onProgress("✓✓✓ RÉPARATION TERMINÉE AVEC SUCCÈS ✓✓✓");
                callback.onProgress("══════════════════════════════════════");
                callback.onProgress("");
                callback.onProgress("Fichier réparé: " + outputFile.getName());
                callback.onProgress("Frames récupérées: " + processedFrames);
                callback.onProgress("Résolution: " + videoInfo.width + "x" + videoInfo.height);
                callback.onProgress("Framerate: " + String.format("%.2f", videoInfo.frameRate) + " fps");
                callback.onProgress("");
                
                double durationSeconds = processedFrames / videoInfo.frameRate;
                int minutes = (int) (durationSeconds / 60);
                int seconds = (int) (durationSeconds % 60);
                callback.onProgress("Durée estimée: " + minutes + "m " + seconds + "s");
            }
            
            // Nettoyer le fichier H.264 temporaire
            h264File.delete();
            
            return outputFile;
            
        } catch (Exception e) {
            if (grabber != null) {
                try { grabber.release(); } catch (Exception ex) {}
            }
            if (recorder != null) {
                try { recorder.release(); } catch (Exception ex) {}
            }
            
            // Nettoyer
            h264File.delete();
            
            throw new Exception(
                "Erreur lors de la création du MP4 final:\n" + e.getMessage() + "\n\n" +
                "Le flux H.264 a été reconstruit mais la conversion en MP4 a échoué.\n" +
                "Fichier H.264 brut disponible temporairement."
            );
        }
    }
}
