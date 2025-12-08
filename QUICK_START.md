# 🎬 Éditeur Vidéo - Démarrage Rapide

## Installation Express (5 minutes)

### Prérequis
```bash
# Vérifier Java 21+
java -version

# Vérifier Maven
mvn -version

# Installer FFmpeg
# Ubuntu/Debian:
sudo apt install ffmpeg

# macOS:
brew install ffmpeg

# Windows:
choco install ffmpeg
```

### Lancer l'Application

**Linux/macOS:**
```bash
cd video-editor
./run.sh
```

**Windows:**
```batch
cd video-editor
run.bat
```

**Manuel:**
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="fr.videoeditor.ui.VideoEditorFrame"
```

## Tutoriel en 3 Minutes

### 1️⃣ Charger et Découper (30s)
1. Cliquez **"📂 Charger Vidéo"**
2. Sélectionnez votre vidéo
3. Déplacez les **poignées blanches** dans la timeline
4. Ajustez début et fin comme vous voulez

### 2️⃣ Ajouter Plus de Vidéos (30s)
1. Cliquez encore sur **"📂 Charger Vidéo"**
2. Les vidéos s'ajoutent bout à bout dans la timeline
3. Découpez chaque segment individuellement

### 3️⃣ Extraire les Images (1 min)
1. Cliquez sur un segment dans la timeline
2. Cliquez **"🖼 Extraire Images"**
3. Choisissez le format (PNG ou JPG)
4. Sélectionnez le dossier de sortie
5. Cliquez "Extraire" - toutes les frames sont sauvegardées!

### 4️⃣ Appliquer le Stacking (1 min)
1. Cliquez sur un segment avec une **scène fixe et sombre**
2. Cliquez **"📸 Configurer Stacking"**
3. Cochez "Activer"
4. Définissez début et fin (ex: 2s à 5s)
5. Une zone **rouge** apparaît = stacking actif ✓

### 5️⃣ Exporter (1 min)
1. Cliquez **"💾 Exporter MP4"**
2. Choisissez nom et emplacement
3. Attendez la barre de progression
4. C'est fait! 🎉

## Vidéos de Test

Générer des vidéos pour tester:
```bash
./generate_test_videos.sh
```

Cela crée 4 vidéos dans `test_videos/`:
- `video1.mp4` - Barres colorées (10s)
- `video2.mp4` - Dégradé (15s)
- `dark_video.mp4` - Scène sombre (12s) **← Parfait pour tester le stacking!**
- `text_video.mp4` - Texte animé (8s)

## Exemple Complet

### Scénario: Créer une vidéo de 30s à partir de 3 clips

1. **Charger video1.mp4**
   - Découper: 2s → 12s (durée: 10s)

2. **Charger dark_video.mp4**
   - Découper: 0s → 10s (durée: 10s)
   - Activer stacking: 2s → 8s (pour éclaircir)

3. **Charger video2.mp4**
   - Découper: 5s → 15s (durée: 10s)

4. **Exporter**
   - Résultat: vidéo de ~30s avec la partie sombre éclaircie ✨

## Fonctionnalités Clés

| Fonction | Description | Raccourci |
|----------|-------------|-----------|
| 📂 Charger | Ajouter une vidéo | - |
| ▶ Prévisualiser | Voir le début du segment | - |
| 🖼 Extraire Images | Sauvegarder toutes les frames en PNG/JPG | Sur segment sélectionné |
| 📸 Stacking | Éclaircir zones sombres | Sur segment fixe |
| 💾 Exporter | Créer le MP4 final | - |
| 🗑 Supprimer | Retirer un segment | - |

## Astuces Pro

### ✅ Pour de Meilleurs Résultats

**Stacking:**
- ✓ Scènes **complètement fixes** (pas de mouvement)
- ✓ Vidéos **sous-exposées** ou de nuit
- ✓ Durée de **2-5 secondes** pour la zone
- ✗ Évitez sur scènes avec mouvement

**Performance:**
- Vidéos 4K = plus lent (normal)
- Ajoutez `-Xmx8G` pour plus de mémoire si besoin

**Qualité:**
- L'export conserve la résolution d'origine
- Bitrate: 8 Mbps vidéo + 192 kbps audio
- Codec: H.264 + AAC (compatible partout)

### 🚫 Erreurs Courantes

**"Erreur de chargement"**
→ Format non supporté ou fichier corrompu
→ Essayez un autre format (MP4, AVI, MOV, MKV)

**"Export échoué"**
→ Pas assez d'espace disque
→ Vérifiez les permissions d'écriture

**Application lente**
→ Vidéo trop lourde
→ Augmentez la mémoire: `java -Xmx4G -jar ...`

## Structure du Projet

```
video-editor/
├── src/main/java/fr/videoeditor/
│   ├── model/              # VideoSegment
│   ├── ui/                 # Interface Swing
│   └── export/             # Export et Stacking
├── pom.xml                 # Dépendances Maven
├── run.sh                  # Lanceur Linux/Mac
├── run.bat                 # Lanceur Windows
├── README.md               # Documentation complète
├── VISUAL_GUIDE.md         # Guide visuel ASCII
├── TECHNICAL_GUIDE.md      # Détails techniques
└── generate_test_videos.sh # Créer vidéos de test
```

## Commandes Utiles

```bash
# Compiler
mvn clean compile

# Lancer
mvn exec:java -Dexec.mainClass="fr.videoeditor.ui.VideoEditorFrame"

# Créer JAR
mvn clean package

# Nettoyer
mvn clean

# Tester (avec vidéos de test)
./generate_test_videos.sh
./run.sh
```

## Utilisation Programmatique

Sans interface graphique (headless):

```java
import fr.videoeditor.model.VideoSegment;
import fr.videoeditor.export.VideoExporter;

// Créer segments
VideoSegment seg1 = new VideoSegment(new File("video1.mp4"), 30.0);
seg1.setStartTime(5);
seg1.setEndTime(15);

VideoSegment seg2 = new VideoSegment(new File("video2.mp4"), 20.0);
seg2.setStartTime(0);
seg2.setEndTime(10);
seg2.setStackingEnabled(true);
seg2.setStackingStart(2);
seg2.setStackingEnd(8);

// Exporter
List<VideoSegment> segments = Arrays.asList(seg1, seg2);
VideoExporter.exportVideo(segments, new File("output.mp4"), listener);
```

Voir `src/main/java/fr/videoeditor/examples/ProgrammaticExample.java` pour plus d'exemples.

## Support et Contribution

### Documentation
- **README.md** - Vue d'ensemble complète
- **VISUAL_GUIDE.md** - Représentation visuelle de l'UI
- **TECHNICAL_GUIDE.md** - Détails techniques et optimisations

### Technologies
- Java 21 (features modernes)
- Swing + FlatLaf (UI moderne)
- JavaCV 1.5.10 (FFmpeg wrapper)
- FFmpeg (traitement vidéo)
- OpenCV (stacking d'images)

### Liens Utiles
- [JavaCV GitHub](https://github.com/bytedeco/javacv)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [FlatLaf](https://www.formdev.com/flatlaf/)

## FAQ Rapide

**Q: Quels formats sont supportés?**
R: MP4, AVI, MOV, MKV, FLV, WMV, WEBM et plus via FFmpeg

**Q: Le stacking fonctionne sur quels types de vidéos?**
R: Seulement les scènes **complètement fixes** (caméra immobile)

**Q: Puis-je exporter en autre chose que MP4?**
R: Actuellement MP4 uniquement, mais facilement extensible (voir TECHNICAL_GUIDE.md)

**Q: Combien de vidéos puis-je charger?**
R: Illimité (limité seulement par la mémoire)

**Q: L'export conserve-t-il la qualité?**
R: Oui, résolution et framerate d'origine conservés, bitrate élevé (8 Mbps)

## Démarrer Maintenant!

```bash
cd video-editor
./run.sh  # ou run.bat sur Windows
```

🎬 Bon montage vidéo! 🎬
