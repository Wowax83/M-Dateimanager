# M-Dateimanager

Moderner Dateimanager für Android — mit **APK-Extraktor**, **Datei-Explorer** und **SMB-Netzwerkzugriff**. Kostenlos (Freeware) unter der **MIT-Lizenz**.

> Interner Paketname: `com.wowax.apkextractor` · Aktuelle Version **1.16** (`versionCode 17`)

---

## Funktionen

### 📊 Dashboard
- Übersicht über den **Speicherplatz** (belegt / frei, live via `StatFs`).
- Bereichs-Karten für **APK-Extraktor** und **Datei-Explorer**.

### 📱 APK-Extraktor
- Listet **alle installierten Apps** inkl. Größe & Paketname.
- **Extraktion** installierter APKs und **Installation** eigener APKs.

### 📁 Datei-Explorer
- Vollständige Datei- und Ordneransicht des gemeinsamen Speichers.
- **Typ-spezifische Icons** (PDF, Dokument, Bild, Video, Audio, Archiv, APK, Ordner).
- **Mehrfachauswahl** mit permanenten Checkboxen.
- Operationen: **Kopieren, Verschieben, Umbenennen, Löschen**.

### 🌐 Netzwerk (SMB)
- Zugriff auf **SMB-Freigaben** (jcifs-ng).
- Ordner- und Dateiliste mit typ-spezifischen Icons.
- Operationen: **Herunterladen, Verschieben, Umbenennen, Löschen, Neuer Ordner**.
- **Zugangsdaten** werden **verschlüsselt** gespeichert (Android Keystore, AES-256/GCM).

### 🎨 Design
- Klares, konsistentes **Light-Theme** (Material 3).

---

## Sicherheit

- **Keine hartcodierten Zugangsdaten, Token oder IPs** im Quellcode.
- SMB-Zugangsdaten werden zur Laufzeit eingegeben und **verschlüsselt** über die
  Android-Keystore-basierte `KeystoreCrypto`-Klasse gespeichert (AES-256/GCM).
- **Keine Datensammlung**, keine Analyse-Tracker, keine Netzwerkaufrufe an Dritte
  (nur die vom Nutzer konfigurierten SMB-Server).

---

## Systemvoraussetzungen

- **Android 7.0 (API 24)** oder neuer.
- Für vollen Speicherzugriff (Android 11+) wird die Berechtigung
  **„Alle Dateien"** (`MANAGE_EXTERNAL_STORAGE`) benötigt.

---

## Installation

### APK direkt installieren
Die signierte APK liegt unter den **[Releases](../../releases)**. Datei auf das
Gerät laden, öffnen und ggf. „Installation aus unbekannten Quellen" erlauben.

### Aus dem Quellcode bauen
```bash
# Voraussetzungen: JDK 17, Android SDK 34, Gradle 8.7
export ANDROID_HOME=/pfad/zum/android-sdk
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Signieren für eigene Builds:
```bash
apksigner sign --ks dein.keystore --out app-signed.apk app-release.apk
```

---

## Berechtigungen (Überblick)

| Berechtigung | Zweck |
|---|---|
| `QUERY_ALL_PACKAGES` | Alle installierten Apps anzeigen (APK-Extraktor) |
| `REQUEST_INSTALL_PACKAGES` | APKs installieren |
| `INTERNET` | SMB-Netzwerkzugriff |
| `MANAGE_EXTERNAL_STORAGE` | Voller Dateizugriff (Android 11+) |
| `READ/WRITE_EXTERNAL_STORAGE` | Speicherzugriff (Android ≤ 10) |

---

## Lizenz

**MIT License** — kostenlos nutzbar, veränderbar und weiterverbreitbar.
Vollständiger Text in [`LICENSE`](LICENSE).

> Die Software wird ohne Gewährleistung („as is") bereitgestellt. Keine Haftung
> für Schäden — Details siehe Lizenztext.

---

## Dritt-Bibliotheken

| Bibliothek | Lizenz |
|---|---|
| jcifs-ng | LGPL-2.1 |
| Bouncy Castle (bcprov) | MIT |
| AndroidX / Material Components | Apache-2.0 |
| SLF4J | MIT |
