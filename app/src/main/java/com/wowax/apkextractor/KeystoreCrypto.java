package com.wowax.apkextractor;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Verschlüsselt sensible Zugangsdaten (Benutzername/Passwort) mit einem
 * AES-256-Schlüssel, der im Android Keystore liegt (hardware-gesichert,
 * verlässt nie den Geräte-Keystore).
 */
public class KeystoreCrypto {

    private static final String KEY_ALIAS = "smb_credentials_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String PREFIX = "enc:";
    private static final int GCM_TAG_LENGTH = 128;

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        SecretKey key = (SecretKey) ks.getKey(KEY_ALIAS, null);
        if (key == null) {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();
            kg.init(spec);
            key = kg.generateKey();
        }
        return key;
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

            byte[] combined = new byte[1 + iv.length + encrypted.length];
            combined[0] = (byte) iv.length;
            System.arraycopy(iv, 0, combined, 1, iv.length);
            System.arraycopy(encrypted, 0, combined, 1 + iv.length, encrypted.length);

            return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            return plaintext; // Fallback: unverschlüsselt (sollte nicht passieren)
        }
    }

    public static String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return "";
        if (!encoded.startsWith(PREFIX)) {
            // Alte Daten (vor Verschlüsselung) lagen im Klartext vor.
            return encoded;
        }
        try {
            byte[] combined = Base64.decode(encoded.substring(PREFIX.length()), Base64.NO_WRAP);
            int ivLen = combined[0] & 0xFF;
            byte[] iv = new byte[ivLen];
            byte[] encrypted = new byte[combined.length - 1 - ivLen];
            System.arraycopy(combined, 1, iv, 0, ivLen);
            System.arraycopy(combined, 1 + ivLen, encrypted, 0, encrypted.length);

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            return ""; // nicht entschlüsselbar (z.B. Schlüssel invalidiert) -> leer
        }
    }
}
