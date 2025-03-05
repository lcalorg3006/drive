package com.mycompany.drive;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FileWatcher {
    private final FTPClientManager ftpManager; // Gestor de conexión FTP
    private final String localFolder; // Carpeta local a monitorear
    private final String remoteFolder; // Carpeta remota en el servidor FTP
    private final ExecutorService executorService; // Servicio de hilos para manejar tareas concurrentes

    public FileWatcher(FTPClientManager ftpManager, String localFolder, String remoteFolder) {
        this.ftpManager = ftpManager;
        this.localFolder = localFolder;
        this.remoteFolder = remoteFolder;
        this.executorService = Executors.newFixedThreadPool(5); // Pool de hilos con 5 hilos disponibles
    }

    // Método que monitorea cambios en la carpeta local
    public void watch() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(localFolder);
        
        // Registra el servicio  para detectar eventos de creación, eliminación y modificación de archivos
        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        // Agrega un shutdown hook para cerrar correctamente los recursos cuando la aplicación se detiene
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                executorService.shutdown();
                try {
                    watchService.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        // Bucle infinito para detectar cambios en la carpeta
        while (true) {
            WatchKey key = watchService.take(); // Espera un evento de cambio en la carpeta

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path fileName = (Path) event.context();
                String localFilePath = localFolder + "/" + fileName.toString();
                String remoteFilePath = remoteFolder + "/" + fileName.toString();

                // Envía la tarea al pool de hilos para manejar la subida o eliminación del archivo en el servidor FTP
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Thread.sleep(2000); // Espera 2 segundos para evitar conflictos con archivos en uso
                                ftpManager.uploadFile(localFilePath, remoteFilePath);
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                ftpManager.deleteFile(remoteFilePath);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restaura el estado de interrupción del hilo
                        }
                    }
                });
            }
            key.reset(); // Reinicia la clave para seguir monitoreando cambios
        }
    }
}
