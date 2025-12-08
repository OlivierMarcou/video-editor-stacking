# Traitement Astrophotographique - Soustraction d'Offset

## Vue d'ensemble

La soustraction d'offset (ou dark frame subtraction) est une technique essentielle en astrophotographie pour réduire le bruit thermique du capteur et améliorer la qualité des images stackées.

## Utilisation

### Définir la zone d'offset (Dark Frames)

1. **Maintenir la touche Shift enfoncée**
2. **Clic gauche** dans la timeline = positionner le curseur de **début d'offset** (orange)
3. **Clic droit** dans la timeline = positionner le curseur de **fin d'offset** (orange)

Une zone **orange transparente** apparaît dans la timeline avec le label "OFFSET".

### Définir la zone de stacking (Light Frames)

1. **Sans maintenir Shift**
2. **Clic gauche** = positionner le curseur de **début de coupe** (rouge)
3. **Clic droit** = positionner le curseur de **fin de coupe** (bleu)

### Stacker avec soustraction d'offset

1. Cliquer sur **"📸 Stacker Images"**
2. Choisir le format (PNG, JPG, ou FITS)
3. Choisir le nom du fichier de sortie
4. Cliquer sur "Stacker"

Le processus va:
1. Collecter les frames d'offset (dark frames)
2. Créer un "Master Dark" (moyenne des dark frames)
3. Collecter les frames principales (light frames)
4. Soustraire le Master Dark de chaque light frame
5. Stacker les frames corrigées

## Principe du Traitement

### Master Dark (Offset)
```
Master Dark = Moyenne(Dark Frame 1, Dark Frame 2, ..., Dark Frame N)
```

### Correction des Light Frames
```
Light Frame Corrigée = Light Frame Brute - Master Dark
```

### Stacking Final
```
Image Stackée = Moyenne(Light Frame Corrigée 1, Light Frame Corrigée 2, ...)
```

## Cas d'usage

### Astrophotographie Classique
- **Offset/Dark frames**: Frames prises avec le capuchon sur l'objectif (noir complet)
- **Light frames**: Frames de votre cible astronomique
- **Résultat**: Image avec beaucoup moins de bruit thermique

### Vidéo de Nuit
- **Offset**: Portion de la vidéo où la scène est complètement noire
- **Light frames**: Portion avec le sujet visible
- **Résultat**: Sujet plus clair avec fond noir propre

## Exemple Pratique

### Scénario: Vidéo de la Lune
Vous avez une vidéo de 60 secondes:
- 0-5s: Noir complet avant l'enregistrement
- 5-55s: La Lune visible
- 55-60s: Noir complet après l'enregistrement

**Configuration:**
1. Shift + clic gauche à 0s → début offset
2. Shift + clic droit à 5s → fin offset
3. Clic gauche à 5s → début stacking
4. Clic droit à 55s → fin stacking

**Résultat:**
- Les 5 premières secondes servent de dark frames
- Les 50 secondes de la Lune sont stackées avec soustraction du bruit
- Image finale de la Lune beaucoup plus propre

## Curseurs de la Timeline

| Couleur | Touche | Fonction |
|---------|--------|----------|
| Rouge | Clic gauche normal | Début de coupe (light frames) |
| Bleu | Clic droit normal | Fin de coupe (light frames) |
| Orange gauche | Shift + clic gauche | Début offset (dark frames) |
| Orange droit | Shift + clic droit | Fin offset (dark frames) |

## Conseils pour de Meilleurs Résultats

### ✅ Bonnes Pratiques
- Utilisez au moins 10-20 dark frames pour un bon Master Dark
- Les dark frames doivent être pris dans les mêmes conditions (température, exposition)
- Plus vous avez de light frames, meilleur sera le résultat
- Les scènes doivent être fixes (pas de mouvement)

### ❌ À Éviter
- Ne pas utiliser de frames avec mouvement comme dark frames
- Ne pas mélanger différentes expositions
- Ne pas utiliser trop peu de frames (minimum 5-10)

## Format FITS 32-bit

Le format FITS 32-bit est idéal pour l'astrophotographie car:
- Pas de perte de données (float 32 bits)
- Conserve toute la gamme dynamique
- Compatible avec les logiciels d'astronomie (PixInsight, Siril, etc.)
- Permet des traitements ultérieurs sans dégradation

## Workflow Complet Astrophotographie

1. **Enregistrer la vidéo**
   - Prendre des dark frames (capuchon sur objectif)
   - Prendre les light frames (sujet visible)

2. **Dans l'éditeur**
   - Charger la vidéo
   - Shift + définir zone offset (dark frames)
   - Définir zone de stacking (light frames)
   - Exporter en FITS 32-bit

3. **Post-traitement** (optionnel)
   - Ouvrir le FITS dans PixInsight/Siril
   - Ajuster niveaux, courbes
   - Appliquer déconvolution
   - Exporter en PNG/JPG final

## Algorithme Détaillé

```
1. Collecter N dark frames:
   D1, D2, ..., DN

2. Créer Master Dark:
   MD = (D1 + D2 + ... + DN) / N

3. Collecter M light frames:
   L1, L2, ..., LM

4. Corriger chaque light frame:
   L1_corr = L1 - MD
   L2_corr = L2 - MD
   ...
   LM_corr = LM - MD

5. Stacker les frames corrigées:
   Image_finale = (L1_corr + L2_corr + ... + LM_corr) / M
```

## Comparaison Sans/Avec Offset

**Sans soustraction d'offset:**
- Bruit thermique visible
- Pixels chauds apparents
- Fond gris au lieu de noir
- Signal/Bruit ratio faible

**Avec soustraction d'offset:**
- Bruit thermique réduit
- Pixels chauds éliminés
- Fond noir uniforme
- Signal/Bruit ratio élevé

## Support

Cette fonctionnalité utilise les techniques standard de l'astrophotographie professionnelle pour obtenir les meilleurs résultats possibles lors du stacking vidéo.
