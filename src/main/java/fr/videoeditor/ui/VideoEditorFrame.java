package fr.videoeditor.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import fr.videoeditor.export.VideoExporter;
import fr.videoeditor.export.FrameExtractor;
import fr.videoeditor.export.ImageStacker;
import fr.videoeditor.model.VideoSegment;
import fr.videoeditor.ui.TimelinePanel;
import fr.videoeditor.ui.VideoPreviewPanel;
import org.bytedeco.javacv.FFmpegFrameGrabber;
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
        
        // Bouton exporter
        JButton exportButton = new JButton("💾 Exporter MP4");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.setBackground(new Color(46, 125, 50));
        exportButton.setForeground(Color.WHITE);
        exportButton.setOpaque(true);
        exportButton.addActionListener(e -> exportVideo());
        panel.add(exportButton);
        
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
            
            loadVideoFile(videoFile);
        }
    }
    
    private void loadVideoFile(File videoFile) {
        SwingWorker<VideoSegment, Void> worker = new SwingWorker<>() {
            @Override
            protected VideoSegment doInBackground() throws Exception {
                statusLabel.setText("Chargement de " + videoFile.getName() + "...");
                
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
                    grabber.start();
                    double duration = grabber.getLengthInTime() / 1_000_000.0;
                    grabber.stop();
                    
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
                    JOptionPane.showMessageDialog(VideoEditorFrame.this,
                        "Erreur lors du chargement de la vidéo:\n" + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Erreur de chargement");
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
