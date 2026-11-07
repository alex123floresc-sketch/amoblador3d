package pe.edu.unaj.amoblador3d.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;

public final class FSHFPriceRepository {
    private final FSHFDatabase db;

    public FSHFPriceRepository(FSHFDatabase db) {
        this.db = db;
    }

    public FSHFPriceList load() throws SQLException {
        FSHFPriceList prices = new FSHFPriceList();
        String sql = "SELECT kind, item, price FROM price";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    prices.set(new FSHFPriceList.Key(FSHFPriceList.Kind.fromId(rs.getString(1)), rs.getString(2)), rs.getDouble(3));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return prices;
    }

    public void save(FSHFPriceList.Key key, double price) throws SQLException {
        String sql = "INSERT INTO price (kind, item, price) VALUES (?, ?, ?)"
                + " ON CONFLICT (kind, item) DO UPDATE SET price = excluded.price";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, key.kind().id());
            ps.setString(2, key.item());
            ps.setDouble(3, price);
            ps.executeUpdate();
        }
    }

    public void resetAll() throws SQLException {
        try (Statement st = db.connection().createStatement()) {
            st.executeUpdate("DELETE FROM price");
        }
    }
}
