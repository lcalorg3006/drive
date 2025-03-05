package com.mycompany.drive;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

class FTPClientManager {

    private final FTPClient ftpClient;
    private final ReentrantLock ftpLock = new ReentrantLock(); // Bloqueo para evitar accesos concurrentes al FTP
    private static final String HISTORY_FOLDER = "/history/"; // Carpeta donde se guardan versiones anteriores de los archivos

    public FTPClientManager(String server, int port, String user, String pass) throws IOException {
        ftpClient = new FTPClient();
        try {
            // Conectar al servidor FTP
            ftpClient.connect(server, port);
            if (ftpClient.login(user, pass)) {
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                // Crear la carpeta de historial si no existe
                if (!ftpClient.changeWorkingDirectory(HISTORY_FOLDER)) {
                    ftpClient.makeDirectory(HISTORY_FOLDER);
                }
                System.out.println("FTP Connection established successfully.");
            } else {
                System.err.println("FTP Authentication error.");
                disconnect();
                throw new IOException("FTP Login failed");
            }
        } catch (IOException e) {
            System.err.println("Error establishing FTP connection: " + e.getMessage());
            throw e;
        }
    }

    // Genera un nombre para las versiones históricas de los archivos
    private String generateHistoryFileName(String originalFileName, String type, int version) {
        String suffix = type.equals("v") ? "v" + version : "b" + version;
        return HISTORY_FOLDER + originalFileName + "_" + suffix;
    }

    // Sube un archivo al servidor FTP
    public void uploadFile(String localFilePath, String remoteFilePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ftpLock.lock();
                try {
                    uploadFileInternal(localFilePath, remoteFilePath);
                } finally {
                    ftpLock.unlock();
                }
            }
        }).start();
    }

    private void uploadFileInternal(String localFilePath, String remoteFilePath) {
        try {
            File file = new File(localFilePath);
            if (!file.exists() || file.isDirectory()) {
                return;
            }

            // Encripta el archivo antes de subirlo
            String encryptedPath = localFilePath + ".enc";
            AESUtil.encryptFile(localFilePath, encryptedPath);

            // Verifica si el archivo remoto ya existe y lo mueve al historial
            try {
                String[] existingFiles = ftpClient.listNames(remoteFilePath);
                if (existingFiles != null && existingFiles.length > 0) {
                    int version = 1;
                    String historyFileName = generateHistoryFileName(new File(remoteFilePath).getName(), "v", version);
                    while (ftpClient.listNames(historyFileName) != null) {
                        version++;
                        historyFileName = generateHistoryFileName(new File(remoteFilePath).getName(), "v", version);
                    }
                    ftpClient.rename(remoteFilePath, historyFileName);
                }
            } catch (IOException ignored) {}

            // Sube el archivo encriptado al servidor FTP
            try (FileInputStream fis = new FileInputStream(encryptedPath)) {
                ftpClient.storeFile(remoteFilePath, fis);
            }

            // Elimina el archivo temporal encriptado
            new File(encryptedPath).delete();

        } catch (Exception e) {
            System.err.println("Error uploading file: " + localFilePath);
            e.printStackTrace();
        }
    }

    // Elimina un archivo del servidor FTP moviéndolo primero al historial
    public void deleteFile(String remoteFilePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ftpLock.lock();
                try {
                    deleteFileInternal(remoteFilePath);
                } finally {
                    ftpLock.unlock();
                }
            }
        }).start();
    }

    private void deleteFileInternal(String remoteFilePath) {
        try {
            int version = 1;
            String historyFileName = generateHistoryFileName(new File(remoteFilePath).getName(), "b", version);
            while (ftpClient.listNames(historyFileName) != null) {
                version++;
                historyFileName = generateHistoryFileName(new File(remoteFilePath).getName(), "b", version);
            }
            ftpClient.rename(remoteFilePath, historyFileName);

            // Elimina el archivo del servidor
            ftpClient.deleteFile(remoteFilePath);
        } catch (IOException e) {
            System.err.println("Error deleting file: " + remoteFilePath);
            e.printStackTrace();
        }
    }

    // Descarga y desencripta un archivo del servidor FTP
    public void downloadAndDecryptFile(String remoteFilePath, String localFilePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ftpLock.lock();
                try {
                    downloadAndDecryptFileInternal(remoteFilePath, localFilePath);
                } finally {
                    ftpLock.unlock();
                }
            }
        }).start();
    }

    private void downloadAndDecryptFileInternal(String remoteFilePath, String localFilePath) {
        try {
            String encryptedLocalFile = localFilePath + ".enc";

            // Descarga el archivo encriptado
            try (FileOutputStream fos = new FileOutputStream(encryptedLocalFile)) {
                if (!ftpClient.retrieveFile(remoteFilePath, fos)) {
                    return;
                }
            }

            // Verifica el tamaño del archivo descargado
            File downloadedFile = new File(encryptedLocalFile);

            // Solo desencripta si es un archivo de texto
            if (remoteFilePath.endsWith(".txt")) {
                AESUtil.decryptFile(encryptedLocalFile, localFilePath);
                new File(encryptedLocalFile).delete();
            }
        } catch (Exception e) {
            System.err.println("Error downloading or decrypting file: " + remoteFilePath);
            e.printStackTrace();
        }
    }

    // Lista las versiones históricas de un archivo en el servidor FTP
    public void listHistoricalVersions(String fileName) {
        ftpLock.lock();
        try {
            String[] historicalFiles = ftpClient.listNames(HISTORY_FOLDER + fileName + "*");

            if (historicalFiles != null && historicalFiles.length > 0) {
                System.out.println("Historical versions of " + fileName + ":");
                for (String historicalFile : historicalFiles) {
                    System.out.println(historicalFile);
                }
            } else {
                System.out.println("No historical versions found for " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Error listing historical versions: " + e.getMessage());
        } finally {
            ftpLock.unlock();
        }
    }

    // Descarga una versión histórica de un archivo del servidor FTP
    public void downloadHistoricalFile(String remoteFilePath, String localFilePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ftpLock.lock();
                try {
                    downloadHistoricalFileInternal(remoteFilePath, localFilePath);
                } finally {
                    ftpLock.unlock();
                }
            }
        }).start();
    }

    private void downloadHistoricalFileInternal(String remoteFilePath, String localFilePath) {
        try {
            String encryptedLocalFile = localFilePath + ".enc";

            // Descarga el archivo histórico
            try (FileOutputStream fos = new FileOutputStream(encryptedLocalFile)) {
                if (!ftpClient.retrieveFile(remoteFilePath, fos)) {
                    return;
                }
            }

            // Desencripta el archivo
            AESUtil.decryptFile(encryptedLocalFile, localFilePath);
            new File(encryptedLocalFile).delete();

        } catch (Exception e) {
            System.err.println("Error downloading or decrypting historical file: " + remoteFilePath);
            e.printStackTrace();
        }
    }

    // Desconecta del servidor FTP liberando la conexión
    public void disconnect() {
        ftpLock.lock();
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                System.out.println("Disconnected from FTP server.");
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting from FTP server.");
            e.printStackTrace();
        } finally {
            ftpLock.unlock();
        }
    }
}