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

public final class FSHFAppLog {
    private static final String ROOT = "pe.edu.unaj.amoblador3d";
    private static final int FILE_LIMIT = 1024 * 1024;
    private static final int FILE_COUNT = 5;
    private static volatile Path file;
    private static FileHandler handler;

    private FSHFAppLog() {}

    public static Logger of(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

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

    public static synchronized void close() {
        if (handler == null) return;
        Logger.getLogger(ROOT).removeHandler(handler);
        handler.close();
        handler = null;
        file = null;
    }

    public static Path file() {
        return file;
    }

    public static void catchUncaught(Consumer<Throwable> notify) {
        Logger log = Logger.getLogger(ROOT);
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.log(Level.SEVERE, "Error no controlado en el hilo " + thread.getName(), error);
            try {
                notify.accept(error);
            } catch (RuntimeException ignored) {
            }
        });
    }

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
