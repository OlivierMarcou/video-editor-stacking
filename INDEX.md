# 📋 Index du Projet - Éditeur Vidéo Java 21

## 🎯 Vue d'Ensemble

Éditeur vidéo complet en Java 21 avec interface Swing moderne, permettant de:
- ✂️ Charger et découper plusieurs vidéos
- 🎬 Composer une timeline interactive
- 📸 Appliquer le stacking d'images sur zones sombres
- 💾 Exporter en MP4 haute qualité

---

## 📁 Structure du Projet

```
video-editor/
├── 📖 Documentation
│   ├── README.md              ← Documentation complète
│   ├── QUICK_START.md         ← Démarrage rapide (5 min)
│   ├── VISUAL_GUIDE.md        ← Schémas ASCII de l'interface
│   └── TECHNICAL_GUIDE.md     ← Détails techniques avancés
│
├── 🚀 Lanceurs
│   ├── run.sh                 ← Linux/macOS
│   ├── run.bat                ← Windows
│   └── generate_test_videos.sh ← Créer vidéos de test
│
├── ⚙️ Configuration
│   ├── pom.xml                ← Dépendances Maven
│   └── .gitignore             ← Fichiers à ignorer
│
└── 💻 Code Source
    └── src/main/java/fr/videoeditor/
        ├── model/
        │   └── VideoSegment.java           ← Modèle de segment
        ├── ui/
        │   ├── VideoEditorFrame.java       ← Fenêtre principale
        │   ├── TimelinePanel.java          ← Timeline interactive
        │   └── VideoPreviewPanel.java      ← Prévisualisation
        ├── export/
        │   └── VideoExporter.java          ← Export + Stacking
        └── examples/
            └── ProgrammaticExample.java    ← Exemples d'API
```

---

## 📚 Guide de Lecture

### Pour Démarrer Rapidement
**1. [QUICK_START.md](QUICK_START.md)** ⭐ **COMMENCEZ ICI**
   - Installation en 5 minutes
   - Tutoriel complet
   - Exemples pratiques

### Pour Comprendre le Projet
**2. [README.md](README.md)**
   - Fonctionnalités détaillées
   - Guide d'utilisation complet
   - Architecture du projet
   - Troubleshooting

### Pour Visualiser l'Interface
**3. [VISUAL_GUIDE.md](VISUAL_GUIDE.md)**
   - Schémas ASCII de l'UI
   - Explication des contrôles
   - Workflow visuel
   - États de la timeline

### Pour les Développeurs
**4. [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md)**
   - Architecture et patterns
   - Algorithmes de stacking
   - Optimisations possibles
   - Extensions futures

---

## 🚀 Démarrage Express

### Étape 1: Installation
```bash
# Vérifier les prérequis
java -version  # Doit être >= 21
mvn -version   # Maven 3.6+
ffmpeg -version # FFmpeg

# Si FFmpeg manque:
# Ubuntu: sudo apt install ffmpeg
# macOS: brew install ffmpeg
# Windows: choco install ffmpeg
```

### Étape 2: Lancer
```bash
# Linux/macOS
./run.sh

# Windows
run.bat
```

### Étape 3: Tester
```bash
# Générer des vidéos de test
./generate_test_videos.sh

# Puis charger les vidéos dans l'application
```

---

## 🎓 Tutoriel en 2 Minutes

### Utilisation Basique
1. **Charger** une vidéo → `📂 Charger Vidéo`
2. **Découper** avec les poignées blanches dans la timeline
3. **Ajouter** d'autres vidéos (elles se mettent bout à bout)
4. **Exporter** → `💾 Exporter MP4`

### Utilisation Avancée - Stacking
1. Charger une vidéo avec **scène fixe et sombre**
2. Sélectionner le segment
3. Cliquer `📸 Configurer Stacking`
4. Définir la zone (ex: 2s à 5s)
5. Exporter → La zone sera éclaircie ✨

---

## 🗂️ Fichiers Clés

### Documentation (lisez dans cet ordre)
| Fichier | Contenu | Quand le lire |
|---------|---------|---------------|
| **QUICK_START.md** | Démarrage rapide | **EN PREMIER** |
| README.md | Documentation complète | Pour tout comprendre |
| VISUAL_GUIDE.md | Schémas de l'UI | Pour visualiser |
| TECHNICAL_GUIDE.md | Détails techniques | Pour développer |

### Code Principal
| Fichier | Rôle |
|---------|------|
| **VideoEditorFrame.java** | Fenêtre principale, contrôleur |
| **TimelinePanel.java** | Timeline avec curseurs |
| **VideoExporter.java** | Export et stacking |
| **VideoSegment.java** | Modèle de données |

### Scripts Utiles
| Script | Usage |
|--------|-------|
| **run.sh** / **run.bat** | Lancer l'application |
| **generate_test_videos.sh** | Créer vidéos de test |

---

## 🔧 Technologies Utilisées

| Technologie | Version | Rôle |
|-------------|---------|------|
| **Java** | 21 | Langage principal |
| **Swing** | Built-in | Interface graphique |
| **FlatLaf** | 3.4.1 | Look and Feel moderne |
| **JavaCV** | 1.5.10 | Wrapper FFmpeg/OpenCV |
| **FFmpeg** | Latest | Manipulation vidéo/audio |
| **OpenCV** | via JavaCV | Stacking d'images |
| **Maven** | 3.6+ | Build et dépendances |

---

## 🎯 Fonctionnalités

### ✅ Implémentées
- [x] Chargement vidéo multi-formats (MP4, AVI, MOV, MKV)
- [x] Timeline interactive avec découpage précis
- [x] Curseurs de début/fin par segment
- [x] Prévisualisation des frames
- [x] Stacking d'images sur zones fixes
- [x] Export MP4 haute qualité (H.264 + AAC)
- [x] Concaténation de multiples vidéos
- [x] Interface moderne avec FlatLaf
- [x] Barre de progression d'export
- [x] Support audio
- [x] Conservation résolution/qualité

### 🔮 Extensions Possibles (voir TECHNICAL_GUIDE.md)
- [ ] Transitions (fade, dissolve, wipe)
- [ ] Filtres vidéo (brightness, contrast, grayscale)
- [ ] Ondulation audio dans la timeline
- [ ] Zoom sur la timeline
- [ ] Marqueurs de temps personnalisés
- [ ] Lecture temps réel
- [ ] Export en d'autres formats
- [ ] Thumbnails dans la timeline

---

## 📊 Paramètres d'Export

### Vidéo
- **Codec:** H.264
- **Format:** MP4
- **Bitrate:** 8 Mbps
- **Résolution:** Conservée de la source
- **Framerate:** Conservé de la source

### Audio
- **Codec:** AAC
- **Bitrate:** 192 kbps
- **Sample Rate:** Conservé de la source
- **Canaux:** Conservés de la source

---

## 💡 Conseils Rapides

### Pour le Stacking
✅ **OUI:**
- Scènes fixes (caméra immobile)
- Vidéos sous-exposées
- Zones sombres
- Durée 2-5 secondes

❌ **NON:**
- Scènes avec mouvement
- Vidéos bien exposées
- Zones déjà claires

### Pour les Performances
- Vidéos 4K: augmenter mémoire `-Xmx8G`
- Export long: normal, dépend durée totale
- Stacking: augmente temps d'export

---

## 🆘 Aide Rapide

### Problèmes Courants

**"Erreur de chargement"**
```
→ FFmpeg pas installé ou format non supporté
→ Solution: Vérifier FFmpeg, essayer autre format
```

**"Export échoué"**
```
→ Espace disque insuffisant ou permissions
→ Solution: Vérifier espace et permissions d'écriture
```

**"Application lente"**
```
→ Vidéo trop volumineuse
→ Solution: java -Xmx4G -jar video-editor.jar
```

---

## 📞 Support

### Documentation
- Lisez `QUICK_START.md` en premier
- Consultez `README.md` pour détails
- Voir `TECHNICAL_GUIDE.md` pour développement

### Ressources Externes
- [JavaCV GitHub](https://github.com/bytedeco/javacv)
- [FFmpeg Docs](https://ffmpeg.org/documentation.html)
- [FlatLaf](https://www.formdev.com/flatlaf/)

---

## 🎬 Prêt à Commencer?

```bash
cd video-editor
./run.sh  # ou run.bat
```

**Bonne édition vidéo!** 🎉

---

## 📝 Checklist de Démarrage

- [ ] Java 21+ installé
- [ ] Maven installé
- [ ] FFmpeg installé
- [ ] Lu QUICK_START.md
- [ ] Lancé l'application (./run.sh ou run.bat)
- [ ] Généré vidéos de test (./generate_test_videos.sh)
- [ ] Testé chargement + découpage
- [ ] Testé le stacking
- [ ] Testé l'export

✅ **Tout fonctionne?** → Vous êtes prêt à éditer vos vidéos!

---

*Projet créé avec Java 21, Swing, JavaCV et FFmpeg*
*Version 1.0 - Décembre 2024*
