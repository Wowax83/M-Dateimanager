package com.wowax.apkextractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verschlüsselt/entschlüsselt einzelne Dateien mit AES-256-GCM.
 * Schlüssel wird aus dem Passwort per PBKDF2 (SHA-1, 200k Iterationen) abgeleitet.
 * Dateiformat: [Salt 16 B][IV 12 B][Ciphertext+Tag].
 */
public class FileCrypto {

    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 200000;
    private static final int KEY_BITS = 256;
    private static final String EXT = ".enc";

    public static File encrypt(File src, String password) throws Exception {
        File dest = new File(src.getParentFile(), src.getName() + EXT);
        if (dest.exists()) throw new Exception("Zieldatei existiert bereits: " + dest.getName());

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(salt);
        rnd.nextBytes(iv);
        SecretKeySpec key = deriveKey(password, salt);

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            out.write(salt);
            out.write(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) cos.write(buf, 0, n);
            }
        }
        return dest;
    }

    public static File decrypt(File src, String password) throws Exception {
        String name = src.getName();
        String base = name.endsWith(EXT)
                ? name.substring(0, name.length() - EXT.length())
                : name + ".dec";
        File dest = new File(src.getParentFile(), base);
        if (dest.exists()) throw new Exception("Zieldatei existiert bereits: " + dest.getName());

        try (FileInputStream in = new FileInputStream(src)) {
            byte[] salt = new byte[SALT_LEN];
            byte[] iv = new byte[IV_LEN];
            if (in.read(salt) != SALT_LEN || in.read(iv) != IV_LEN) {
                throw new Exception("Ungültige oder beschädigte Datei");
            }
            SecretKeySpec key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            try (CipherInputStream cis = new CipherInputStream(in, cipher);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = cis.read(buf)) > 0) out.write(buf, 0, n);
            }
        }
        return dest;
    }

    public static boolean isEncrypted(String name) {
        return name != null && name.toLowerCase().endsWith(EXT);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = f.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
