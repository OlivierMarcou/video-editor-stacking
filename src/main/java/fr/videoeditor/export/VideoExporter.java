package fr.videoeditor.export;

import fr.videoeditor.model.VideoSegment;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Classe pour exporter les vidéos
 */
public class VideoExporter {
    
    public interface ProgressListener {
        void onProgress(int percent, String message);
        void onComplete(boolean success, String message);
    }
    
    public static void exportVideo(List<VideoSegment> segments, File outputFile, 
                                   double brightnessMultiplier, ProgressListener listener) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                FFmpegFrameRecorder recorder = null;
                Java2DFrameConverter converter = new Java2DFrameConverter();
                
                try {
                    // Déterminer les paramètres de la première vidéo
                    VideoSegment firstSegment = segments.get(0);
                    FFmpegFrameGrabber firstGrabber = new FFmpegFrameGrabber(
                        firstSegment.getVideoFile());
                    firstGrabber.start();
                    
                    int width = firstGrabber.getImageWidth();
                    int height = firstGrabber.getImageHeight();
                    double frameRate = firstGrabber.getFrameRate();
                    int audioChannels = firstGrabber.getAudioChannels();
                    
                    publish("Initialisation de l'export...");
                    
                    // Créer l'enregistreur
                    recorder = new FFmpegFrameRecorder(outputFile, width, height, audioChannels);
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    recorder.setVideoBitrate(8000000); // 8 Mbps
                    
                    if (audioChannels > 0) {
                        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                        recorder.setAudioBitrate(192000); // 192 kbps
                        recorder.setSampleRate(firstGrabber.getSampleRate());
                    }
                    
                    recorder.start();
                    firstGrabber.stop();
                    
                    // Calculer le nombre total de frames
                    int totalFrames = 0;
                    for (VideoSegment segment : segments) {
                        double duration = segment.getSegmentDuration();
                        
                        // Soustraire la durée de l'offset si activé
                        if (segment.isOffsetEnabled()) {
                            double offsetDuration = segment.getOffsetEnd() - segment.getOffsetStart();
                            duration -= offsetDuration;
                        }
                        
                        totalFrames += (int) (duration * frameRate);
                    }
                    
                    int processedFrames = 0;
                    
                    // Traiter chaque segment
                    for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
                        VideoSegment segment = segments.get(segIdx);
                        publish(String.format("Traitement du segment %d/%d: %s", 
                                            segIdx + 1, segments.size(), 
                                            segment.getVideoFile().getName()));
                        
                        processedFrames = processSegment(
                            segment, recorder, frameRate, totalFrames, processedFrames);
                    }
                    
                    publish("Finalisation...");
                    recorder.stop();
                    recorder.release();
                    
                    return true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Erreur: " + e.getMessage());
                    if (recorder != null) {
                        try {
                            recorder.stop();
                            recorder.release();
                        } catch (Exception ex) {}
                    }
                    return false;
                }
            }
            
            private int processSegment(VideoSegment segment, FFmpegFrameRecorder recorder,
                                      double frameRate, int totalFrames, int processedFrames) 
                    throws Exception {
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                grabber.start();
                
                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                
                long startTimestamp = (long) (segment.getStartTime() * 1_000_000);
                long endTimestamp = (long) (segment.getEndTime() * 1_000_000);
                
                // Calculer les zones d'offset si activées
                long offsetStartTimestamp = 0;
                long offsetEndTimestamp = 0;
                if (segment.isOffsetEnabled()) {
                    offsetStartTimestamp = (long) (segment.getOffsetStart() * 1_000_000);
                    offsetEndTimestamp = (long) (segment.getOffsetEnd() * 1_000_000);
                }
                
                grabber.setTimestamp(startTimestamp);
                
                while (true) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;
                    
                    long timestamp = grabber.getTimestamp();
                    if (timestamp > endTimestamp) break;
                    
                    // Exclure les frames dans la zone d'offset (dark frames)
                    if (segment.isOffsetEnabled() && 
                        timestamp >= offsetStartTimestamp && 
                        timestamp <= offsetEndTimestamp) {
                        continue; // Skip cette frame
                    }
                    
                    // Appliquer la luminosité si nécessaire
                    if (brightnessMultiplier != 1.0 && frame.image != null) {
                        org.bytedeco.opencv.opencv_core.Mat mat = matConverter.convert(frame);
                        if (mat != null) {
                            mat.convertTo(mat, -1, brightnessMultiplier, 0);
                            frame = matConverter.convert(mat);
                            mat.release();
                        }
                    }
                    
                    recorder.record(frame);
                    processedFrames++;
                    
                    if (processedFrames % 10 == 0) {
                        int percent = (int) ((processedFrames / (double) totalFrames) * 100);
                        publish(String.format("Progression: %d%%", percent));
                    }
                }
                
                grabber.stop();
                grabber.release();
                matConverter.close();
                
                return processedFrames;
            }
            
            @Override
            protected void process(List<String> chunks) {
                if (listener != null && !chunks.isEmpty()) {
                    String lastMessage = chunks.get(chunks.size() - 1);
                    if (lastMessage.contains("%")) {
                        try {
                            int percent = Integer.parseInt(
                                lastMessage.replaceAll("[^0-9]", ""));
                            listener.onProgress(percent, lastMessage);
                        } catch (NumberFormatException e) {
                            listener.onProgress(0, lastMessage);
                        }
                    } else {
                        listener.onProgress(-1, lastMessage);
                    }
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (listener != null) {
                        if (success) {
                            listener.onComplete(true, "Export réussi!");
                        } else {
                            listener.onComplete(false, "Échec de l'export");
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
