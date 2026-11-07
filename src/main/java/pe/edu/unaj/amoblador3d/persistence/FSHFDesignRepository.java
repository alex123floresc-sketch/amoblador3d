package pe.edu.unaj.amoblador3d.persistence;

import com.google.gson.JsonParser;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign;

public final class FSHFDesignRepository {
    private final FSHFDatabase db;

    public FSHFDesignRepository(FSHFDatabase db) {
        this.db = db;
    }

    public List<FSHFCustomDesign> list(List<String> warnings) throws SQLException {
        List<FSHFCustomDesign> result = new ArrayList<>();
        String sql = "SELECT id, name, data FROM custom_design ORDER BY created_at";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    FSHFCustomDesign d = FSHFCustomDesign.fromJson(JsonParser.parseString(rs.getString(3)).getAsJsonObject());
                    d.id = rs.getString(1);
                    result.add(d);
                } catch (RuntimeException e) {
                    warnings.add("El mueble diseñado \"" + rs.getString(2) + "\" está dañado: " + e.getMessage());
                }
            }
        }
        return result;
    }

    public void save(FSHFCustomDesign d) throws SQLException {
        String error = d.validate();
        if (error != null) throw new SQLException(error);
        String now = Instant.now().toString();
        String sql = "INSERT INTO custom_design (id, name, data, created_at, updated_at) VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT(id) DO UPDATE SET name = excluded.name, data = excluded.data, updated_at = excluded.updated_at";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, d.id);
            ps.setString(2, d.name.trim());
            ps.setString(3, d.toJson().toString());
            ps.setString(4, now);
            ps.setString(5, now);
            ps.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM custom_design WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}
