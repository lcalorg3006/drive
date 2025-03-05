FTPClientManager
Descripción
FTPClientManager es una aplicación en Java que permite gestionar archivos en un servidor FTP con funcionalidades avanzada- Subida y descarga de archivos con cifrado AES.
- Gestión de versiones históricas para mantener copias previas de los archivos.
- Eliminación segura de archivos, trasladándolos primero a un historial.
- Operaciones concurrentes protegidas con ReentrantLock para evitar conflictos en la conexión FTP.
Características
- Cifrado AES para garantizar la seguridad de los archivos transferidos.
- Manejo de versiones: Guarda versiones anteriores de los archivos antes de sobrescribir o eliminar.
- Soporte multihilo: Ejecuta operaciones en segundo plano sin bloquear la aplicación.
- Registro de versiones históricas de archivos en la carpeta /history/.
- Descarga y desencriptación automática de archivos.
Estructura del Proyecto
FTPClientManager/
 src/com/mycompany/drive/
 FTPClientManager.java # Clase principal de gestión FTP
 AESUtil.java # Clase para cifrado y descifrado de archivos
 pom.xml # Configuración del proyecto (si usa Maven)
 README.md # Documentación del proyecto
Instalación y Configuración
Requisitos previos
1. Tener instalado Java 8+.
2. Agregar la dependencia de Apache Commons Net para gestionar FTP:
 - Si usas Maven, agrega esto en pom.xml:
 <dependency>
 <groupId>commons-net</groupId>
 <artifactId>commons-net</artifactId>
 <version>3.6</version>
 </dependency>
 - O descargar manualmente commons-net-3.6.jar y agregarlo al classpath.
Configuración de conexión FTP
Antes de usar el cliente, asegúrate de configurar correctamente los parámetros en FTPClientManager:
FTPClientManager ftpManager = new FTPClientManager("ftp.server.com", 21, "usuario", "contraseña");
Uso
Subir un archivo
ftpManager.uploadFile("C:/miarchivo.txt", "/servidor/miarchivo.txt");
Descargar y desencriptar un archivo
ftpManager.downloadAndDecryptFile("/servidor/miarchivo.txt", "C:/descargas/miarchivo.txt");
Eliminar un archivo (mover a historial primero)
ftpManager.deleteFile("/servidor/miarchivo.txt");
Listar versiones históricas de un archivo
ftpManager.listHistoricalVersions("miarchivo.txt");
Descargar una versión histórica
ftpManager.downloadHistoricalFile("/history/miarchivo.txt_v1", "C:/descargas/miarchivo_v1.txt");
Desconectar del servidor
ftpManager.disconnect();
