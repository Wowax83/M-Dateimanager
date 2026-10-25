# M-Dateimanager

**Schnell · Minimalistisch · Werbefrei** — ein schlanker Dateimanager für Android, ohne Schnickschnack.

Ein ressourcenschonender Dateimanager mit **APK-Extraktor**, **Datei-Explorer** und **Netzwerkzugriff** (SMB / FTP / SFTP). Kostenlos, **ohne Werbung**, **ohne Tracker**, **ohne Datensammlung**. Open Source unter der **MIT-Lizenz**.

> Interner Paketname: `com.wowax.apkextractor` · Aktuelle Version **1.34** (`versionCode 35`) · Android 7.0+

---

## Warum M-Dateimanager?

- ⚡ **Schnell** — bewusst schlank gehalten: keine unnötigen Hintergrundprozesse, Dateioperationen laufen direkt und verzögerungsfrei.
- 🧘 **Minimalistisch** — klare Oberfläche, nur das Nötigste, kein Bloat, kein Konto-Zwang.
- 🚫 **Werbefrei** — keine Werbung, keine Analyse-Tracker, keine Datensammlung, keine Netzwerkaufrufe an Dritte.

---

## Funktionen

### 📊 Dashboard
- Live-Übersicht des **Speicherplatzes** (belegt / frei).
- Bereichs-Karten für APK-Extraktor, Datei-Explorer und Netzwerk.

### 📱 APK-Extraktor
- Listet **alle installierten Apps** inkl. Größe und Paketname.
- **Extraktion** installierter APKs.

### 📁 Datei-Explorer
- Vollständige Datei- und Ordneransicht des gemeinsamen Speichers.
- **Typ-spezifische Icons** (PDF, Dokument, Bild, Video, Audio, Archiv, APK, Ordner).
- **Mehrfachauswahl** mit permanenten Checkboxen.
- Operationen: **Kopieren, Verschieben, Umbenennen, Löschen** (mit Fortschritt + Abbruch).
- **Suche**, **Sortierung** (Name/Größe/Datum/Typ), **versteckte Dateien**, **Favoriten**.
- **Archive**: ZIP, 7z, TAR, TAR.GZ, GZ entpacken; ZIP, TAR, TAR.GZ erstellen.
- **Datei-Verschlüsselung** (AES-256-GCM, Passwort + PBKDF2).
- **Bild-/Video-Vorschau** mit Wisch-Navigation, Pinch-Zoom, Play/Stop + Zeitleiste.
- **Fast-Scroll**-Leiste für schnelles Blättern in langen Ordnern.

### 🌐 Netzwerk (SMB / FTP / SFTP)
- Zugriff auf **SMB-Freigaben**, **FTP**- und **SFTP**-Server.
- Ordner- und Dateiliste mit typ-spezifischen Icons.
- Operationen: **Herunterladen, Verschieben, Umbenennen, Löschen, Neuer Ordner**.
- **Zugangsdaten** werden **verschlüsselt** gespeichert (Android Keystore, AES-256/GCM).

### 🎨 Design
- Klares, konsistentes **Light-Theme** (Material 3).

---

## Sicherheit & Datenschutz

- **Werbefrei** — keine Werbung, keine Drittanbieter-Netzwerkaufrufe.
- **Keine Datensammlung**, keine Analyse-Tracker.
- **Keine hartcodierten Zugangsdaten, Token oder IPs** im Quellcode.
- SMB/FTP/SFTP-Zugangsdaten werden **verschlüsselt** über die Android-Keystore-basierte `KeystoreCrypto` gespeichert (AES-256/GCM).
- Datei-Verschlüsselung mit AES-256-GCM und PBKDF2.

---

## Systemvoraussetzungen

- **Android 7.0 (API 24)** oder neuer.
- Für vollen Speicherzugriff (Android 11+) wird die Berechtigung **„Alle Dateien“** (`MANAGE_EXTERNAL_STORAGE`) benötigt.

---

## Installation

### APK direkt installieren
Die signierte APK liegt unter den **[Releases](../../releases)**. Datei auf das Gerät laden, öffnen und ggf. „Installation aus unbekannten Quellen“ erlauben.

### Aus dem Quellcode bauen
```bash
# Voraussetzungen: JDK 17, Android SDK 36, Gradle 8.11.1
export ANDROID_HOME=/pfad/zum/android-sdk
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

---

## Berechtigungen (Überblick)

| Berechtigung | Zweck |
|---|---|
| `QUERY_ALL_PACKAGES` | Alle installierten Apps anzeigen (APK-Extraktor) |
| `INTERNET` | Netzwerkzugriff (SMB / FTP / SFTP) |
| `MANAGE_EXTERNAL_STORAGE` | Voller Dateizugriff (Android 11+) |
| `READ/WRITE_EXTERNAL_STORAGE` | Speicherzugriff (Android ≤ 10) |

---

## Lizenz

**MIT License** — kostenlos nutzbar, veränderbar und weiterverbreitbar.
Vollständiger Text in [`LICENSE`](LICENSE).

> Die Software wird ohne Gewährleistung („as is“) bereitgestellt. Keine Haftung für Schäden — Details siehe Lizenztext.

---

## Dritt-Bibliotheken

| Bibliothek | Lizenz |
|---|---|
| jcifs-ng (SMB) | LGPL-2.1 |
| Bouncy Castle (bcprov) | MIT |
| Apache Commons Net (FTP) | Apache-2.0 |
| JSch / mwiede-Fork (SFTP) | BSD-3-Clause |
| Apache Commons Compress + XZ | Apache-2.0 |
| AndroidFastScroll (zhanghai) | Apache-2.0 |
| AndroidX / Material Components | Apache-2.0 |
| SLF4J | MIT |
