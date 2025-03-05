

package com.mycompany.drive;

public class Drive {
    public static void main(String[] args) {
        try {
            FTPClientManager ftpManager = new FTPClientManager("127.0.0.1", 21, "lorena", "lorena");
            FileWatcher watcher = new FileWatcher(ftpManager, "C:/backup_local", "/");
            ftpManager.downloadAndDecryptFile("/prueba.txt", "C:/backup_local/prueba_descifrado.txt");
            watcher.watch();
        } catch (Exception e) {
            System.err.println(" Error en la aplicación principal.");
            e.printStackTrace();
        }
    }
}


























