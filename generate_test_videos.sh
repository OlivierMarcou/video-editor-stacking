#!/bin/bash

# Script pour générer des vidéos de test avec FFmpeg
# Nécessite FFmpeg installé sur le système

echo "============================================"
echo "  Génération de Vidéos de Test"
echo "============================================"
echo ""

# Vérifier FFmpeg
if ! command -v ffmpeg &> /dev/null; then
    echo "❌ FFmpeg n'est pas installé!"
    echo "Installation:"
    echo "  Ubuntu/Debian: sudo apt install ffmpeg"
    echo "  macOS: brew install ffmpeg"
    echo "  Windows: choco install ffmpeg"
    exit 1
fi

echo "✅ FFmpeg détecté"
echo ""

# Créer le dossier test_videos
mkdir -p test_videos
cd test_videos

echo "Génération des vidéos de test..."
echo ""

# Vidéo 1: Barres de couleur animées (10 secondes)
echo "1/4 - Création de video1.mp4 (barres colorées)..."
ffmpeg -f lavfi -i testsrc=duration=10:size=1280x720:rate=30 \
       -c:v libx264 -pix_fmt yuv420p -y video1.mp4 2>/dev/null

# Vidéo 2: Dégradé animé (15 secondes)
echo "2/4 - Création de video2.mp4 (dégradé)..."
ffmpeg -f lavfi -i "color=c=blue:s=1280x720:d=15" \
       -vf "geq=r='X/W*255':g='(1-X/W)*255':b='Y/H*255'" \
       -c:v libx264 -pix_fmt yuv420p -y video2.mp4 2>/dev/null

# Vidéo 3: Vidéo sombre avec bruit (pour tester le stacking)
echo "3/4 - Création de dark_video.mp4 (scène sombre pour stacking)..."
ffmpeg -f lavfi -i testsrc=duration=12:size=1280x720:rate=30 \
       -vf "eq=brightness=-0.5:contrast=0.6,noise=alls=20" \
       -c:v libx264 -pix_fmt yuv420p -y dark_video.mp4 2>/dev/null

# Vidéo 4: Texte animé
echo "4/4 - Création de text_video.mp4 (texte)..."
ffmpeg -f lavfi -i color=c=white:s=1280x720:d=8 \
       -vf "drawtext=fontfile=/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf:text='Vidéo de Test':fontcolor=black:fontsize=80:x=(w-text_w)/2:y=(h-text_h)/2" \
       -c:v libx264 -pix_fmt yuv420p -y text_video.mp4 2>/dev/null

echo ""
echo "✅ Vidéos de test générées avec succès!"
echo ""
echo "Fichiers créés dans test_videos/:"
echo "  - video1.mp4 (10s) - Barres colorées"
echo "  - video2.mp4 (15s) - Dégradé animé"
echo "  - dark_video.mp4 (12s) - Vidéo sombre (pour stacking)"
echo "  - text_video.mp4 (8s) - Texte animé"
echo ""
echo "Vous pouvez maintenant tester l'application!"
echo ""
echo "Exemples d'utilisation:"
echo "  1. Charger video1.mp4, découper de 2s à 7s"
echo "  2. Ajouter video2.mp4, découper de 5s à 12s"
echo "  3. Charger dark_video.mp4, activer stacking de 2s à 8s"
echo "  4. Exporter le résultat"
echo ""
