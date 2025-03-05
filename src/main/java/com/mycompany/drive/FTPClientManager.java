package com.mycompany.drive;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class FTPClientManager {

    private FTPClient ftpClient;

    public FTPClientManager(String server, int port, String user, String pass) throws IOException {
        ftpClient = new FTPClient();
        ftpClient.connect(server, port);
        if (ftpClient.login(user, pass)) {
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println(" Conexión FTP establecida con éxito.");
        } else {
            System.err.println(" Error en la autenticación FTP.");
            disconnect();
        }
    }

    public void uploadFile(String localFilePath, String remoteFilePath) {
        File file = new File(localFilePath);
        if (!file.exists() || file.isDirectory()) {
            System.err.println(" Archivo no encontrado o es un directorio: " + localFilePath);
            return;
        }

        try {
            String encryptedPath = localFilePath + ".enc";
            if (localFilePath.endsWith(".txt")) {
                AESUtil.encryptFile(localFilePath, encryptedPath);
            } else {
                encryptedPath = localFilePath; 
            }

            try (FileInputStream fis = new FileInputStream(encryptedPath)) {
                if (ftpClient.storeFile(remoteFilePath, fis)) {
                    System.out.println(" Archivo subido correctamente: " + remoteFilePath);
                } else {
                    System.err.println(" Error al subir el archivo: " + remoteFilePath);
                }
            }

            if (localFilePath.endsWith(".txt")) {
                new File(encryptedPath).delete();
            }
        } catch (IOException e) {  
            System.err.println(" Error de E/S al subir el archivo: " + localFilePath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(" Error de E/S o cifrado al subir el archivo: " + localFilePath);
            e.printStackTrace();
        }
    }

    public void downloadAndDecryptFile(String remoteFilePath, String localFilePath) {
        String encryptedLocalFile = localFilePath + ".enc";

        try (FileOutputStream fos = new FileOutputStream(encryptedLocalFile)) {
            if (ftpClient.retrieveFile(remoteFilePath, fos)) {
                System.out.println("Archivo cifrado descargado correctamente: " + remoteFilePath);
            } else {
                System.err.println("Error al descargar el archivo: " + remoteFilePath);
                return;
            }
        } catch (IOException e) {
            System.err.println("Error de E/S al descargar el archivo: " + remoteFilePath);
            e.printStackTrace();
            return;
        }

        if (remoteFilePath.endsWith(".txt")) {
            try {
                AESUtil.decryptFile(encryptedLocalFile, localFilePath);
                System.out.println("Archivo descifrado correctamente: " + localFilePath);
                new File(encryptedLocalFile).delete(); 
            } catch (Exception e) {
                System.err.println("Error al descifrar el archivo: " + localFilePath);
                e.printStackTrace();
            }
        }
    }

    public void deleteFile(String remoteFilePath) {
        try {
            if (ftpClient.deleteFile(remoteFilePath)) {
                System.out.println(" Archivo eliminado correctamente: " + remoteFilePath);
            } else {
                System.err.println(" Error al eliminar el archivo: " + remoteFilePath);
            }
        } catch (IOException e) {
            System.err.println(" Error de E/S al eliminar el archivo: " + remoteFilePath);
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                System.out.println(" Desconectado del servidor FTP.");
            }
        } catch (IOException e) {
            System.err.println(" Error al desconectarse del servidor FTP.");
            e.printStackTrace();
        }
    }
}
