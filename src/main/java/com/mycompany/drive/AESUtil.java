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
    private static SecretKey secretKey; // Clave secreta para cifrado AES
    private static final String KEY_FILE = "C:/backup_local/secret.key"; // Ruta del archivo donde se almacena la clave

    // Cargar la clave al iniciar la clase
    static {
        try {
            if (Files.exists(Paths.get(KEY_FILE))) {
                loadKey(KEY_FILE); // Si existe la clave, se carga desde el archivo
            } else {
                generateKey(); // Si no existe, se genera una nueva clave
                saveKey(KEY_FILE); // Se guarda la nueva clave en un archivo
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para generar una clave AES de 128 bits
    public static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128, new SecureRandom()); // Inicialización con número aleatorio seguro
        secretKey = keyGen.generateKey();
    }

    // Método para guardar la clave en un archivo en formato Base64
    public static void saveKey(String filePath) throws IOException {
        byte[] keyBytes = secretKey.getEncoded(); // Obtener los bytes de la clave  
        String encodedKey = Base64.getEncoder().encodeToString(keyBytes); // Codificar en Base64
        Files.write(Paths.get(filePath), encodedKey.getBytes()); // Guardar la clave en el archivo
    }

    // Método para cargar la clave desde un archivo
    public static void loadKey(String filePath) throws IOException {
        byte[] keyBytes = Base64.getDecoder().decode(new String(Files.readAllBytes(Paths.get(filePath)))); // Leer y decodificar la clave
        secretKey = new SecretKeySpec(keyBytes, "AES"); // Restaurar la clave en el formato AES
    }

    // Método para cifrar datos con AES
    public static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES"); // Obtener una instancia del cifrador AES
        cipher.init(Cipher.ENCRYPT_MODE, secretKey); // Inicializar el cifrador en modo de cifrado
        return cipher.doFinal(data); // Cifrar los datos y devolver el resultado
    }

    // Método para descifrar datos con AES
    public static byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES"); // Obtener una instancia del cifrador AES
        cipher.init(Cipher.DECRYPT_MODE, secretKey); // Inicializar el cifrador en modo de descifrado
        return cipher.doFinal(data); // Descifrar los datos y devolver el resultado
    }

    // Método para cifrar un archivo y guardar el resultado en otro archivo
    public static void encryptFile(String inputPath, String outputPath) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(inputPath)); // Leer el archivo original
        Files.write(Paths.get(outputPath), encrypt(data)); // Cifrar y guardar en el archivo de salida
    }

    // Método para descifrar un archivo y guardar el resultado en otro archivo
    public static void decryptFile(String inputPath, String outputPath) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(inputPath)); // Leer el archivo cifrado
        Files.write(Paths.get(outputPath), decrypt(data)); // Descifrar y guardar en el archivo de salida
    }
}
