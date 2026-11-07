package pe.edu.unaj.amoblador3d.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class FSHFUnits {
    public static final Locale LOCALE = Locale.forLanguageTag("es-PE");

    private FSHFUnits() {}

    public static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    public static String meters(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(value) + " m";
    }

    public static String number(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setGroupingUsed(false);
        f.setMaximumFractionDigits(2);
        return f.format(value);
    }

    public static String money(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "S/ " + f.format(value);
    }

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

    public static String checkRange(String label, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min - 1e-9 || value > max + 1e-9) {
            return label + " debe estar entre " + number(min) + " y " + number(max) + ".";
        }
        return null;
    }
}
