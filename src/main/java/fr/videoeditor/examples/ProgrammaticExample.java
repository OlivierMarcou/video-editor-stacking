package fr.videoeditor.examples;

import fr.videoeditor.export.VideoExporter;
import fr.videoeditor.model.VideoSegment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Exemple d'utilisation de l'éditeur vidéo en mode programmatique
 * (sans interface graphique)
 */
public class ProgrammaticExample {
    
    public static void main(String[] args) {
        // Exemple 1: Découper une seule vidéo
        example1_CutSingleVideo();
        
        // Exemple 2: Concaténer plusieurs vidéos
        // example2_ConcatenateVideos();
        
        // Exemple 3: Appliquer le stacking sur une zone sombre
        // example3_StackingOnDarkArea();
    }
    
    /**
     * Exemple 1: Découper une vidéo de 0 à 30 secondes
     */
    private static void example1_CutSingleVideo() {
        System.out.println("=== Exemple 1: Découpage simple ===");
        
        // Fichier d'entrée (à adapter selon votre système)
        File inputFile = new File("input_video.mp4");
        
        if (!inputFile.exists()) {
            System.err.println("Le fichier " + inputFile + " n'existe pas!");
            return;
        }
        
        // Créer un segment de 60 secondes (durée totale de la vidéo)
        VideoSegment segment = new VideoSegment(inputFile, 60.0);
        
        // Définir le découpage: de 0 à 30 secondes
        segment.setStartTime(0);
        segment.setEndTime(30);
        
        // Créer la liste de segments
        List<VideoSegment> segments = new ArrayList<>();
        segments.add(segment);
        
        // Fichier de sortie
        File outputFile = new File("output_cut.mp4");
        
        // Exporter
        System.out.println("Export en cours...");
        VideoExporter.exportVideo(segments, outputFile, new VideoExporter.ProgressListener() {
            @Override
            public void onProgress(int percent, String message) {
                if (percent >= 0) {
                    System.out.println("Progression: " + percent + "% - " + message);
                }
            }
            
            @Override
            public void onComplete(boolean success, String message) {
                if (success) {
                    System.out.println("✅ Export réussi: " + outputFile.getAbsolutePath());
                } else {
                    System.err.println("❌ Échec: " + message);
                }
            }
        });
        
        // Attendre la fin de l'export
        try {
            Thread.sleep(60000); // Attendre max 1 minute
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Exemple 2: Concaténer plusieurs vidéos
     */
    private static void example2_ConcatenateVideos() {
        System.out.println("=== Exemple 2: Concaténation ===");
        
        // Première vidéo: prendre de 10 à 20 secondes
        File video1 = new File("video1.mp4");
        VideoSegment segment1 = new VideoSegment(video1, 30.0);
        segment1.setStartTime(10);
        segment1.setEndTime(20);
        
        // Deuxième vidéo: prendre de 5 à 15 secondes
        File video2 = new File("video2.mp4");
        VideoSegment segment2 = new VideoSegment(video2, 25.0);
        segment2.setStartTime(5);
        segment2.setEndTime(15);
        
        // Troisième vidéo: prendre entièrement
        File video3 = new File("video3.mp4");
        VideoSegment segment3 = new VideoSegment(video3, 10.0);
        
        // Créer la liste
        List<VideoSegment> segments = new ArrayList<>();
        segments.add(segment1);
        segments.add(segment2);
        segments.add(segment3);
        
        // Fichier de sortie
        File outputFile = new File("output_concatenated.mp4");
        
        // Exporter
        VideoExporter.exportVideo(segments, outputFile, new VideoExporter.ProgressListener() {
            @Override
            public void onProgress(int percent, String message) {
                System.out.println(message);
            }
            
            @Override
            public void onComplete(boolean success, String message) {
                System.out.println(success ? "✅ " + message : "❌ " + message);
            }
        });
    }
    
    /**
     * Exemple 3: Appliquer le stacking sur une zone sombre
     */
    private static void example3_StackingOnDarkArea() {
        System.out.println("=== Exemple 3: Stacking d'images ===");
        
        // Vidéo de nuit avec une scène fixe
        File nightVideo = new File("night_video.mp4");
        VideoSegment segment = new VideoSegment(nightVideo, 60.0);
        
        // On veut toute la vidéo
        segment.setStartTime(0);
        segment.setEndTime(60);
        
        // Activer le stacking de 10s à 20s (zone fixe et sombre)
        segment.setStackingEnabled(true);
        segment.setStackingStart(10.0); // Début relatif au segment
        segment.setStackingEnd(20.0);   // Fin relative au segment
        
        List<VideoSegment> segments = new ArrayList<>();
        segments.add(segment);
        
        File outputFile = new File("output_stacked.mp4");
        
        System.out.println("Début de l'export avec stacking...");
        System.out.println("Zone de stacking: 10s à 20s");
        
        VideoExporter.exportVideo(segments, outputFile, new VideoExporter.ProgressListener() {
            @Override
            public void onProgress(int percent, String message) {
                System.out.println(message);
            }
            
            @Override
            public void onComplete(boolean success, String message) {
                if (success) {
                    System.out.println("✅ Vidéo stackée exportée avec succès!");
                    System.out.println("La zone 10-20s devrait être plus lumineuse.");
                } else {
                    System.err.println("❌ Erreur: " + message);
                }
            }
        });
    }
    
    /**
     * Exemple 4: Scénario complexe avec plusieurs segments et stacking
     */
    private static void example4_ComplexScenario() {
        System.out.println("=== Exemple 4: Scénario complexe ===");
        
        List<VideoSegment> segments = new ArrayList<>();
        
        // Segment 1: Introduction claire (pas de stacking)
        VideoSegment intro = new VideoSegment(new File("intro.mp4"), 15.0);
        intro.setStartTime(2);
        intro.setEndTime(12);
        segments.add(intro);
        
        // Segment 2: Scène de nuit avec stacking
        VideoSegment night = new VideoSegment(new File("night_scene.mp4"), 30.0);
        night.setStartTime(5);
        night.setEndTime(25);
        night.setStackingEnabled(true);
        night.setStackingStart(0);  // Dès le début du segment
        night.setStackingEnd(20);   // Sur toute la durée
        segments.add(night);
        
        // Segment 3: Conclusion claire
        VideoSegment outro = new VideoSegment(new File("outro.mp4"), 20.0);
        outro.setStartTime(0);
        outro.setEndTime(10);
        segments.add(outro);
        
        File outputFile = new File("output_complex.mp4");
        
        System.out.println("Configuration:");
        System.out.println("- Intro: 10s");
        System.out.println("- Scène de nuit avec stacking: 20s");
        System.out.println("- Outro: 10s");
        System.out.println("Total: ~40 secondes");
        
        VideoExporter.exportVideo(segments, outputFile, new VideoExporter.ProgressListener() {
            private long startTime = System.currentTimeMillis();
            
            @Override
            public void onProgress(int percent, String message) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                System.out.printf("[%ds] %s%n", elapsed, message);
            }
            
            @Override
            public void onComplete(boolean success, String message) {
                long totalTime = (System.currentTimeMillis() - startTime) / 1000;
                System.out.println();
                System.out.println("=================================");
                System.out.println(success ? "✅ " + message : "❌ " + message);
                System.out.println("Temps total: " + totalTime + "s");
                System.out.println("=================================");
            }
        });
    }
}
