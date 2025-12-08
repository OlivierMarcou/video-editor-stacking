# Éditeur Vidéo - Java 21

Un éditeur vidéo complet développé en Java 21 avec Swing et JavaCV (FFmpeg).

## Fonctionnalités

### ✅ Fonctionnalités principales
- **Chargement de vidéos multiples** - Charger plusieurs vidéos à la suite
- **Timeline interactive** - Visualiser et organiser vos segments vidéo
- **Découpage précis** - 2 curseurs par segment pour définir début et fin
- **Prévisualisation** - Voir les frames à n'importe quel moment
- **Extraction d'images** - Sauvegarder toutes les frames d'un segment en PNG ou JPG
- **Stacking d'images** - Éclaircir les zones sombres en combinant plusieurs frames
- **Export MP4** - Exporter en conservant qualité et résolution d'origine

### 📸 Stacking d'images
Le stacking d'images est une technique qui combine plusieurs frames pour :
- Augmenter la luminosité des zones sombres
- Réduire le bruit dans les vidéos de nuit
- Améliorer la visibilité des détails

**Utilisation :** Sélectionnez un segment fixe (sans mouvement) et activez le stacking sur la période souhaitée.

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.6+**
- **FFmpeg** doit être installé sur le système (JavaCV l'utilise automatiquement)

### Installation de FFmpeg

**Windows:**
```bash
choco install ffmpeg
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install ffmpeg
```

**macOS:**
```bash
brew install ffmpeg
```

## Compilation et exécution

### 1. Compiler le projet
```bash
mvn clean compile
```

### 2. Lancer l'application
```bash
mvn exec:java -Dexec.mainClass="fr.videoeditor.ui.VideoEditorFrame"
```

Ou créer un JAR exécutable :
```bash
mvn clean package
java -jar target/video-editor-1.0-SNAPSHOT.jar
```

## Guide d'utilisation

### Étape 1 : Charger des vidéos
1. Cliquez sur **"📂 Charger Vidéo"**
2. Sélectionnez votre fichier vidéo (MP4, AVI, MOV, MKV)
3. La vidéo apparaît dans la timeline
4. Répétez pour charger d'autres vidéos à la suite

### Étape 2 : Découper les segments
1. Cliquez sur un segment dans la timeline pour le sélectionner (bordure jaune)
2. Déplacez les **poignées blanches** sur les bords :
   - Poignée gauche = point de début
   - Poignée droite = point de fin
3. La durée affichée se met à jour en temps réel

### Étape 3 : Prévisualiser
1. Sélectionnez un segment
2. Cliquez sur **"▶ Prévisualiser"**
3. La frame au point de début s'affiche

### Étape 3.5 : Extraire les images (optionnel)
1. Sélectionnez un segment dans la timeline
2. Cliquez sur **"🖼 Extraire Images"**
3. Choisissez le format (PNG ou JPG)
4. Sélectionnez le dossier de destination
5. Cliquez sur "Extraire"
6. Toutes les frames du segment sont sauvegardées individuellement

### Étape 4 : Configurer le stacking (optionnel)
1. Sélectionnez un segment avec des **images fixes et sombres**
2. Cliquez sur **"📸 Configurer Stacking"**
3. Cochez "Activer le stacking"
4. Définissez début et fin de la zone à éclaircir
5. Une zone rouge apparaît dans la timeline

### Étape 5 : Exporter
1. Cliquez sur **"💾 Exporter MP4"**
2. Choisissez l'emplacement et le nom du fichier
3. L'export démarre avec barre de progression
4. La vidéo finale conserve la résolution et qualité d'origine

## Architecture du projet

```
video-editor/
├── src/main/java/fr/videoeditor/
│   ├── model/
│   │   └── VideoSegment.java       # Représente un segment vidéo
│   ├── ui/
│   │   ├── VideoEditorFrame.java   # Fenêtre principale
│   │   ├── TimelinePanel.java      # Timeline interactive
│   │   └── VideoPreviewPanel.java  # Prévisualisation
│   └── export/
│       └── VideoExporter.java      # Export et stacking
├── pom.xml                         # Dépendances Maven
└── README.md
```

## Technologies utilisées

- **Java 21** - Langage de programmation
- **Swing** - Interface graphique
- **FlatLaf** - Look and Feel moderne
- **JavaCV 1.5.10** - Wrapper Java pour FFmpeg/OpenCV
- **FFmpeg** - Manipulation vidéo et audio
- **OpenCV** - Traitement d'images pour le stacking

## Paramètres d'export

- **Codec vidéo:** H.264
- **Format:** MP4
- **Bitrate vidéo:** 8 Mbps
- **Codec audio:** AAC
- **Bitrate audio:** 192 kbps
- **Résolution:** Conservée de la source
- **Framerate:** Conservé de la source

## Astuces et conseils

### Pour de meilleurs résultats avec le stacking :
- ✅ Utilisez sur des scènes **complètement fixes** (caméra sur trépied)
- ✅ Appliquez sur des vidéos de **nuit ou sous-exposées**
- ✅ Sélectionnez une **zone de 2-5 secondes** pour un bon équilibre
- ❌ N'utilisez pas sur des scènes avec **mouvement**

### Optimisation des performances :
- Les fichiers vidéo lourds prennent plus de temps à charger
- L'export peut être long selon la durée totale et le nombre de segments
- Le stacking augmente significativement le temps d'export

## Dépannage

### Problème : "Erreur de chargement de la vidéo"
- Vérifiez que FFmpeg est bien installé
- Essayez avec un autre format vidéo
- Vérifiez que le fichier n'est pas corrompu

### Problème : "Export échoué"
- Vérifiez l'espace disque disponible
- Assurez-vous d'avoir les droits d'écriture
- Essayez avec un nom de fichier plus court

### Problème : "Application lente"
- Les vidéos 4K peuvent nécessiter plus de mémoire
- Augmentez la mémoire JVM : `java -Xmx4G -jar ...`

## Licence

Projet personnel - Libre d'utilisation et de modification

## Auteur

Développé avec Java 21, Swing et JavaCV
