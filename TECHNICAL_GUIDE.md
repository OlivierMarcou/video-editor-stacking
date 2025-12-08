# Guide Technique Avancé

## Architecture et Design Patterns

### Structure du Projet

```
fr.videoeditor/
├── model/              # Modèle de données
│   └── VideoSegment   # Représente un segment de vidéo
├── ui/                # Interface utilisateur
│   ├── VideoEditorFrame     # Fenêtre principale (Controller)
│   ├── TimelinePanel        # Vue de la timeline
│   └── VideoPreviewPanel    # Prévisualisation vidéo
├── export/            # Logique d'export
│   └── VideoExporter  # Gestion export et stacking
└── examples/          # Exemples d'utilisation
    └── ProgrammaticExample
```

### Patterns Utilisés

1. **MVC (Model-View-Controller)**
   - Model: `VideoSegment`
   - View: `TimelinePanel`, `VideoPreviewPanel`
   - Controller: `VideoEditorFrame`

2. **Observer Pattern**
   - `ProgressListener` pour les notifications d'export

3. **SwingWorker**
   - Opérations asynchrones pour ne pas bloquer l'UI

## Détails d'Implémentation

### 1. Gestion de la Timeline

#### Calcul des Positions
```java
private int timeToX(double time) {
    double totalDuration = getTotalDuration();
    return (int) ((time / totalDuration) * getWidth());
}

private double xToTime(int x) {
    double totalDuration = getTotalDuration();
    return (x / (double) getWidth()) * totalDuration;
}
```

#### Gestion des Poignées
- **Handle Start (0)**: Modifie `startTime` du segment
- **Handle End (1)**: Modifie `endTime` du segment
- **Drag Detection**: Distance < 10 pixels

### 2. Algorithme de Stacking

Le stacking utilise OpenCV pour moyenner les frames:

```java
// Conversion en float pour éviter overflow
Mat result = frames.get(0).clone();
result.convertTo(result, CV_32F);

// Addition de toutes les frames
for (Mat frame : frames) {
    add(result, frame, result);
}

// Division par le nombre de frames
divide(result, Scalar.all(frames.size()), result);

// Reconversion en 8-bit
result.convertTo(output, CV_8U);
```

**Avantages:**
- Augmente le rapport signal/bruit
- Réduit le bruit aléatoire
- Améliore la visibilité dans les zones sombres

**Limitations:**
- Nécessite des images **complètement fixes**
- Augmente le temps de traitement
- Utilise plus de mémoire

### 3. Export Vidéo

#### Paramètres H.264
```java
recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
recorder.setVideoBitrate(8000000);  // 8 Mbps
```

#### Paramètres Audio AAC
```java
recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
recorder.setAudioBitrate(192000);   // 192 kbps
```

#### Synchronisation Audio/Vidéo
```java
// Traiter vidéo
Frame videoFrame = grabber.grabImage();
recorder.record(videoFrame);

// Traiter audio
Frame audioFrame = grabber.grabSamples();
if (audioFrame != null) {
    recorder.record(audioFrame);
}
```

## Optimisations Possibles

### 1. Performance

#### Utilisation de la Mémoire
```bash
# Augmenter la heap Java pour grosses vidéos
java -Xmx8G -jar video-editor.jar
```

#### Traitement Multi-thread
Le stacking pourrait être parallélisé:
```java
ExecutorService executor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);
```

#### Cache des Thumbnails
Pré-générer des miniatures pour la timeline:
```java
private Map<VideoSegment, BufferedImage[]> thumbnailCache;
```

### 2. Fonctionnalités Additionnelles

#### a) Transitions
```java
public enum TransitionType {
    NONE, FADE, DISSOLVE, WIPE
}

private void applyTransition(Mat frame1, Mat frame2, 
                             TransitionType type, double progress) {
    switch (type) {
        case FADE:
            addWeighted(frame1, 1-progress, frame2, progress, 0, result);
            break;
        // ...
    }
}
```

#### b) Filtres Vidéo
```java
public void applyFilter(Mat frame, FilterType filter) {
    switch (filter) {
        case GRAYSCALE:
            cvtColor(frame, frame, COLOR_BGR2GRAY);
            break;
        case BRIGHTNESS:
            frame.convertTo(frame, -1, 1.0, brightness);
            break;
        case CONTRAST:
            frame.convertTo(frame, -1, contrast, 0);
            break;
    }
}
```

#### c) Ondulation Audio
Visualiser l'audio dans la timeline:
```java
private void drawWaveform(Graphics2D g2d, VideoSegment segment) {
    // Extraire échantillons audio
    // Dessiner forme d'onde
}
```

### 3. Interface Utilisateur

#### Zoom sur la Timeline
```java
private double zoomLevel = 1.0;

private int timeToX(double time) {
    return (int) ((time / totalDuration) * getWidth() * zoomLevel);
}
```

#### Marqueurs de Temps
```java
private List<TimeMarker> markers = new ArrayList<>();

class TimeMarker {
    double time;
    String label;
    Color color;
}
```

#### Prévisualisation en Temps Réel
```java
private Timer playbackTimer;

private void playPreview() {
    playbackTimer = new Timer(33, e -> {  // 30 FPS
        currentTime += 0.033;
        updatePreview(currentTime);
    });
    playbackTimer.start();
}
```

## Gestion des Erreurs

### 1. Erreurs de Codec
```java
try {
    grabber.start();
} catch (FFmpegFrameGrabber.Exception e) {
    if (e.getMessage().contains("codec")) {
        // Suggérer conversion du fichier
        showCodecErrorDialog();
    }
}
```

### 2. Mémoire Insuffisante
```java
try {
    Mat stackedMat = stackImages(frames);
} catch (OutOfMemoryError e) {
    // Réduire le nombre de frames
    frames = frames.subList(0, frames.size() / 2);
    retry();
}
```

### 3. Fichier Corrompu
```java
if (grabber.getLengthInFrames() <= 0) {
    throw new InvalidVideoException("Fichier vidéo corrompu");
}
```

## Formats Supportés

### Lecture (via FFmpeg)
- **Vidéo:** MP4, AVI, MOV, MKV, FLV, WMV, WEBM
- **Codecs:** H.264, H.265, VP8, VP9, MPEG-4, etc.
- **Audio:** MP3, AAC, FLAC, WAV, OGG, etc.

### Export
- **Format:** MP4 (conteneur)
- **Vidéo:** H.264
- **Audio:** AAC

### Ajouter Support d'Autres Formats
```java
public void exportAs(File output, String format) {
    recorder.setFormat(format);
    switch (format) {
        case "avi":
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            break;
        case "mov":
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            break;
        // ...
    }
}
```

## Tests Unitaires

### Exemple de Test
```java
@Test
public void testVideoSegmentDuration() {
    VideoSegment segment = new VideoSegment(
        new File("test.mp4"), 60.0
    );
    segment.setStartTime(10.0);
    segment.setEndTime(30.0);
    
    assertEquals(20.0, segment.getSegmentDuration(), 0.01);
}

@Test
public void testStackingFrames() {
    List<Mat> frames = createTestFrames(10);
    Mat result = stackImages(frames);
    
    assertNotNull(result);
    assertEquals(CV_8U, result.type());
}
```

## Profiling et Debug

### Activer les Logs FFmpeg
```java
FFmpegLogCallback.set();
```

### Mesurer les Performances
```java
long startTime = System.nanoTime();
exportVideo(segments, output, listener);
long duration = System.nanoTime() - startTime;

System.out.println("Export temps: " + 
    (duration / 1_000_000_000.0) + "s");
```

### Débug de la Timeline
```java
private void debugTimeline() {
    double currentTime = 0;
    for (VideoSegment seg : segments) {
        System.out.printf("Segment: %s [%.2f - %.2f] @ %.2f%n",
            seg.getVideoFile().getName(),
            seg.getStartTime(),
            seg.getEndTime(),
            currentTime);
        currentTime += seg.getSegmentDuration();
    }
}
```

## Considérations de Sécurité

### 1. Validation des Fichiers
```java
private boolean isValidVideoFile(File file) {
    String[] validExtensions = {".mp4", ".avi", ".mov", ".mkv"};
    String filename = file.getName().toLowerCase();
    
    return Arrays.stream(validExtensions)
        .anyMatch(filename::endsWith);
}
```

### 2. Limitation de Taille
```java
private static final long MAX_FILE_SIZE = 5_000_000_000L; // 5 GB

if (file.length() > MAX_FILE_SIZE) {
    throw new FileTooLargeException(
        "Fichier trop volumineux: " + file.length()
    );
}
```

### 3. Chemin d'Export Sûr
```java
private File sanitizeOutputPath(File file) {
    // Éviter les injections de chemin
    String name = file.getName()
        .replaceAll("[^a-zA-Z0-9.-]", "_");
    return new File(file.getParent(), name);
}
```

## Ressources Supplémentaires

### Documentation
- **JavaCV:** https://github.com/bytedeco/javacv
- **FFmpeg:** https://ffmpeg.org/documentation.html
- **OpenCV:** https://docs.opencv.org/

### Tutoriels
- Processing video with JavaCV
- FFmpeg encoding guide
- Image stacking algorithms

### Communauté
- Stack Overflow - tag `javacv`
- ByteDeco GitHub Issues
- FFmpeg mailing list
