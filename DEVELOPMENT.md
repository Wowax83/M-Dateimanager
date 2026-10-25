# M-Dateimanager — Entwicklungs-Handbuch

> **Stand:** 25.10.2026 · **Version:** 1.42 (versionCode 43) · **Status:** läuft, 2 offene Punkte (siehe §10)

Dieses Handbuch beschreibt alles, was nötig ist, um die Arbeit an der App wieder aufzunehmen: Pfade, Build-Umgebung, Architektur, Features, Versionshistorie und offene Punkte.

---

## 1. Was ist das Projekt?

**M-Dateimanager** — ein moderner, schneller, minimalistischer und werbefreier Dateimanager für Android. Entstanden aus einem „APK-Extraktor" und zu einem vollwertigen Dateimanager ausgebaut.

Kernfunktionen:
- Speicher-**Dashboard** (Visualisierung, Bereichskarten, typ-spezifische Icons)
- Lokaler **Datei-Explorer** (Mehrfachauswahl, Suche, Sortierung, Fast-Scroll)
- **SMB**- und **FTP/SFTP**-Browsing (Netzwerk)
- **Bild-/Video-Vorschau** mit Wischen (Swipe) zwischen Dateien
- **Bild-Bearbeitung**: drehen + zuschneiden
- **PDF-Vorschau** + **PDF-Bearbeitung**: Seite drehen + zuschneiden — **bleibt ein echtes PDF** (kein Rastern)
- **Archive** erstellen/entpacken (zip, tar, gz, 7z …)
- **Datei-Verschlüsselung** (AES über Android Keystore)
- **Dark/Light-Theme** + Einstellungen

---

## 2. Wichtige Pfade & Repository

| Was | Pfad / Wert |
|---|---|
| Quellcode | `/opt/data/apk-extractor` |
| Artefakte (APK/AAB) | `/store/KI/KI Projekt/M-Dateimanager/` |
| Git-Repository | `git@github.com:Wowax83/M-Dateimanager.git` |
| Branch | `main` |
| Paket / App-ID | `com.wowax.apkextractor` |
| minSdk | 24 (Android 7.0) |
| targetSdk / compileSdk | 36 |

> Hinweis: Das Repo heißt intern noch `ApkExtractor` (rootProject.name), App-ID `com.wowax.apkextractor` — aus der Ursprungs-App beibehalten. Nicht umbenennen, würde App-Updates brechen.

---

## 3. Build-Umgebung (Toolchain)

Alles liegt unter `/opt/data/android-toolchain/`:

| Komponente | Pfad |
|---|---|
| JDK 17 (Temurin) | `/opt/data/android-toolchain/jdk17` |
| Android SDK | `/opt/data/android-toolchain/sdk` |
| Build-Tools | `/opt/data/android-toolchain/sdk/build-tools/36.0.0` |
| Gradle | `/opt/data/android-toolchain/gradle-8.11.1/bin/gradle` |

- **Gradle** 8.11.1 · **AGP** 8.9.1 · **Java** 17
- `minifyEnabled false`, `shrinkResources false` (Release ungeproguardt)

---

## 4. Build & Release (Schritt für Schritt)

```bash
export JAVA_HOME=/opt/data/android-toolchain/jdk17
export ANDROID_HOME=/opt/data/android-toolchain/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:/opt/data/android-toolchain/gradle-8.11.1/bin:$PATH

cd /opt/data/apk-extractor
gradle assembleRelease bundleRelease --no-daemon
```

Ergebnisse:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

**Verifizieren:**
```bash
BT=/opt/data/android-toolchain/sdk/build-tools/36.0.0
$BT/aapt dump badging app/build/outputs/apk/release/app-release.apk | grep versionName
$BT/apksigner verify app/build/outputs/apk/release/app-release.apk   # "SIGNATUR OK" erwartet
```

**Artefakte kopieren + Committen:**
```bash
DEST="/store/KI/KI Projekt/M-Dateimanager"
cp app/build/outputs/apk/release/app-release.apk  "$DEST/M-Dateimanager-<version>.apk"
cp app/build/outputs/bundle/release/app-release.aab "$DEST/M-Dateimanager-<version>.aab"

eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519   # GitHub SSH (ed25519)
git add -A
git commit -m "vX.YY: <Beschreibung>"
git push origin main
```

**Versionsregel:** `versionCode` bei JEDEM Release um 1 erhöhen (Play-Store-Pflicht). Steht in `app/build.gradle` (`versionCode` / `versionName`).

---

## 5. Signierung

- Keystore-Datei: `apk-extractor.keystore` (Repo-Root, via `.gitignore` NICHT eingecheckt)
- Zugangsdaten: `keystore.properties` (NICHT eingecheckt) — Keys: `storePassword`, `keyAlias`, `keyPassword`
- Signing wird in `app/build.gradle` über `signingConfigs.release` konfiguriert.

> ⚠️ Keystore + Passwörter liegen lokal. Ohne diesen Keystore ist ein Play-Store-Update **nicht** möglich (gleiche Signatur nötig).

---

## 6. Architektur

### Activities (11)

| Klasse | Aufgabe |
|---|---|
| `DashboardActivity` | Startbildschirm (Launcher): Speicher-Dashboard, Bereichskarten |
| `MainActivity` | APK-Extraktor (installierte Apps, Extraktion) — Ursprung der App |
| `FileBrowserActivity` | Lokaler Datei-Explorer |
| `SmbBrowserActivity` | SMB-/Netzwerk-Browser |
| `FtpBrowserActivity` | FTP/SFTP-Browser |
| `NetworkActivity` | Netzwerk-Verbindungen verwalten (SMB/FTP) |
| `SettingsActivity` | Einstellungen (Theme: Hell/Dunkel/System) |
| `MediaViewerActivity` | Bild-/Video-Vorschau mit Wischen (ViewPager2) |
| `PdfViewerActivity` | PDF-Vorschau (PdfRenderer) |
| `ImageEditActivity` | Bild drehen + zuschneiden |
| `PdfEditActivity` | PDF drehen + zuschneiden (PDFBox) |

### Adapter
`AppListAdapter`, `FileListAdapter`, `SmbListAdapter`, `FtpListAdapter`

### Daten/Modelle
`AppInfo`, `ExtractResult`, `FileItem`, `SmbItem`, `FtpItem`, `SmbServer`

### Kern-/Hilfsklassen
- `BaseActivity` — **wichtig**: setzt das Theme global via `AppCompatDelegate.setDefaultNightMode` (alle Activities erben davon)
- `KeystoreCrypto` / `FileCrypto` — AES-Verschlüsselung über Android Keystore
- `SmbCredentialStore` — SMB-Zugangsdaten (verschlüsselt)
- `InsetsHelper` — Window-Insets (Statusleiste etc.)
- `FileTypeIcon` — typ-spezifische Datei-Icons
- `ZoomableImageView` — Pinch-Zoom-Bild (extends AppCompatImageView, ScaleType.MATRIX)
- `CropOverlayView` — Zuschneide-Rahmen mit Griffen (Bild + PDF)
- `ApkExtractor`, `ArchiveCreator`, `ArchiveExtractor`, `FolderPickerDialog`, `ProgressDialog`

### Ressourcen
- `values/strings.xml` — deutsche Strings
- `values/colors.xml` + `values-night/colors.xml` — Hell-/Dunkel-Farben
- `values/themes.xml` — `Theme.ApkExtractor` = `Theme.Material3.DayNight.NoActionBar` (adaptive Farben)

---

## 7. Abhängigkeiten (`app/build.gradle`)

| Zweck | Artefakt |
|---|---|
| UI | `androidx.appcompat:appcompat:1.7.0`, `material:1.12.0`, `recyclerview:1.3.2`, `viewpager2:1.1.0` |
| Fast-Scroll | `me.zhanghai.android.fastscroll:library:1.3.0` |
| PDF-Bearbeitung | `com.tom-roush:pdfbox-android:2.0.27.0` (Apache-2.0, Android-Port) |
| SMB | `eu.agno3.jcifs:jcifs-ng:2.1.10` |
| FTP | `commons-net:commons-net:3.11.1` |
| SFTP | `com.github.mwiede:jsch:0.2.24` |
| Krypto | `org.bouncycastle:bcprov-jdk15to18:1.76`, `bcutil-jdk15to18:1.76` |
| Logging | `org.slf4j:slf4j-android:1.7.36` |
| Archive | `org.apache.commons:commons-compress:1.26.2`, `org.tukaani:xz:1.10` |

---

## 8. Theme-System

- Ein einziges Theme `Theme.ApkExtractor` (DayNight). Umschaltung in `BaseActivity.onCreate` via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM | NO | YES)`.
- Auswahl gespeichert in `SharedPreferences` (Schlüssel in `SettingsActivity`).
- Dunkle Farben in `values-night/colors.xml` (`surface` #1E1E1E, `background` #121212).
- Die Media-/PDF-Viewer verwenden hartkodiertes `Color.BLACK` (unabhängig vom Theme).

---

## 9. Versionshistorie (Kurzfassung)

| Version | Inhalt |
|---|---|
| 1.0–1.9 | APK-Extraktor → Umbenennung M-Dateimanager |
| 1.10–1.16 | Dateimanager-Grundfunktionen |
| 1.17–1.21 | AAB/Play-Store, Store-Listing |
| 1.22–1.31 | Fast-Scroll, Suche/Sortierung, Archive, Video-Player, Zurück-Button, Swipe |
| 1.32–1.34 | Video zentriert, Swipe zwischen Bild/Video |
| 1.35 | **Dark Theme + Einstellungen** |
| 1.36 | PDF-Vorschau |
| 1.37 | PDF-Seitensprung |
| 1.38 | Seitenanzeige vergrößert |
| 1.39 | **Bild-Bearbeitung** (drehen + zuschneiden) |
| 1.40 | Größere Zuschneide-Griffe + Buttons |
| 1.41 | **PDF-Bearbeitung** (drehen + zuschneiden, PDFBox) |
| 1.42 | Fix: PDF-Zuschnitt Y-Koordinaten |

(Details in `git log` — Commit-Messages sind deutsch & sprechend.)

---

## 10. Bekannte Probleme / TODO

1. **PNG wird schwarz angezeigt** — offen, Ursache unklar. Benötigt ein Beispiel-PNG zur Reproduktion. Verdacht: Alpha-/Farbraum-Problem in `MediaViewerActivity` → `decodeSampled` → `ZoomableImageView`.
2. **Manche Videos „spielen, aber schwarz"** — Codec-Limit von `VideoView`/`MediaPlayer` (nur Hardware-Decoder). Betrifft z. B. H.265/HEVC 10-Bit, AV1, bestimmte MKV/AVI. Lösung wäre **ExoPlayer/Media3** (Software-Decoder-Fallback) — größere Abhängigkeit, noch nicht entschieden.
3. **PDF-Zuschnitt** nimmt Media-Box-Ursprung (0,0) an — bei exotischen PDFs mit versetztem Media-Box evtl. minimal versetzt.
4. **OCR-Bearbeitung** bewusst NICHT implementiert (Nutzer-Wunsch „ohne OCR").

---

## 11. Wie die Arbeit wieder aufnehmen

1. **Toolchain prüfen** (§3) — Pfade sind auf dem Unraid-Host stabil.
2. **Build ausführen** (§4) — sollte sofort durchlaufen.
3. **Offene Punkte** in §10 abarbeiten.
4. **Version** in `app/build.gradle` für jedes Release erhöhen.
5. **Keystore** nicht verlieren (§5).

> Die App läuft in einem Unraid-Docker (Hermes). Persistente Daten liegen unter `/store`. `/store` unterstützt KEINE Symlinks (npm/gradle mit `EIO`-Fehlern); Schreibarbeit in `/opt/data` und ggf. Symlink ins Ziel. Sandbox erlaubt Schreiben nur unter `/opt/data` (HERMES_WRITE_SAFE_ROOT).
