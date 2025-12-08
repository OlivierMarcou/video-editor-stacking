# 🖼 Extraction d'Images - Guide Complet

## Vue d'ensemble

La fonction **Extraire Images** permet de sauvegarder toutes les frames (images) d'un segment vidéo sélectionné. Chaque frame est sauvegardée individuellement en PNG ou JPG.

## Utilisation

### Étape par étape

1. **Sélectionner un segment**
   - Cliquez sur un segment dans la timeline (bordure jaune = sélectionné)

2. **Ouvrir le dialogue**
   - Cliquez sur le bouton **🖼 Extraire Images**

3. **Configurer l'extraction**
   - **Format**: Choisissez PNG ou JPG
     - PNG: Sans perte, fichiers plus gros
     - JPG: Avec compression, fichiers plus petits
   
   - **Dossier de sortie**: Choisissez où sauvegarder
     - Par défaut: `~/extracted_frames`
     - Cliquez sur "..." pour changer

4. **Lancer l'extraction**
   - Cliquez sur "Extraire"
   - La barre de progression s'affiche
   - Attendez la fin du traitement

5. **Résultat**
   - Toutes les frames sont dans le dossier choisi
   - Nommage: `videoname_frame_00001.png`, `videoname_frame_00002.png`, etc.

## Cas d'usage

### 📸 Photographie

Extraire une seule frame parfaite d'une vidéo:
1. Chargez votre vidéo
2. Découpez le segment au moment exact (ex: 5.00s à 5.05s)
3. Extrayez les images
4. Choisissez la meilleure frame parmi les résultats

### 🎨 Animation Frame par Frame

Créer une animation en stop-motion:
1. Découpez votre segment
2. Extrayez toutes les frames
3. Modifiez les images dans un logiciel de retouche
4. Réassemblez en vidéo avec FFmpeg

### 🔬 Analyse d'Images

Analyser chaque frame individuellement:
1. Extrayez un segment d'intérêt
2. Traitez les images avec Python/OpenCV
3. Détectez des objets, faites des mesures, etc.

### 🎬 Production de Contenu

Créer des miniatures ou des captures:
1. Découpez plusieurs segments intéressants
2. Extrayez les frames de chaque segment
3. Sélectionnez les meilleures images
4. Utilisez comme miniatures ou captures d'écran

## Format de sortie

### PNG (Recommandé pour qualité)
- **Avantages:**
  - Sans perte de qualité
  - Supporte la transparence
  - Idéal pour retouche ultérieure
  
- **Inconvénients:**
  - Fichiers plus volumineux
  - Plus lent à sauvegarder

### JPG (Recommandé pour espace disque)
- **Avantages:**
  - Fichiers plus petits
  - Plus rapide à sauvegarder
  - Largement compatible
  
- **Inconvénients:**
  - Perte de qualité (compression)
  - Pas de transparence

## Performance

### Estimation du temps

Pour un segment de **30 secondes** à **30 fps** (≈900 frames):
- Format PNG: ~2-3 minutes
- Format JPG: ~1-2 minutes

### Estimation de l'espace disque

Pour une vidéo **1920×1080** (Full HD):
- PNG: ~2-3 MB par frame → 900 frames = **2-3 GB**
- JPG: ~200-300 KB par frame → 900 frames = **180-270 MB**

## FAQ

**Q: Puis-je extraire seulement quelques frames au lieu de toutes?**
R: Actuellement, toutes les frames sont extraites. Pour extraire moins de frames, découpez d'abord un segment très court.

**Q: Les frames conservent-elles la résolution d'origine?**
R: Oui, la résolution d'origine est conservée.

**Q: Quel format pour le machine learning?**
R: PNG est meilleur pour la qualité, mais JPG est acceptable et prend moins de place.
