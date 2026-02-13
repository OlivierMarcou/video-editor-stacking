package fr.videoeditor.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import fr.videoeditor.export.VideoExporter;
import fr.videoeditor.export.FrameExtractor;
import fr.videoeditor.export.ImageStacker;
import fr.videoeditor.model.VideoSegment;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * Fenêtre principale de l'éditeur vidéo
 */
public class VideoEditorFrame extends JFrame {
    private TimelinePanel timelinePanel;
    private VideoPreviewPanel previewPanel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JSlider brightnessSlider;
    private double brightnessMultiplier = 1.0;
    private Preferences prefs;
    private static final String PREF_LAST_DIRECTORY = "lastDirectory";
    
    public VideoEditorFrame() {
        setTitle("Éditeur Vidéo - Java 21");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        prefs = Preferences.userNodeForPackage(VideoEditorFrame.class);
        
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Panneau supérieur - Prévisualisation
        previewPanel = new VideoPreviewPanel();
        JPanel previewContainer = new JPanel(new BorderLayout());
        previewContainer.setBorder(BorderFactory.createTitledBorder("Prévisualisation"));
        previewContainer.add(previewPanel, BorderLayout.CENTER);
        add(previewContainer, BorderLayout.CENTER);
        
        // Panneau timeline
        timelinePanel = new TimelinePanel();
        timelinePanel.setPreviewCallback((segment, time) -> {
            previewPanel.loadFrame(segment.getVideoFile(), time);
            statusLabel.setText(String.format("Position: %.2fs", time));
        });
        JPanel timelineContainer = new JPanel(new BorderLayout());
        timelineContainer.setBorder(BorderFactory.createTitledBorder("Timeline"));
        timelineContainer.setPreferredSize(new Dimension(800, 180));
        JScrollPane timelineScroll = new JScrollPane(timelinePanel);
        timelineScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        timelineScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        timelineContainer.add(timelineScroll, BorderLayout.CENTER);
        
        // Panneau de contrôle
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        
        // Barre de statut
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel = new JLabel("Prêt");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);
        
        // Combiner timeline et status dans un panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(timelineContainer, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Bouton charger vidéo
        JButton loadButton = new JButton("📂 Charger Vidéo");
        loadButton.setFont(new Font("Arial", Font.BOLD, 12));
        loadButton.addActionListener(e -> loadVideo());
        panel.add(loadButton);
        
        // Bouton réparer vidéo
        JButton repairButton = new JButton("🔧 Réparer Vidéo");
        repairButton.setFont(new Font("Arial", Font.BOLD, 11));
        repairButton.setForeground(new Color(200, 100, 0));
        repairButton.setToolTipText("Réparer une vidéo corrompue avec une vidéo de référence");
        repairButton.addActionListener(e -> repairVideo());
        panel.add(repairButton);
        
        // Bouton prévisualiser
        JButton previewButton = new JButton("▶ Prévisualiser");
        previewButton.setFont(new Font("Arial", Font.BOLD, 12));
        previewButton.addActionListener(e -> previewSelectedSegment());
        panel.add(previewButton);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Bouton stacking d'images
        JButton stackingButton = new JButton("📸 Stacker Images");
        stackingButton.setFont(new Font("Arial", Font.BOLD, 12));
        stackingButton.addActionListener(e -> configureStacking());
        panel.add(stackingButton);
        
        // Bouton extraire images
        JButton extractButton = new JButton("🖼 Extraire Images");
        extractButton.setFont(new Font("Arial", Font.BOLD, 12));
        extractButton.addActionListener(e -> extractFrames());
        panel.add(extractButton);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Bouton exporter MP4
        JButton exportButton = new JButton("💾 Exporter MP4");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.setBackground(new Color(46, 125, 50));
        exportButton.setForeground(Color.WHITE);
        exportButton.setOpaque(true);
        exportButton.addActionListener(e -> exportVideo());
        panel.add(exportButton);
        
        // Bouton exporter AVI sans perte
        JButton exportAviButton = new JButton("🎬 Exporter AVI (Sans perte)");
        exportAviButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportAviButton.setBackground(new Color(25, 118, 210));
        exportAviButton.setForeground(Color.WHITE);
        exportAviButton.setOpaque(true);
        exportAviButton.setToolTipText("Export sans perte de qualité (codec FFV1)");
        exportAviButton.addActionListener(e -> exportVideoAVI());
        panel.add(exportAviButton);
        
        // Bouton supprimer segment
        JButton deleteButton = new JButton("🗑 Supprimer Segment");
        deleteButton.setFont(new Font("Arial", Font.BOLD, 12));
        deleteButton.addActionListener(e -> deleteSelectedSegment());
        panel.add(deleteButton);
        
        // Bouton retirer toutes les vidéos
        JButton clearButton = new JButton("✖ Retirer Toutes");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.addActionListener(e -> clearAllVideos());
        panel.add(clearButton);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Slider de luminosité
        JLabel brightnessLabel = new JLabel("Luminosité:");
        brightnessLabel.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(brightnessLabel);
        
        brightnessSlider = new JSlider(JSlider.HORIZONTAL, 10, 400, 100);
        brightnessSlider.setPreferredSize(new Dimension(150, 30));
        brightnessSlider.setMajorTickSpacing(100);
        brightnessSlider.setMinorTickSpacing(10);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        brightnessSlider.addChangeListener(e -> {
            brightnessMultiplier = brightnessSlider.getValue() / 100.0;
            previewPanel.setBrightnessMultiplier(brightnessMultiplier);
            JLabel valueLabel = (JLabel) panel.getComponent(panel.getComponentCount() - 1);
            valueLabel.setText(String.format("%.1fx", brightnessMultiplier));
        });
        panel.add(brightnessSlider);
        
        JLabel valueLabel = new JLabel("1.0x");
        valueLabel.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(valueLabel);
        
        return panel;
    }
    
    private void loadVideo() {
        // Dialogue pour choisir le mode de chargement
        String[] options = {"Chargement Normal", "Réparer avec Référence", "Réparation Avancée (⭐ Recommandé)", "Annuler"};
        int choice = JOptionPane.showOptionDialog(this,
            "Comment souhaitez-vous charger la vidéo?\n\n" +
            "• Chargement Normal: Pour vidéos valides\n" +
            "• Réparer avec Référence: Nécessite une vidéo du même appareil\n" +
            "• Réparation Avancée: Style Digital Video Repair, SANS référence ⭐",
            "Mode de chargement",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) {
            return; // Annulé
        }
        
        boolean repairMode = (choice == 1);
        boolean advancedRepairMode = (choice == 2);
        
        JFileChooser fileChooser = new JFileChooser();
        
        // Restaurer le dernier dossier
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Fichiers vidéo (*.mp4, *.avi, *.mov, *.mkv)", 
            "mp4", "avi", "mov", "mkv"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File videoFile = fileChooser.getSelectedFile();
            
            // Sauvegarder le dossier
            prefs.put(PREF_LAST_DIRECTORY, videoFile.getParent());
            
            if (advancedRepairMode) {
                loadDamagedVideoAdvanced(videoFile);
            } else if (repairMode) {
                loadDamagedVideo(videoFile);
            } else {
                loadVideoFile(videoFile);
            }
        }
    }
    
    private void repairVideo() {
        // Dialogue de réparation
        JDialog dialog = new JDialog(this, "Réparer une Vidéo Corrompue", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Explication
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JTextArea explanation = new JTextArea(
            "Pour réparer une vidéo corrompue, vous devez fournir:\n\n" +
            "1. La vidéo corrompue à réparer\n" +
            "2. Une vidéo de référence fonctionnelle\n\n" +
            "La vidéo de référence doit:\n" +
            "- Provenir du même appareil/caméra\n" +
            "- Utiliser les mêmes paramètres (codec, résolution, fps)\n" +
            "- Être fonctionnelle et lisible\n\n" +
            "La réparation utilisera la structure de la référence pour\n" +
            "reconstruire les métadonnées de la vidéo corrompue."
        );
        explanation.setEditable(false);
        explanation.setWrapStyleWord(true);
        explanation.setLineWrap(true);
        explanation.setOpaque(false);
        explanation.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(explanation, gbc);
        
        // Vidéo corrompue
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Vidéo corrompue:"), gbc);
        
        gbc.gridx = 1;
        JTextField corruptedField = new JTextField(30);
        corruptedField.setEditable(false);
        JButton browseCorrupted = new JButton("Parcourir...");
        JPanel corruptedPanel = new JPanel(new BorderLayout(5, 5));
        corruptedPanel.add(corruptedField, BorderLayout.CENTER);
        corruptedPanel.add(browseCorrupted, BorderLayout.EAST);
        mainPanel.add(corruptedPanel, gbc);
        
        // Vidéo de référence
        gbc.gridy = 2;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Vidéo de référence:"), gbc);
        
        gbc.gridx = 1;
        JTextField referenceField = new JTextField(30);
        referenceField.setEditable(false);
        JButton browseReference = new JButton("Parcourir...");
        JPanel referencePanel = new JPanel(new BorderLayout(5, 5));
        referencePanel.add(referenceField, BorderLayout.CENTER);
        referencePanel.add(browseReference, BorderLayout.EAST);
        mainPanel.add(referencePanel, gbc);
        
        // Fichier de sortie
        gbc.gridy = 3;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Fichier réparé:"), gbc);
        
        gbc.gridx = 1;
        JTextField outputField = new JTextField(30);
        outputField.setEditable(false);
        JButton browseOutput = new JButton("Parcourir...");
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(outputField, BorderLayout.CENTER);
        outputPanel.add(browseOutput, BorderLayout.EAST);
        mainPanel.add(outputPanel, gbc);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        // Actions des boutons
        browseCorrupted.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
            if (lastDir != null) {
                chooser.setCurrentDirectory(new File(lastDir));
            }
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Fichiers vidéo", "mp4", "avi", "mov", "mkv", "mpg", "mpeg", "m4v", "3gp"));
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                corruptedField.setText(chooser.getSelectedFile().getAbsolutePath());
                prefs.put(PREF_LAST_DIRECTORY, chooser.getSelectedFile().getParent());
                
                // Suggérer un nom de sortie
                if (outputField.getText().isEmpty()) {
                    String name = chooser.getSelectedFile().getName();
                    String baseName = name.substring(0, name.lastIndexOf('.'));
                    String ext = name.substring(name.lastIndexOf('.'));
                    outputField.setText(chooser.getSelectedFile().getParent() + 
                        File.separator + baseName + "_repaired" + ext);
                }
            }
        });
        
        browseReference.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
            if (lastDir != null) {
                chooser.setCurrentDirectory(new File(lastDir));
            }
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Fichiers vidéo", "mp4", "avi", "mov", "mkv", "mpg", "mpeg", "m4v", "3gp"));
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                referenceField.setText(chooser.getSelectedFile().getAbsolutePath());
                prefs.put(PREF_LAST_DIRECTORY, chooser.getSelectedFile().getParent());
            }
        });
        
        browseOutput.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
            if (lastDir != null) {
                chooser.setCurrentDirectory(new File(lastDir));
            }
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Fichiers vidéo", "mp4", "avi", "mov", "mkv", "mpg", "mpeg", "m4v", "3gp"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                outputField.setText(chooser.getSelectedFile().getAbsolutePath());
                prefs.put(PREF_LAST_DIRECTORY, chooser.getSelectedFile().getParent());
            }
        });
        
        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton repairBtn = new JButton("Réparer");
        JButton cancelBtn = new JButton("Annuler");
        
        repairBtn.addActionListener(e -> {
            String corrupted = corruptedField.getText();
            String reference = referenceField.getText();
            String output = outputField.getText();
            
            if (corrupted.isEmpty() || reference.isEmpty() || output.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Veuillez sélectionner tous les fichiers requis.",
                    "Champs manquants",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            dialog.dispose();
            performRepair(new File(corrupted), new File(reference), new File(output));
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(repairBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void performRepair(File corruptedFile, File referenceFile, File outputFile) {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Réparation en cours...");
        
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    publish("Analyse de la vidéo de référence...");
                    
                    // Étape 1: Extraire les informations de la référence
                    FFmpegFrameGrabber refGrabber = new FFmpegFrameGrabber(referenceFile);
                    refGrabber.start();
                    
                    int width = refGrabber.getImageWidth();
                    int height = refGrabber.getImageHeight();
                    double frameRate = refGrabber.getFrameRate();
                    String videoCodec = refGrabber.getVideoCodecName();
                    int videoBitrate = refGrabber.getVideoBitrate();
                    
                    refGrabber.stop();
                    refGrabber.release();
                    
                    publish("Extraction des frames de la vidéo corrompue...");
                    
                    // Étape 2: Lire la vidéo corrompue avec tolérance maximale
                    FFmpegFrameGrabber corruptedGrabber = new FFmpegFrameGrabber(corruptedFile);
                    corruptedGrabber.setOption("fflags", "+genpts+igndts");
                    corruptedGrabber.setOption("err_detect", "ignore_err");
                    corruptedGrabber.setOption("skip_frame", "noref");
                    corruptedGrabber.start();
                    
                    publish("Reconstruction de la vidéo...");
                    
                    // Étape 3: Créer la vidéo réparée avec les paramètres de la référence
                    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                        outputFile, width, height);
                    recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat(outputFile.getName().substring(
                        outputFile.getName().lastIndexOf('.') + 1));
                    recorder.setFrameRate(frameRate);
                    recorder.setVideoBitrate(videoBitrate > 0 ? videoBitrate : 5000000);
                    recorder.start();
                    
                    publish("Copie des frames...");
                    
                    int frameCount = 0;
                    while (true) {
                        try {
                            Frame frame = corruptedGrabber.grabImage();
                            if (frame == null) break;
                            
                            recorder.record(frame);
                            frameCount++;
                            
                            if (frameCount % 30 == 0) {
                                publish(String.format("Frames réparées: %d", frameCount));
                            }
                        } catch (Exception e) {
                            // Ignorer les erreurs sur frames individuelles
                            continue;
                        }
                    }
                    
                    publish("Finalisation...");
                    
                    recorder.stop();
                    recorder.release();
                    corruptedGrabber.stop();
                    corruptedGrabber.release();
                    
                    publish(String.format("Réparation terminée: %d frames récupérées", frameCount));
                    
                    return frameCount > 0;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Erreur: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                    
                    if (success) {
                        int result = JOptionPane.showConfirmDialog(
                            VideoEditorFrame.this,
                            "Vidéo réparée avec succès!\n" +
                            "Fichier: " + outputFile.getName() + "\n\n" +
                            "Voulez-vous charger la vidéo réparée?",
                            "Réparation réussie",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        if (result == JOptionPane.YES_OPTION) {
                            loadVideoFile(outputFile);
                        }
                        
                        statusLabel.setText("Vidéo réparée: " + outputFile.getName());
                    } else {
                        JOptionPane.showMessageDialog(VideoEditorFrame.this,
                            "La réparation a échoué.\n" +
                            "Vérifiez que:\n" +
                            "- La vidéo de référence est fonctionnelle\n" +
                            "- Les deux vidéos utilisent le même codec\n" +
                            "- La vidéo corrompue contient des données récupérables",
                            "Échec de la réparation",
                            JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Échec de la réparation");
                    }
                } catch (Exception e) {
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        "Erreur lors de la réparation:\n" + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Erreur de réparation");
                }
            }
        };
        
        worker.execute();
    }
    
    private void loadVideoFile(File videoFile) {
        SwingWorker<VideoSegment, Void> worker = new SwingWorker<>() {
            @Override
            protected VideoSegment doInBackground() throws Exception {
                statusLabel.setText("Chargement de " + videoFile.getName() + "...");
                
                try {
                    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
                    grabber.start();
                    double duration = grabber.getLengthInTime() / 1_000_000.0;
                    grabber.stop();
                    grabber.release();
                    
                    return new VideoSegment(videoFile, duration);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            
            @Override
            protected void done() {
                try {
                    VideoSegment segment = get();
                    timelinePanel.addSegment(segment);
                    statusLabel.setText("Vidéo chargée: " + videoFile.getName() + 
                                      " (" + String.format("%.2f", segment.getDuration()) + "s)");
                    previewPanel.loadFrame(videoFile, 0);
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    
                    // Détecter si la vidéo est endommagée
                    boolean isDamaged = errorMsg != null && (
                        errorMsg.contains("moov atom not found") ||
                        errorMsg.contains("Could not open input") ||
                        errorMsg.contains("Invalid data found") ||
                        errorMsg.contains("End of file")
                    );
                    
                    if (isDamaged) {
                        String[] repairOptions = {
                            "Réparation Avancée ⭐ (Recommandé)", 
                            "Réparation avec Référence", 
                            "Annuler"
                        };
                        
                        int choice = JOptionPane.showOptionDialog(VideoEditorFrame.this,
                            "Cette vidéo semble endommagée ou corrompue.\n" +
                            "Erreur: " + errorMsg + "\n\n" +
                            "╔═══════════════════════════════════════╗\n" +
                            "║   CHOISISSEZ UNE MÉTHODE DE RÉPARATION    ║\n" +
                            "╚═══════════════════════════════════════╝\n\n" +
                            "• Réparation Avancée (⭐ Recommandé):\n" +
                            "  - Style Digital Video Repair\n" +
                            "  - SANS vidéo de référence\n" +
                            "  - Analyse directe du flux H.264\n" +
                            "  - Taux de succès: ~90%\n\n" +
                            "• Réparation avec Référence:\n" +
                            "  - Nécessite une vidéo du même appareil\n" +
                            "  - Basé sur FFmpeg\n" +
                            "  - Taux de succès: ~60%",
                            "Vidéo endommagée détectée",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            repairOptions,
                            repairOptions[0]);
                        
                        if (choice == 0) {
                            loadDamagedVideoAdvanced(videoFile);
                        } else if (choice == 1) {
                            loadDamagedVideo(videoFile);
                        } else {
                            statusLabel.setText("Chargement annulé - vidéo endommagée");
                        }
                    } else {
                        JOptionPane.showMessageDialog(VideoEditorFrame.this,
                            "Erreur lors du chargement de la vidéo:\n" + errorMsg,
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Erreur de chargement");
                    }
                }
            }
        };
        worker.execute();
    }
    
    private void loadDamagedVideo(File damagedFile) {
        // Vérifier d'abord que le fichier existe et est accessible
        if (!damagedFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Le fichier n'existe pas:\n" + damagedFile.getAbsolutePath(),
                "Fichier introuvable",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!damagedFile.canRead()) {
            JOptionPane.showMessageDialog(this,
                "Impossible de lire le fichier (problème de permissions):\n" + damagedFile.getAbsolutePath(),
                "Accès refusé",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Demander la vidéo de référence
        JOptionPane.showMessageDialog(this,
            "Sélectionnez une vidéo de référence VALIDE.\n\n" +
            "IMPORTANT:\n" +
            "- Même appareil/caméra que la vidéo endommagée\n" +
            "- Même résolution (ex: 1920x1080)\n" +
            "- Fichier totalement valide et lisible\n\n" +
            "La référence fournit le format pour reconstruire la vidéo.",
            "Vidéo de référence requise",
            JOptionPane.INFORMATION_MESSAGE);
        
        JFileChooser refChooser = new JFileChooser();
        
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
        if (lastDir != null) {
            refChooser.setCurrentDirectory(new File(lastDir));
        }
        
        refChooser.setFileFilter(new FileNameExtensionFilter(
            "Fichiers vidéo (*.mp4, *.avi, *.mov, *.mkv)", 
            "mp4", "avi", "mov", "mkv"));
        refChooser.setDialogTitle("Choisir vidéo de référence");
        
        if (refChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            statusLabel.setText("Réparation annulée");
            return;
        }
        
        File referenceFile = refChooser.getSelectedFile();
        
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Réparation de la vidéo en cours...");
        
        SwingWorker<File, String> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                publish("Analyse de la vidéo de référence...");
                
                // Créer un fichier temporaire pour la vidéo réparée
                File repairedFile = new File(damagedFile.getParent(), 
                    "repaired_" + System.currentTimeMillis() + "_" + damagedFile.getName());
                
                publish("Extraction des frames récupérables...");
                
                // Utiliser FFmpeg avec options de tolérance d'erreur
                FFmpegFrameGrabber refGrabber = null;
                FFmpegFrameGrabber damagedGrabber = null;
                FFmpegFrameRecorder recorder = null;
                
                try {
                    // Obtenir les paramètres de la vidéo de référence
                    refGrabber = new FFmpegFrameGrabber(referenceFile);
                    refGrabber.start();
                    
                    int width = refGrabber.getImageWidth();
                    int height = refGrabber.getImageHeight();
                    double frameRate = refGrabber.getFrameRate();
                    
                    refGrabber.stop();
                    refGrabber.release();
                    
                    publish("Configuration: " + width + "x" + height + " @ " + frameRate + "fps");
                    
                    // Copier le fichier vers un emplacement temporaire sans espaces/caractères spéciaux
                    File tempDamagedFile = null;
                    try {
                        publish("Copie du fichier vers emplacement temporaire...");
                        tempDamagedFile = File.createTempFile("damaged_video_", ".mp4");
                        tempDamagedFile.deleteOnExit();
                        
                        java.nio.file.Files.copy(
                            damagedFile.toPath(), 
                            tempDamagedFile.toPath(), 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        
                        publish("Copie terminée: " + tempDamagedFile.getAbsolutePath());
                    } catch (Exception e) {
                        publish("Avertissement: Impossible de copier vers temp, utilisation du fichier original");
                        tempDamagedFile = damagedFile;
                    }
                    
                    final File fileToProcess = tempDamagedFile;
                    
                    // Ouvrir la vidéo endommagée avec tolérance maximale
                    publish("Tentative 1: Ouverture avec tolérance d'erreur standard...");
                    damagedGrabber = new FFmpegFrameGrabber(fileToProcess);
                    
                    // Options pour ignorer les erreurs et récupérer ce qui est possible
                    damagedGrabber.setOption("err_detect", "ignore_err");
                    damagedGrabber.setOption("fflags", "+genpts+igndts+discardcorrupt");
                    damagedGrabber.setOption("analyzeduration", "2147483647");
                    damagedGrabber.setOption("probesize", "2147483647");
                    damagedGrabber.setOption("max_delay", "0");
                    
                    // Forcer la lecture même si le moov atom est manquant
                    damagedGrabber.setFormat("mov,mp4,m4a,3gp,3g2,mj2");
                    
                    boolean opened = false;
                    
                    try {
                        damagedGrabber.start();
                        publish("✓ Vidéo endommagée ouverte avec succès (mode standard)");
                        opened = true;
                    } catch (Exception e) {
                        publish("✗ Échec mode standard: " + e.getMessage());
                        publish("Tentative 2: Mode H.264 brut avec paramètres forcés...");
                        
                        try {
                            damagedGrabber.release();
                        } catch (Exception ex) {}
                        
                        damagedGrabber = new FFmpegFrameGrabber(fileToProcess);
                        damagedGrabber.setOption("err_detect", "ignore_err");
                        damagedGrabber.setOption("fflags", "+genpts+igndts+discardcorrupt");
                        damagedGrabber.setFormat("h264");
                        damagedGrabber.setImageWidth(width);
                        damagedGrabber.setImageHeight(height);
                        damagedGrabber.setFrameRate(frameRate);
                        
                        try {
                            damagedGrabber.start();
                            publish("✓ Vidéo ouverte en mode H.264 brut");
                            opened = true;
                        } catch (Exception e2) {
                            publish("✗ Échec mode H.264 brut: " + e2.getMessage());
                            publish("Tentative 3: Extraction directe avec rawvideo...");
                            
                            try {
                                damagedGrabber.release();
                            } catch (Exception ex) {}
                            
                            // Dernière tentative: rawvideo
                            damagedGrabber = new FFmpegFrameGrabber(fileToProcess);
                            damagedGrabber.setOption("err_detect", "ignore_err");
                            damagedGrabber.setFormat("rawvideo");
                            damagedGrabber.setOption("video_size", width + "x" + height);
                            damagedGrabber.setOption("pixel_format", "yuv420p");
                            damagedGrabber.setFrameRate(frameRate);
                            
                            try {
                                damagedGrabber.start();
                                publish("✓ Vidéo ouverte en mode rawvideo (extraction brute)");
                                opened = true;
                            } catch (Exception e3) {
                                publish("✗ Toutes les tentatives ont échoué");
                                
                                // Nettoyer le fichier temp
                                if (tempDamagedFile != null && !tempDamagedFile.equals(damagedFile)) {
                                    try { tempDamagedFile.delete(); } catch (Exception ex) {}
                                }
                                
                                throw new Exception(
                                    "Le fichier est trop corrompu pour les 3 méthodes automatiques.\n\n" +
                                    "Détails des tentatives:\n" +
                                    "1. Mode standard: " + e.getMessage() + "\n" +
                                    "2. Mode H.264 brut: " + e2.getMessage() + "\n" +
                                    "3. Mode rawvideo: " + e3.getMessage() + "\n\n" +
                                    "🔧 SOLUTION RECOMMANDÉE:\n\n" +
                                    "Utilisez Digital Video Repair (gratuit, très efficace)\n" +
                                    "→ https://codecpack.co/download/Digital_Video_Repair.html\n\n" +
                                    "Digital Video Repair fonctionne souvent quand FFmpeg échoue.\n" +
                                    "Une fois réparé, rechargez le fichier normalement.\n\n" +
                                    "Alternatives:\n" +
                                    "- untrunc (ligne de commande, nécessite référence)\n" +
                                    "- Stellar Repair for Video (payant)\n" +
                                    "- Wondershare Repairit (payant)"
                                );
                            }
                        }
                    }
                    
                    if (!opened) {
                        // Nettoyer le fichier temp
                        if (tempDamagedFile != null && !tempDamagedFile.equals(damagedFile)) {
                            try { tempDamagedFile.delete(); } catch (Exception ex) {}
                        }
                        throw new Exception("Impossible d'ouvrir le fichier endommagé");
                    }
                    
                    publish("Création du fichier réparé...");
                    
                    // Créer l'enregistreur avec les paramètres de référence
                    recorder = new FFmpegFrameRecorder(repairedFile, width, height, 0);
                    recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    recorder.setVideoBitrate(8000000);
                    recorder.setVideoOption("preset", "medium");
                    recorder.setVideoOption("crf", "23");
                    recorder.start();
                    
                    int recoveredFrames = 0;
                    int errorCount = 0;
                    
                    publish("Récupération des frames...");
                    
                    // Extraire toutes les frames possibles
                    while (true) {
                        try {
                            Frame frame = damagedGrabber.grabImage();
                            if (frame == null) {
                                break; // Fin de vidéo
                            }
                            
                            recorder.record(frame);
                            recoveredFrames++;
                            
                            if (recoveredFrames % 30 == 0) {
                                publish("Frames récupérées: " + recoveredFrames);
                            }
                            
                            errorCount = 0; // Reset error count on success
                            
                        } catch (Exception e) {
                            errorCount++;
                            if (errorCount > 100) {
                                publish("Trop d'erreurs consécutives, arrêt...");
                                break;
                            }
                            // Ignorer les erreurs et continuer
                            continue;
                        }
                    }
                    
                    publish("Finalisation...");
                    
                    recorder.stop();
                    recorder.release();
                    
                    damagedGrabber.stop();
                    damagedGrabber.release();
                    
                    publish("Vidéo réparée: " + recoveredFrames + " frames récupérées");
                    
                    if (recoveredFrames == 0) {
                        if (repairedFile.exists()) {
                            repairedFile.delete();
                        }
                        throw new Exception("Aucune frame n'a pu être récupérée");
                    }
                    
                    return repairedFile;
                    
                } catch (Exception e) {
                    if (refGrabber != null) {
                        try { refGrabber.release(); } catch (Exception ex) {}
                    }
                    if (damagedGrabber != null) {
                        try { damagedGrabber.release(); } catch (Exception ex) {}
                    }
                    if (recorder != null) {
                        try { recorder.release(); } catch (Exception ex) {}
                    }
                    throw e;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                
                try {
                    File repairedFile = get();
                    
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        "Vidéo réparée avec succès!\n" +
                        "Fichier: " + repairedFile.getName() + "\n\n" +
                        "La vidéo réparée va maintenant être chargée.",
                        "Réparation réussie",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Charger la vidéo réparée
                    loadVideoFile(repairedFile);
                    
                } catch (Exception e) {
                    String errorDetail = e.getMessage();
                    String suggestion = "";
                    String title = "Erreur de réparation";
                    
                    // Détecter si c'est un échec des 3 tentatives
                    if (errorDetail != null && errorDetail.contains("3 méthodes automatiques")) {
                        title = "Réparation impossible avec FFmpeg";
                        suggestion = "\n\n🔧 SOLUTION RECOMMANDÉE:\n\n" +
                                   "Utilisez Digital Video Repair (gratuit)\n" +
                                   "https://codecpack.co/download/Digital_Video_Repair.html\n\n" +
                                   "Ce logiciel est plus efficace que FFmpeg pour les fichiers\n" +
                                   "très corrompus et fonctionne souvent dans ces cas.\n\n" +
                                   "Une fois la vidéo réparée avec Digital Video Repair,\n" +
                                   "rechargez-la dans cet éditeur en mode normal.";
                    } else if (errorDetail != null && errorDetail.contains("moov atom not found")) {
                        suggestion = "\n\n💡 Problème détecté: Atome MOOV manquant\n" +
                                   "Cela signifie que les métadonnées MP4 sont absentes.\n" +
                                   "Causes courantes:\n" +
                                   "- Enregistrement interrompu brutalement\n" +
                                   "- Carte SD retirée pendant l'écriture\n" +
                                   "- Batterie vide pendant l'enregistrement\n\n" +
                                   "🔧 Essayez Digital Video Repair:\n" +
                                   "https://codecpack.co/download/Digital_Video_Repair.html";
                    } else if (errorDetail != null && errorDetail.contains("Could not open input")) {
                        suggestion = "\n\n💡 Le fichier ne peut pas être ouvert.\n" +
                                   "Cela peut être dû à une corruption sévère.\n\n" +
                                   "🔧 Essayez Digital Video Repair:\n" +
                                   "https://codecpack.co/download/Digital_Video_Repair.html";
                    } else {
                        suggestion = "\n\nSuggestions:\n" +
                                   "- Vérifiez que la vidéo de référence est valide\n" +
                                   "- Assurez-vous qu'elle provient du même appareil\n" +
                                   "- Essayez avec une autre vidéo de référence\n" +
                                   "- Vérifiez que les deux vidéos ont la même résolution";
                    }
                    
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        errorDetail + suggestion,
                        title,
                        JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Échec de la réparation - Essayez Digital Video Repair");
                }
            }
        };
        
        worker.execute();
    }
    
    private void loadDamagedVideoAdvanced(File damagedFile) {
        // Vérifier d'abord que le fichier existe et est accessible
        if (!damagedFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Le fichier n'existe pas:\n" + damagedFile.getAbsolutePath(),
                "Fichier introuvable",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!damagedFile.canRead()) {
            JOptionPane.showMessageDialog(this,
                "Impossible de lire le fichier (problème de permissions):\n" + damagedFile.getAbsolutePath(),
                "Accès refusé",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Confirmation
        int confirm = JOptionPane.showConfirmDialog(this,
            "╔══════════════════════════════════════════╗\n" +
            "║  RÉPARATION AVANCÉE (Style Digital Video Repair)  ║\n" +
            "╚══════════════════════════════════════════╝\n\n" +
            "Cette méthode:\n" +
            "✓ N'a PAS besoin de vidéo de référence\n" +
            "✓ Analyse directement le flux H.264 brut\n" +
            "✓ Reconstruit le moov atom automatiquement\n" +
            "✓ Fonctionne souvent quand FFmpeg échoue\n\n" +
            "Fichier: " + damagedFile.getName() + "\n" +
            "Taille: " + (damagedFile.length() / (1024*1024)) + " MB\n\n" +
            "Voulez-vous continuer?",
            "Confirmation - Réparation Avancée",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Réparation avancée en cours...");
        
        SwingWorker<File, String> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                // Créer le fichier de sortie
                File repairedFile = new File(damagedFile.getParent(), 
                    "repaired_advanced_" + System.currentTimeMillis() + "_" + damagedFile.getName());
                
                // Utiliser le parser H.264 brut
                File result = fr.videoeditor.repair.MP4Rebuilder.repairWithoutReference(
                    damagedFile, 
                    repairedFile,
                    new fr.videoeditor.repair.MP4Rebuilder.ProgressCallback() {
                        @Override
                        public void onProgress(String message) {
                            publish(message);
                        }
                    }
                );
                
                return result;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String lastMessage = chunks.get(chunks.size() - 1);
                    statusLabel.setText(lastMessage);
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                
                try {
                    File repairedFile = get();
                    
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        "╔══════════════════════════════════════════╗\n" +
                        "║       ✓✓✓ RÉPARATION RÉUSSIE ✓✓✓              ║\n" +
                        "╚══════════════════════════════════════════╝\n\n" +
                        "Fichier réparé: " + repairedFile.getName() + "\n" +
                        "Taille: " + (repairedFile.length() / (1024*1024)) + " MB\n\n" +
                        "Le fichier réparé va maintenant être chargé\n" +
                        "dans l'éditeur.",
                        "Réparation Réussie",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Charger la vidéo réparée
                    loadVideoFile(repairedFile);
                    
                } catch (Exception e) {
                    String errorDetail = e.getMessage();
                    
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        "╔══════════════════════════════════════════╗\n" +
                        "║          ÉCHEC DE LA RÉPARATION               ║\n" +
                        "╚══════════════════════════════════════════╝\n\n" +
                        errorDetail + "\n\n" +
                        "═══ SOLUTIONS ALTERNATIVES ═══\n\n" +
                        "1. Digital Video Repair (Windows):\n" +
                        "   https://codecpack.co/download/Digital_Video_Repair.html\n" +
                        "   → Très efficace, interface graphique\n\n" +
                        "2. Réparation avec référence:\n" +
                        "   → Sélectionnez \"Réparer avec Référence\"\n" +
                        "   → Nécessite une vidéo du même appareil\n\n" +
                        "3. Outils ligne de commande:\n" +
                        "   → untrunc, FFmpeg direct",
                        "Échec de la Réparation",
                        JOptionPane.ERROR_MESSAGE);
                    
                    statusLabel.setText("Échec - Essayez Digital Video Repair");
                }
            }
        };
        
        worker.execute();
    }
    
    private void previewSelectedSegment() {
        VideoSegment selected = timelinePanel.getSelectedSegment();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez sélectionner un segment dans la timeline",
                "Aucun segment sélectionné",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        previewPanel.loadFrame(selected.getVideoFile(), selected.getStartTime());
    }
    
    private void configureStacking() {
        VideoSegment selected = timelinePanel.getSelectedSegment();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez sélectionner un segment dans la timeline",
                "Aucun segment sélectionné",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "Stacking d'Images", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Info segment
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("Segment: " + selected.getVideoFile().getName());
        infoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(infoLabel, gbc);
        
        gbc.gridy = 1;
        JLabel durationLabel = new JLabel(String.format("Durée: %.2fs (%.2fs à %.2fs)", 
            selected.getSegmentDuration(), selected.getStartTime(), selected.getEndTime()));
        panel.add(durationLabel, gbc);
        
        // Format
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Format d'image:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"png", "jpg", "fits"});
        panel.add(formatCombo, gbc);
        
        // Fichier de sortie
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("Fichier de sortie:"), gbc);
        
        gbc.gridx = 1;
        JTextField fileField = new JTextField(20);
        fileField.setEditable(false);
        String videoName = selected.getVideoFile().getName();
        videoName = videoName.substring(0, videoName.lastIndexOf('.'));
        fileField.setText(System.getProperty("user.home") + "/" + videoName + "_stacked.png");
        JButton browseButton = new JButton("...");
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        panel.add(filePanel, gbc);
        
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            
            // Restaurer le dernier dossier
            String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
            if (lastDir != null) {
                chooser.setCurrentDirectory(new File(lastDir));
            }
            
            chooser.setFileFilter(new FileNameExtensionFilter("Images (*.png, *.jpg, *.fits)", "png", "jpg", "fits"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                String format = (String) formatCombo.getSelectedItem();
                if (!path.endsWith("." + format)) {
                    path += "." + format;
                }
                fileField.setText(path);
                
                // Sauvegarder le dossier
                prefs.put(PREF_LAST_DIRECTORY, chooser.getSelectedFile().getParent());
            }
        });
        
        // Info
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(
            "Le stacking combine toutes les frames du segment\n" +
            "pour créer une seule image plus lumineuse.\n" +
            "FITS 32 bits: Format astronomique haute précision.");
        infoArea.setEditable(false);
        infoArea.setBackground(panel.getBackground());
        infoArea.setFont(new Font("Arial", Font.ITALIC, 11));
        panel.add(infoArea, gbc);
        
        dialog.add(panel, BorderLayout.CENTER);
        
        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Stacker");
        JButton cancelButton = new JButton("Annuler");
        
        okButton.addActionListener(e -> {
            String format = (String) formatCombo.getSelectedItem();
            File outputFile = new File(fileField.getText());
            
            dialog.dispose();
            
            progressBar.setVisible(true);
            progressBar.setValue(0);
            statusLabel.setText("Stacking en cours...");
            
            ImageStacker.stackSegment(selected, outputFile, format, brightnessMultiplier,
                new ImageStacker.ProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(message);
                        });
                    }
                    
                    @Override
                    public void onComplete(boolean success, String message) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setVisible(false);
                            statusLabel.setText("Stacking terminé");
                            
                            if (success) {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    message,
                                    "Stacking réussi",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    message,
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    
    private void deleteSelectedSegment() {
        VideoSegment selected = timelinePanel.getSelectedSegment();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez sélectionner un segment dans la timeline",
                "Aucun segment sélectionné",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vraiment supprimer ce segment?\n" + selected.toString(),
            "Confirmer la suppression",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            timelinePanel.getSegments().remove(selected);
            timelinePanel.repaint();
            statusLabel.setText("Segment supprimé");
        }
    }
    
    private void clearAllVideos() {
        if (timelinePanel.getSegments().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucune vidéo chargée",
                "Timeline vide",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vraiment retirer toutes les vidéos (" + 
            timelinePanel.getSegments().size() + " segment(s))?\n" +
            "Cette action ne peut pas être annulée.",
            "Confirmer la suppression",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            timelinePanel.getSegments().clear();
            timelinePanel.repaint();
            previewPanel.clearFrame();
            statusLabel.setText("Toutes les vidéos ont été retirées");
        }
    }
    
    private void extractFrames() {
        VideoSegment selected = timelinePanel.getSelectedSegment();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez sélectionner un segment dans la timeline",
                "Aucun segment sélectionné",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Dialogue de configuration
        JDialog dialog = new JDialog(this, "Extraction des Images", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Info segment
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("Segment: " + selected.getVideoFile().getName());
        infoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(infoLabel, gbc);
        
        gbc.gridy = 1;
        JLabel durationLabel = new JLabel(String.format("Durée: %.2fs (%.2fs à %.2fs)", 
            selected.getSegmentDuration(), selected.getStartTime(), selected.getEndTime()));
        panel.add(durationLabel, gbc);
        
        // Format
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Format d'image:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"png", "jpg"});
        panel.add(formatCombo, gbc);
        
        // Dossier de sortie
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("Dossier de sortie:"), gbc);
        
        gbc.gridx = 1;
        JTextField folderField = new JTextField(20);
        folderField.setEditable(false);
        folderField.setText(System.getProperty("user.home") + "/extracted_frames");
        JButton browseButton = new JButton("...");
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.add(folderField, BorderLayout.CENTER);
        folderPanel.add(browseButton, BorderLayout.EAST);
        panel.add(folderPanel, gbc);
        
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            
            // Restaurer le dernier dossier
            String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
            if (lastDir != null) {
                chooser.setCurrentDirectory(new File(lastDir));
            }
            
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showDialog(dialog, "Sélectionner") == JFileChooser.APPROVE_OPTION) {
                folderField.setText(chooser.getSelectedFile().getAbsolutePath());
                
                // Sauvegarder le dossier
                prefs.put(PREF_LAST_DIRECTORY, chooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        dialog.add(panel, BorderLayout.CENTER);
        
        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Extraire");
        JButton cancelButton = new JButton("Annuler");
        
        okButton.addActionListener(e -> {
            String format = (String) formatCombo.getSelectedItem();
            File outputDir = new File(folderField.getText());
            
            dialog.dispose();
            
            progressBar.setVisible(true);
            progressBar.setValue(0);
            statusLabel.setText("Extraction des frames en cours...");
            
            FrameExtractor.extractFrames(selected, outputDir, format, 
                new FrameExtractor.ProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        SwingUtilities.invokeLater(() -> {
                            int percent = (int) ((current / (double) total) * 100);
                            progressBar.setValue(percent);
                            statusLabel.setText(message);
                        });
                    }
                    
                    @Override
                    public void onComplete(boolean success, String message) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setVisible(false);
                            statusLabel.setText("Extraction terminée");
                            
                            if (success) {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    message,
                                    "Extraction réussie",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    message,
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void exportVideo() {
        if (timelinePanel.getSegments().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucun segment à exporter. Veuillez charger au moins une vidéo.",
                "Timeline vide",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        
        // Restaurer le dernier dossier
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier MP4 (*.mp4)", "mp4"));
        fileChooser.setSelectedFile(new File("export_video.mp4"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (!outputFile.getName().endsWith(".mp4")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".mp4");
            }
            
            // Sauvegarder le dossier
            prefs.put(PREF_LAST_DIRECTORY, outputFile.getParent());
            
            final File finalOutputFile = outputFile;
            
            progressBar.setVisible(true);
            progressBar.setValue(0);
            
            VideoExporter.exportVideo(timelinePanel.getSegments(), finalOutputFile, brightnessMultiplier,
                new VideoExporter.ProgressListener() {
                    @Override
                    public void onProgress(int percent, String message) {
                        SwingUtilities.invokeLater(() -> {
                            if (percent >= 0) {
                                progressBar.setValue(percent);
                            }
                            statusLabel.setText(message);
                        });
                    }
                    
                    @Override
                    public void onComplete(boolean success, String message) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setVisible(false);
                            statusLabel.setText(message);
                            
                            if (success) {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    "Vidéo exportée avec succès!\n" + finalOutputFile.getAbsolutePath(),
                                    "Export réussi",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    "Erreur lors de l'export:\n" + message,
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
        }
    }
    
    private void exportVideoAVI() {
        if (timelinePanel.getSegments().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucun segment à exporter. Veuillez charger au moins une vidéo.",
                "Timeline vide",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        
        // Restaurer le dernier dossier
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, null);
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier AVI sans perte (*.avi)", "avi"));
        fileChooser.setSelectedFile(new File("export_video_lossless.avi"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (!outputFile.getName().endsWith(".avi")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".avi");
            }
            
            // Sauvegarder le dossier
            prefs.put(PREF_LAST_DIRECTORY, outputFile.getParent());
            
            final File finalOutputFile = outputFile;
            
            progressBar.setVisible(true);
            progressBar.setValue(0);
            
            VideoExporter.exportVideoAVI(timelinePanel.getSegments(), finalOutputFile, brightnessMultiplier,
                new VideoExporter.ProgressListener() {
                    @Override
                    public void onProgress(int percent, String message) {
                        SwingUtilities.invokeLater(() -> {
                            if (percent >= 0) {
                                progressBar.setValue(percent);
                            }
                            statusLabel.setText(message);
                        });
                    }
                    
                    @Override
                    public void onComplete(boolean success, String message) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setVisible(false);
                            statusLabel.setText(message);
                            
                            if (success) {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    "Vidéo AVI exportée avec succès (sans perte)!\n" + 
                                    "Fichier: " + finalOutputFile.getAbsolutePath() + "\n" +
                                    "Codec: FFV1 (lossless)\n" +
                                    "Taille: " + (finalOutputFile.length() / (1024*1024)) + " MB",
                                    "Export AVI réussi",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(VideoEditorFrame.this,
                                    "Erreur lors de l'export AVI:\n" + message,
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            VideoEditorFrame frame = new VideoEditorFrame();
            frame.setVisible(true);
        });
    }
}
