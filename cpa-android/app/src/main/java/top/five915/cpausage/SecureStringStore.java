package top.five915.cpausage;

import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStringStore {
    private static final String KEYSTORE_NAME = "AndroidKeyStore";
    private static final String KEY_ALIAS = "cpa_usage_secret_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private final SharedPreferences prefs;

    SecureStringStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    String read(String encryptedKey, String legacyPlainKey) {
        String encrypted = prefs.getString(encryptedKey, "");
        if (encrypted != null && encrypted.length() > 0) {
            try {
                return decrypt(encrypted);
            } catch (Exception ignored) {
                remove(encryptedKey, legacyPlainKey);
            }
        }

        String legacy = prefs.getString(legacyPlainKey, "");
        if (legacy != null && legacy.length() > 0) {
            write(encryptedKey, legacyPlainKey, legacy);
            return legacy;
        }
        return "";
    }

    boolean write(String encryptedKey, String legacyPlainKey, String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.length() == 0) {
            remove(encryptedKey, legacyPlainKey);
            return true;
        }
        try {
            String encrypted = encrypt(raw);
            prefs.edit().putString(encryptedKey, encrypted).remove(legacyPlainKey).apply();
            return true;
        } catch (Exception ignored) {
            prefs.edit().remove(encryptedKey).remove(legacyPlainKey).apply();
            return false;
        }
    }

    void remove(String encryptedKey, String legacyPlainKey) {
        prefs.edit().remove(encryptedKey).remove(legacyPlainKey).apply();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Malformed encrypted value");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_NAME);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME);
        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return keyGenerator.generateKey();
    }
}
