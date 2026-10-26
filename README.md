# 📷 Lector de Códigos de Barras para Android

App Android nativa (Kotlin) que lee códigos de barras y QR en tiempo real usando
**CameraX** y **ML Kit Barcode Scanning**. Funciona sin conexión a internet.

## Características

- Lectura en vivo con la cámara trasera
- Soporta QR, EAN-13/8, UPC-A/E, Code 128/39/93, Codabar, ITF, Data Matrix, PDF417 y Aztec
- Leer códigos desde una imagen de la galería
- Copiar, compartir y abrir enlaces detectados
- Linterna e historial de lecturas de la sesión
- Vibración al detectar un código y filtro anti-repetición

## Requisitos

- Android Studio Koala (2024.1) o superior
- JDK 17
- Dispositivo o emulador con Android 7.0+ (API 24) y cámara

## Cómo ejecutarlo

1. Clona el repositorio:
   ```bash
   git clone https://github.com/TU_USUARIO/android-barcode-scanner.git
   ```
2. Ábrelo con **Android Studio** (`File > Open`). Se descargarán Gradle y las
   dependencias automáticamente.
3. Conecta un dispositivo y pulsa **Run ▶**.

> Si quieres versionar el wrapper de Gradle, ejecuta una vez
> `gradle wrapper --gradle-version 8.7` y haz commit de `gradlew`, `gradlew.bat`
> y `gradle/wrapper/gradle-wrapper.jar`.

## Generar APK

Desde Android Studio: `Build > Build APK(s)`, o por terminal:

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

El repositorio incluye un workflow de **GitHub Actions** (`.github/workflows/build.yml`)
que compila el APK en cada push y lo publica como artefacto descargable.

## Estructura

```
app/src/main/java/com/example/barcodescanner/
├── MainActivity.kt      # UI, permisos, CameraX, acciones
├── BarcodeAnalyzer.kt   # Análisis de fotogramas con ML Kit
└── ScanResult.kt        # Modelo del resultado
```

## Subir a GitHub

```bash
cd android-barcode-scanner
git init
git add .
git commit -m "Primer commit: lector de códigos de barras"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/android-barcode-scanner.git
git push -u origin main
```

## Licencia

MIT
