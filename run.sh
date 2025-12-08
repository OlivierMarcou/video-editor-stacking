#!/bin/bash

echo "========================================"
echo "  Éditeur Vidéo - Java 21"
echo "========================================"
echo ""

# Vérifier Java
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé ou n'est pas dans le PATH"
    exit 1
fi

# Vérifier la version de Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 ou supérieur est requis (version actuelle: $JAVA_VERSION)"
    exit 1
fi

echo "✅ Java $JAVA_VERSION détecté"

# Vérifier FFmpeg
if ! command -v ffmpeg &> /dev/null; then
    echo "⚠️  FFmpeg n'est pas installé. L'application peut ne pas fonctionner correctement."
    echo "   Installation recommandée:"
    echo "   - Ubuntu/Debian: sudo apt install ffmpeg"
    echo "   - macOS: brew install ffmpeg"
    echo ""
else
    echo "✅ FFmpeg détecté"
fi

# Vérifier Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven n'est pas installé"
    exit 1
fi

echo "✅ Maven détecté"
echo ""
echo "Compilation du projet..."

# Compiler si nécessaire
if [ ! -d "target" ] || [ ! -f "target/video-editor-1.0-SNAPSHOT.jar" ]; then
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Erreur lors de la compilation"
        exit 1
    fi
fi

echo ""
echo "🚀 Lancement de l'application..."
echo ""

# Lancer l'application avec plus de mémoire pour les grosses vidéos
mvn exec:java -Dexec.mainClass="fr.videoeditor.ui.VideoEditorFrame" -Dexec.args="-Xmx4G"
