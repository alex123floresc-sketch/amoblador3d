package pe.edu.unaj.amoblador3d.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FSHFAppLogTest {

    @Test
    void seGuardanLosMensajesYLasTrazasEnElArchivo(@TempDir Path dir) throws Exception {
        FSHFAppLog.init(dir.resolve("logs"));
        Path file = FSHFAppLog.file();
        assertNotNull(file, "el registro debe abrirse en una carpeta con permisos");

        Logger log = FSHFAppLog.of(FSHFAppLogTest.class);
        log.warning("precio dañado en la base");
        log.log(Level.SEVERE, "falló el guardado", new IllegalStateException("disco lleno"));
        FSHFAppLog.close(); // en Windows la carpeta temporal no se puede borrar con el archivo abierto

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("WARNING [FSHFAppLogTest] precio dañado en la base"), text);
        assertTrue(text.contains("SEVERE [FSHFAppLogTest] falló el guardado"), text);
        assertTrue(text.contains("java.lang.IllegalStateException: disco lleno"), "debe guardar la traza");
    }

    @Test
    void cadaMensajeOcupaUnaLinea() {
        LogRecord r = new LogRecord(Level.INFO, "hola {0}");
        r.setParameters(new Object[] {"mundo"});
        r.setLoggerName("pe.edu.unaj.amoblador3d.FSHFApp");
        String line = new FSHFAppLog.LineFormatter().format(r);
        assertTrue(line.endsWith("INFO [FSHFApp] hola mundo" + System.lineSeparator()), line);
        assertEquals(1, line.lines().count());
    }
}
