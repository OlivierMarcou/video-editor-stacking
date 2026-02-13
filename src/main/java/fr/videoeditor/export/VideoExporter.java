package fr.videoeditor.export;

import fr.videoeditor.model.VideoSegment;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import org.bytedeco.ffmpeg.global.avutil;
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
                                   double brightnessMultiplier, boolean applyOffsetProcessing,
                                   ProgressListener listener) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                FFmpegFrameRecorder recorder = null;
                Java2DFrameConverter converter = new Java2DFrameConverter();
                Mat masterDark = null;
                
                try {
                    VideoSegment firstSegment = segments.get(0);
                    FFmpegFrameGrabber firstGrabber = new FFmpegFrameGrabber(firstSegment.getVideoFile());
                    firstGrabber.start();
                    
                    int width = firstGrabber.getImageWidth();
                    int height = firstGrabber.getImageHeight();
                    double frameRate = firstGrabber.getFrameRate();
                    int audioChannels = firstGrabber.getAudioChannels();
                    
                    publish("Initialisation de l'export...");
                    
                    // Calculer le master dark si traitement offset activé
                    if (applyOffsetProcessing) {
                        publish("Calcul du Master Dark...");
                        masterDark = calculateMasterDark(segments, width, height);
                        if (masterDark != null) {
                            publish("Master Dark calculé");
                        }
                    }
                    
                    recorder = new FFmpegFrameRecorder(outputFile, width, height, audioChannels);
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    recorder.setVideoBitrate(8000000);
                    
                    if (audioChannels > 0) {
                        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                        recorder.setAudioBitrate(192000);
                        recorder.setSampleRate(firstGrabber.getSampleRate());
                    }
                    
                    recorder.start();
                    firstGrabber.stop();
                    
                    int totalFrames = 0;
                    for (VideoSegment segment : segments) {
                        double duration = segment.getSegmentDuration();
                        if (segment.isOffsetEnabled()) {
                            double offsetDuration = segment.getOffsetEnd() - segment.getOffsetStart();
                            duration -= offsetDuration;
                        }
                        totalFrames += (int) (duration * frameRate);
                    }
                    
                    int processedFrames = 0;
                    
                    for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
                        VideoSegment segment = segments.get(segIdx);
                        publish(String.format("Segment %d/%d: %s", 
                                            segIdx + 1, segments.size(), 
                                            segment.getVideoFile().getName()));
                        
                        processedFrames = processSegmentMP4(segment, recorder, frameRate, 
                                                          totalFrames, processedFrames, 
                                                          brightnessMultiplier, masterDark);
                    }
                    
                    publish("Finalisation...");
                    recorder.stop();
                    recorder.release();
                    
                    if (masterDark != null) {
                        masterDark.release();
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    if (recorder != null) {
                        try { recorder.stop(); recorder.release(); } catch (Exception ex) {}
                    }
                    if (masterDark != null) {
                        try { masterDark.release(); } catch (Exception ex) {}
                    }
                    return false;
                }
            }
            
            private Mat calculateMasterDark(List<VideoSegment> segments, int width, int height) {
                try {
                    Mat accumulator = new Mat(height, width, CV_32FC3, Scalar.all(0));
                    int frameCount = 0;
                    
                    for (VideoSegment segment : segments) {
                        if (!segment.isOffsetEnabled()) continue;
                        
                        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                        grabber.start();
                        
                        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                        
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
                                mat.convertTo(mat, CV_32FC3);
                                add(accumulator, mat, accumulator);
                                mat.release();
                                frameCount++;
                            }
                        }
                        
                        grabber.stop();
                        grabber.release();
                        matConverter.close();
                    }
                    
                    if (frameCount > 0) {
                        // Diviser par frameCount
                        accumulator.convertTo(accumulator, -1, 1.0 / frameCount, 0);
                        return accumulator;
                    }
                    
                    accumulator.release();
                    return null;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            private int processSegmentMP4(VideoSegment segment, FFmpegFrameRecorder recorder,
                                        double frameRate, int totalFrames, int processedFrames,
                                        double brightnessMultiplier, Mat masterDark) throws Exception {
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                grabber.start();
                
                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                
                long startTimestamp = (long) (segment.getStartTime() * 1_000_000);
                long endTimestamp = (long) (segment.getEndTime() * 1_000_000);
                
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
                    
                    if (segment.isOffsetEnabled() && 
                        timestamp >= offsetStartTimestamp && 
                        timestamp <= offsetEndTimestamp) {
                        continue;
                    }
                    
                    if (frame.image != null) {
                        Mat mat = matConverter.convert(frame);
                        if (mat != null) {
                            // Soustraire master dark
                            if (masterDark != null) {
                                mat.convertTo(mat, CV_32FC3);
                                subtract(mat, masterDark, mat);
                                mat.convertTo(mat, CV_8UC3);
                            }
                            
                            // Appliquer luminosité
                            if (brightnessMultiplier != 1.0) {
                                mat.convertTo(mat, -1, brightnessMultiplier, 0);
                            }
                            
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
    
    /**
     * Exporte la vidéo en AVI sans perte de qualité (codec FFV1)
     */
    public static void exportVideoAVI(List<VideoSegment> segments, File outputFile, 
                                      double brightnessMultiplier, boolean applyOffsetProcessing,
                                      ProgressListener listener) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                FFmpegFrameRecorder recorder = null;
                Java2DFrameConverter converter = new Java2DFrameConverter();
                Mat masterDark = null;
                
                try {
                    VideoSegment firstSegment = segments.get(0);
                    FFmpegFrameGrabber firstGrabber = new FFmpegFrameGrabber(firstSegment.getVideoFile());
                    firstGrabber.start();
                    
                    int width = firstGrabber.getImageWidth();
                    int height = firstGrabber.getImageHeight();
                    double frameRate = firstGrabber.getFrameRate();
                    int audioChannels = firstGrabber.getAudioChannels();
                    int sampleRate = firstGrabber.getSampleRate();
                    
                    publish("Initialisation de l'export AVI sans perte...");
                    
                    // IMPORTANT: Traitement offset/luminosité désactivé pour AVI
                    // pour éviter les crashs mémoire avec conversion Mat
                    if (applyOffsetProcessing) {
                        publish("Note: Traitement offset ignoré pour export AVI (stabilité)");
                    }
                    if (brightnessMultiplier != 1.0) {
                        publish("Note: Luminosité ignorée pour export AVI (stabilité)");
                    }
                    
                    recorder = new FFmpegFrameRecorder(outputFile, width, height, audioChannels);
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_HUFFYUV);
                    recorder.setFormat("avi");
                    recorder.setFrameRate(frameRate);
                    recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV422P);
                    
                    if (audioChannels > 0) {
                        recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
                        recorder.setSampleRate(sampleRate);
                    }
                    
                    recorder.start();
                    firstGrabber.stop();
                    
                    publish("Codec: HuffYUV YUV422P (lossless), Audio: PCM");
                    
                    int totalFrames = 0;
                    for (VideoSegment segment : segments) {
                        double duration = segment.getSegmentDuration();
                        if (segment.isOffsetEnabled()) {
                            double offsetDuration = segment.getOffsetEnd() - segment.getOffsetStart();
                            duration -= offsetDuration;
                        }
                        totalFrames += (int) (duration * frameRate);
                    }
                    
                    int processedFrames = 0;
                    
                    for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
                        VideoSegment segment = segments.get(segIdx);
                        publish(String.format("Segment %d/%d: %s", 
                                            segIdx + 1, segments.size(), 
                                            segment.getVideoFile().getName()));
                        
                        processedFrames = processSegmentAVI(segment, recorder, frameRate, 
                                                          totalFrames, processedFrames, 
                                                          1.0, null); // Pas de traitement pour AVI
                    }
                    
                    publish("Finalisation de la vidéo AVI...");
                    recorder.stop();
                    recorder.release();
                    converter.close();
                    
                    if (masterDark != null) {
                        masterDark.release();
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    if (recorder != null) {
                        try { recorder.release(); } catch (Exception ex) {}
                    }
                    if (masterDark != null) {
                        try { masterDark.release(); } catch (Exception ex) {}
                    }
                    throw e;
                } finally {
                    converter.close();
                }
            }
            
            private Mat calculateMasterDark(List<VideoSegment> segments, int width, int height) {
                try {
                    Mat accumulator = new Mat(height, width, CV_32FC3, Scalar.all(0));
                    int frameCount = 0;
                    
                    for (VideoSegment segment : segments) {
                        if (!segment.isOffsetEnabled()) continue;
                        
                        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                        grabber.start();
                        
                        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                        
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
                                mat.convertTo(mat, CV_32FC3);
                                add(accumulator, mat, accumulator);
                                mat.release();
                                frameCount++;
                            }
                        }
                        
                        grabber.stop();
                        grabber.release();
                        matConverter.close();
                    }
                    
                    if (frameCount > 0) {
                        // Diviser par frameCount
                        accumulator.convertTo(accumulator, -1, 1.0 / frameCount, 0);
                        return accumulator;
                    }
                    
                    accumulator.release();
                    return null;
                    
                } catch (Exception e) {
                    return null;
                }
            }
            
            private int processSegmentAVI(VideoSegment segment, FFmpegFrameRecorder recorder,
                                        double frameRate, int totalFrames, int processedFrames,
                                        double brightnessMultiplier, Mat masterDark) throws Exception {
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(segment.getVideoFile());
                grabber.start();
                
                long startTimestamp = (long) (segment.getStartTime() * 1_000_000);
                long endTimestamp = (long) (segment.getEndTime() * 1_000_000);
                
                long offsetStartTimestamp = 0;
                long offsetEndTimestamp = 0;
                if (segment.isOffsetEnabled()) {
                    offsetStartTimestamp = (long) (segment.getOffsetStart() * 1_000_000);
                    offsetEndTimestamp = (long) (segment.getOffsetEnd() * 1_000_000);
                }
                
                grabber.setTimestamp(startTimestamp);
                
                // IMPORTANT: Pour AVI lossless, on ne fait AUCUN traitement Mat
                // On passe les frames directement pour éviter les crashs
                
                while (true) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;
                    
                    long timestamp = grabber.getTimestamp();
                    if (timestamp > endTimestamp) break;
                    
                    if (segment.isOffsetEnabled() && 
                        timestamp >= offsetStartTimestamp && 
                        timestamp <= offsetEndTimestamp) {
                        continue;
                    }
                    
                    // Passer la frame directement SANS conversion Mat
                    // pour éviter les crashs de mémoire
                    recorder.record(frame);
                    processedFrames++;
                    
                    if (processedFrames % 10 == 0) {
                        int percent = (int) ((processedFrames / (double) totalFrames) * 100);
                        publish(String.format("Progression: %d%%", percent));
                    }
                }
                
                grabber.stop();
                grabber.release();
                
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
                            listener.onComplete(true, "Export AVI sans perte réussi!");
                        } else {
                            listener.onComplete(false, "Échec de l'export AVI");
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
