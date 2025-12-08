@echo off
setlocal

echo ========================================
echo   Éditeur Vidéo - Java 21
echo ========================================
echo.

REM Vérifier Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [31mJava n'est pas installé ou n'est pas dans le PATH[0m
    pause
    exit /b 1
)

REM Récupérer la version de Java
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -version 2^>^&1') do set "jver=%%j"
if %jver% LSS 21 (
    echo [31mJava 21 ou supérieur est requis[0m
    pause
    exit /b 1
)

echo [32mJava %jver% détecté[0m

REM Vérifier FFmpeg
where ffmpeg >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [33mFFmpeg n'est pas installé. L'application peut ne pas fonctionner.[0m
    echo    Installation recommandée: choco install ffmpeg
    echo.
) else (
    echo [32mFFmpeg détecté[0m
)

REM Vérifier Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [31mMaven n'est pas installé[0m
    pause
    exit /b 1
)

echo [32mMaven détecté[0m
echo.

REM Compiler si nécessaire
if not exist "target\video-editor-1.0-SNAPSHOT.jar" (
    echo Compilation du projet...
    call mvn clean package -DskipTests
    if %ERRORLEVEL% NEQ 0 (
        echo [31mErreur lors de la compilation[0m
        pause
        exit /b 1
    )
)

echo.
echo [32mLancement de l'application...[0m
echo.

REM Lancer l'application
call mvn exec:java -Dexec.mainClass="fr.videoeditor.ui.VideoEditorFrame"

pause
