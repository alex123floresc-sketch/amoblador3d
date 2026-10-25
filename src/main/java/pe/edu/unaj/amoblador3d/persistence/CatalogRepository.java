package pe.edu.unaj.amoblador3d.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Modelos 3D importados por el usuario (tabla catalog_model). */
public final class CatalogRepository {
    /**
     * @param file     ruta del archivo relativa a la carpeta de modelos ({@link AppPaths#modelsDir()})
     * @param rotation grados para que el frente del modelo mire a +Z
     */
    public record ModelRow(String id, String name, String file, double width, double depth, double height, double rotation) {}

    private final Database db;

    public CatalogRepository(Database db) {
        this.db = db;
    }

    public List<ModelRow> list() throws SQLException {
        List<ModelRow> result = new ArrayList<>();
        String sql = "SELECT id, name, file, width, depth, height, rotation FROM catalog_model ORDER BY created_at";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new ModelRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getDouble(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7)));
            }
        }
        return result;
    }

    public void insert(ModelRow m) throws SQLException {
        String sql = "INSERT INTO catalog_model (id, name, file, width, depth, height, rotation, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, m.id());
            ps.setString(2, m.name());
            ps.setString(3, m.file());
            ps.setDouble(4, m.width());
            ps.setDouble(5, m.depth());
            ps.setDouble(6, m.height());
            ps.setDouble(7, m.rotation());
            ps.setString(8, Instant.now().toString());
            ps.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM catalog_model WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}
