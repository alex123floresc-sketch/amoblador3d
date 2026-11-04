package pe.edu.unaj.amoblador3d.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Registro de la aplicación (bitácora de errores), sin librerías extra: usa {@code java.util.logging}.
 *
 * <p>Escribe en {@code <carpeta de datos>/logs/amoblador3d-0.log} (5 archivos de 1 MB que rotan).
 * Cuando un usuario reporta un problema, ese archivo dice qué pasó: versión, Java, sistema y la
 * traza completa del error, que antes se perdía porque la aplicación no la guardaba en ningún lado.
 */
public final class AppLog {
    private static final String ROOT = "pe.edu.unaj.amoblador3d";
    private static final int FILE_LIMIT = 1024 * 1024;
    private static final int FILE_COUNT = 5;
    private static volatile Path file;
    private static FileHandler handler;

    private AppLog() {}

    /** Logger para una clase de la aplicación: {@code AppLog.of(MiClase.class).warning(...)}. */
    public static Logger of(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /**
     * Abre el archivo de registro en {@code logsDir}. Si no se puede (carpeta sin permisos, disco
     * lleno), la aplicación sigue igual: solo se pierde el registro.
     */
    public static synchronized void init(Path logsDir) {
        if (file != null) return;
        Logger root = Logger.getLogger(ROOT);
        try {
            Files.createDirectories(logsDir);
            handler = new FileHandler(logsDir.resolve("amoblador3d-%g.log").toString(),
                    FILE_LIMIT, FILE_COUNT, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new LineFormatter());
            handler.setLevel(Level.INFO);
            root.addHandler(handler);
            root.setLevel(Level.INFO);
            file = logsDir.resolve("amoblador3d-0.log");
        } catch (IOException | SecurityException e) {
            Logger.getLogger(ROOT).log(Level.WARNING, "No se pudo abrir el registro en " + logsDir, e);
        }
    }

    /** Cierra el archivo de registro (al salir de la aplicación). */
    public static synchronized void close() {
        if (handler == null) return;
        Logger.getLogger(ROOT).removeHandler(handler);
        handler.close();
        handler = null;
        file = null;
    }

    /** Archivo de registro actual, o {@code null} si no se pudo abrir. */
    public static Path file() {
        return file;
    }

    /**
     * Registra los errores que nadie atrapó (en cualquier hilo, incluido el de JavaFX) y luego
     * avisa al usuario con {@code notify}, para que no se pierdan en silencio.
     */
    public static void catchUncaught(Consumer<Throwable> notify) {
        Logger log = Logger.getLogger(ROOT);
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.log(Level.SEVERE, "Error no controlado en el hilo " + thread.getName(), error);
            try {
                notify.accept(error);
            } catch (RuntimeException ignored) {
                // si ni siquiera se puede avisar, al menos quedó en el registro
            }
        });
    }

    /** Una línea por mensaje: {@code 2026-11-03 19:10:05 WARNING [Clase] mensaje}, más la traza. */
    static final class LineFormatter extends Formatter {
        private static final DateTimeFormatter TIME =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord r) {
            String source = r.getLoggerName() == null ? "" : r.getLoggerName();
            int dot = source.lastIndexOf('.');
            StringBuilder sb = new StringBuilder()
                    .append(TIME.format(Instant.ofEpochMilli(r.getMillis()))).append(' ')
                    .append(r.getLevel().getName()).append(" [").append(source.substring(dot + 1)).append("] ")
                    .append(formatMessage(r)).append(System.lineSeparator());
            if (r.getThrown() != null) {
                StringWriter trace = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(trace));
                sb.append(trace);
            }
            return sb.toString();
        }
    }
}
