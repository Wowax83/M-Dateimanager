# Datenschutzerklärung — M-Dateimanager

**Stand: Oktober 2026**

Diese Datenschutzerklärung informiert darüber, wie die App **M-Dateimanager**
(Paket `com.wowax.apkextractor`, Entwickler: Waldemar Merdian) mit Daten umgeht.

## Grundsatz

Die App **erhebt, überträgt oder speichert keinerlei personenbezogene Daten**
auf externen Servern. Alle Daten verbleiben ausschließlich auf deinem Gerät.

Die App **enthält keine Analyse-Tracker, keine Werbung und keine Telemetrie**.

## Welche Daten verarbeitet die App?

### 1. Zugangsdaten für SMB-Netzwerkfreigaben
- Wenn du eine Netzwerkfreigabe (SMB) einrichtest, werden **Benutzername und
  Passwort** gespeichert.
- Diese Zugangsdaten werden **ausschließlich lokal** auf deinem Gerät
  gespeichert und **verschlüsselt** (AES-256/GCM über den Android-Keystore).
- Sie verlassen dein Gerät nur, um sich bei dem **von dir selbst
  konfigurierten** SMB-Server anzumelden — an keinen anderen Empfänger.

### 2. Dateizugriff
- Die App greift auf **Dateien und Ordner auf deinem Gerät** zu (Dateimanager,
  APK-Extraktion), um ihre Kernfunktion zu erfüllen.
- Diese Dateien werden **nicht hochgeladen** und nicht ausgewertet.

### 3. Installierte Apps (APK-Extraktor)
- Zur Anzeige der installierten Apps liest die App die **App-Liste** deines
  Geräts. Diese Information wird nur lokal angezeigt und nicht übertragen.

## Werden Daten weitergegeben?

**Nein.** Es erfolgt keinerlei Weitergabe, kein Verkauf und keine Übermittlung
von Daten an Dritte — mit einer Ausnahme: Verbindungen zu dem SMB-Server, den
du selbst in der App einträgst.

## Netzwerkverbindungen

Die App stellt **ausschließlich** Verbindungen zu den von dir konfigurierten
SMB-Servern her. Es gibt keine Verbindungen zu Servern des Entwicklers oder
Dritter.

## Berechtigungen (Erklärung)

| Berechtigung | Zweck |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Voller Dateizugriff für den Dateimanager |
| `QUERY_ALL_PACKAGES` | Liste aller installierten Apps (APK-Extraktor) |
| `INTERNET` | Verbindung zu SMB-Freigaben |
| `READ/WRITE_EXTERNAL_STORAGE` (≤ Android 10) | Speicherzugriff |

## Deine Rechte

Da keine personenbezogenen Daten erhoben werden, gibt es keine zu löschenden
Daten. Du kannst jederzeit:
- gespeicherte SMB-Zugangsdaten in der App löschen,
- der App Berechtigungen entziehen,
- die App deinstallieren (entfernt alle lokalen Daten).

## Kontakt

Bei Fragen zum Datenschutz: **w.merdian@gmail.com**
