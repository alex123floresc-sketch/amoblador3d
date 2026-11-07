package pe.edu.unaj.amoblador3d.design;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FSHFIdGenerator {
    private static final Pattern NUMBERED = Pattern.compile("^(.*)-(\\d+)$");
    private final Map<String, Integer> counters = new HashMap<>();

    String next(String prefix) {
        int n = counters.merge(prefix, 1, Integer::sum);
        return prefix + "-" + n;
    }

    void register(String id) {
        Matcher m = NUMBERED.matcher(id);
        if (!m.matches()) return;
        try {
            counters.merge(m.group(1), Integer.parseInt(m.group(2)), Math::max);
        } catch (NumberFormatException ignored) {
        }
    }

    void reset() {
        counters.clear();
    }
}
