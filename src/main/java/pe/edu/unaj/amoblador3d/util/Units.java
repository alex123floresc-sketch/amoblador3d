package pe.edu.unaj.amoblador3d.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Redondeo, formato y lectura de medidas. Todas las medidas están en metros. */
public final class Units {
    public static final Locale LOCALE = Locale.forLanguageTag("es-PE");

    private Units() {}

    /** Precisión de 1 cm. */
    public static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    /** 3.5 -> "3.50 m" (con el separador decimal de es-PE). */
    public static String meters(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(value) + " m";
    }

    /** 3.5 -> "3.5"; como mucho 2 decimales. */
    public static String number(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setGroupingUsed(false);
        f.setMaximumFractionDigits(2);
        return f.format(value);
    }

    /** 1234.5 -> "S/ 1,234.50" (soles, con el formato de es-PE). */
    public static String money(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "S/ " + f.format(value);
    }

    /** Lee un número escrito con punto o coma decimal. Devuelve null si no es válido. */
    public static Double parse(String text) {
        if (text == null) return null;
        String t = text.trim().replace(',', '.');
        if (t.isEmpty()) return null;
        try {
            double v = Double.parseDouble(t);
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Verifica que un valor esté en [min, max]. Devuelve un mensaje de error o null. */
    public static String checkRange(String label, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min - 1e-9 || value > max + 1e-9) {
            return label + " debe estar entre " + number(min) + " y " + number(max) + ".";
        }
        return null;
    }
}
