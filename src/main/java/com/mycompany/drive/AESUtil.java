package com.mycompany.drive;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {
    private static SecretKey secretKey;
    private static final String KEY_FILE = "C:/backup_local/secret.key";

    static {
        try {
            if (Files.exists(Paths.get(KEY_FILE))) {
                loadKey(KEY_FILE);
            } else {
                generateKey();
                saveKey(KEY_FILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generar clave AES
    public static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128, new SecureRandom());
        secretKey = keyGen.generateKey();
    }

    // Guardar la clave en un archivo
    public static void saveKey(String filePath) throws IOException {
        byte[] keyBytes = secretKey.getEncoded();
        String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
        Files.write(Paths.get(filePath), encodedKey.getBytes());
    }

    // Cargar la clave desde un archivo
    public static void loadKey(String filePath) throws IOException {
        byte[] keyBytes = Base64.getDecoder().decode(new String(Files.readAllBytes(Paths.get(filePath))));
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // Cifrar datos
    public static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // Descifrar datos
    public static byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // Cifrar un archivo
    public static void encryptFile(String inputPath, String outputPath) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(inputPath));
        Files.write(Paths.get(outputPath), encrypt(data));
    }

    // Descifrar un archivo
    public static void decryptFile(String inputPath, String outputPath) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(inputPath));
        Files.write(Paths.get(outputPath), decrypt(data));
    }
}
