package com.mycompany.drive;

import java.io.IOException;
import java.nio.file.*;

class FileWatcher {
    private FTPClientManager ftpManager;
    private String localFolder;
    private String remoteFolder;

    public FileWatcher(FTPClientManager ftpManager, String localFolder, String remoteFolder) {
        this.ftpManager = ftpManager;
        this.localFolder = localFolder;
        this.remoteFolder = remoteFolder;
    }

    public void watch() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(localFolder);
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, 
                                    StandardWatchEventKinds.ENTRY_DELETE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);

        System.out.println("Monitoreando cambios en: " + localFolder);

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path fileName = (Path) event.context();
                String localFilePath = localFolder + "/" + fileName.toString();
                String remoteFilePath = remoteFolder + "/" + fileName.toString();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    Thread.sleep(2000); 
                    ftpManager.uploadFile(localFilePath, remoteFilePath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    ftpManager.deleteFile(remoteFilePath);
                }
            }
            key.reset();
        }
    }
}
