package com.mycompany.drive; 

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Drive {

    public static void main(String[] args) {
        // Se crea un ExecutorService con un solo hilo para la ejecución de tareas en segundo plano
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            // Se inicializa el gestor de conexión FTP con los datos de acceso
            final FTPClientManager ftpManager = new FTPClientManager("127.0.0.1", 21, "lorena", "lorena");

            // Se crea un FileWatcher para monitorear cambios en la carpeta local
            FileWatcher watcher = new FileWatcher(ftpManager, "C:/backup_local", "/");

            // Se ejecuta una tarea en segundo plano para descargar y desencriptar un archivo y monitorear cambios
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ftpManager.downloadAndDecryptFile("/prueba.txt", "C:/backup_local/prueba_descifrado.txt");
                        watcher.watch();
                    } catch (Exception e) {
                        System.err.println("File watcher error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Se inicia un nuevo hilo para la gestión del historial de archivos
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Scanner scanner = new Scanner(System.in); 
                    while (true) { 
                        System.out.println("\nFile History Management:");
                        System.out.println("1. List Historical Versions");
                        System.out.println("2. Download Historical Version");
                        System.out.println("3. Exit");
                        System.out.print("Choose an option: ");

                        int choice = scanner.nextInt();
                        scanner.nextLine(); 

                        switch (choice) {
                            case 1:
                                System.out.print("Enter filename to list versions: ");
                                String listFileName = scanner.nextLine();
                                ftpManager.listHistoricalVersions(listFileName);
                                break;
                            case 2:
                                System.out.print("Enter historical file path: ");
                                String historicalPath = scanner.nextLine();
                                System.out.print("Enter local save path: ");
                                String localSavePath = scanner.nextLine();
                                ftpManager.downloadHistoricalFile(historicalPath, localSavePath);
                                break;
                            case 3:
                                scanner.close(); 
                                return;
                            default:
                                System.out.println("Invalid option");
                        }
                    }
                }
            }).start();

            // Hook para manejar el cierre del programa y liberar recursos adecuadamente
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("Shutting down application...");

                    executorService.shutdown(); // Se solicita la terminación del executor
                    try {
                        // Se espera un tiempo límite antes de forzar el cierre
                        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                            executorService.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executorService.shutdownNow();
                        Thread.currentThread().interrupt();
                    }

                    ftpManager.disconnect(); // Se desconecta el gestor FTP
                }
            });

            System.out.println("File synchronization started. Press Ctrl+C to exit.");
            Thread.currentThread().join(); // Se mantiene el hilo principal en espera

        } catch (Exception e) {
            System.err.println("Error in main application: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executorService.shutdown(); // Se asegura que el ExecutorService se cierre correctamente
        }
    }
}
