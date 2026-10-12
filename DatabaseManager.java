
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_NAME = "smart_transport";

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/" + DB_NAME +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String ROOT_URL =
        "jdbc:mysql://localhost:3306/" +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String DB_USER = "root";

    // Replace with your actual MySQL password.
    private static final String DB_PASSWORD = "kavinkalai8007";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC driver not found.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void ensureSchema() {
        try (Connection conn =
                 DriverManager.getConnection(ROOT_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                "CREATE DATABASE IF NOT EXISTS " + DB_NAME
            );

            try (Connection dbConn = getConnection();
                 Statement tableStmt = dbConn.createStatement()) {

                tableStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS stops (" +
                    "stop_id INT PRIMARY KEY, " +
                    "stop_name VARCHAR(100) NOT NULL" +
                    ")"
                );

                tableStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS buses (" +
                    "bus_id INT PRIMARY KEY, " +
                    "bus_type VARCHAR(50) DEFAULT 'StandardBus', " +
                    "capacity INT NOT NULL, " +
                    "available BOOLEAN DEFAULT TRUE" +
                    ")"
                );

                tableStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS demands (" +
                    "request_id INT PRIMARY KEY, " +
                    "pickup_stop INT NOT NULL, " +
                    "destination_stop INT NOT NULL, " +
                    "passengers INT NOT NULL, " +
                    "priority INT DEFAULT 1, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (pickup_stop) REFERENCES stops(stop_id), " +
                    "FOREIGN KEY (destination_stop) REFERENCES stops(stop_id)" +
                    ")"
                );
            }

        } catch (SQLException e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }

    public static void saveStop(Stop stop) {
        String sql =
            "INSERT INTO stops (stop_id, stop_name) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE stop_name = VALUES(stop_name)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, stop.getStopId());
            ps.setString(2, stop.getStopName());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Unable to save stop: " + e.getMessage());
        }
    }

    public static void saveBus(Bus bus) {
        String sql =
            "INSERT INTO buses (bus_id, bus_type, capacity, available) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "bus_type = VALUES(bus_type), " +
            "capacity = VALUES(capacity), " +
            "available = VALUES(available)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bus.getBusId());
            ps.setString(2, bus.getBusType());
            ps.setInt(3, bus.getCapacity());
            ps.setBoolean(4, bus.isAvailable());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Unable to save bus: " + e.getMessage());
        }
    }

    public static void saveDemand(DemandRequest demand) {
        String sql =
            "INSERT INTO demands " +
            "(request_id, pickup_stop, destination_stop, passengers, priority) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "pickup_stop = VALUES(pickup_stop), " +
            "destination_stop = VALUES(destination_stop), " +
            "passengers = VALUES(passengers), " +
            "priority = VALUES(priority)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, demand.getRequestId());
            ps.setInt(2, demand.getPickupStop().getStopId());
            ps.setInt(3, demand.getDestination().getStopId());
            ps.setInt(4, demand.getPassengers());
            ps.setInt(5, demand.getArrivalOrder());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Unable to save demand: " + e.getMessage());
        }
    }

    public static void loadIntoService(TransportService service) {
        ensureSchema();

        service.getStops().clear();
        service.getBuses().clear();
        service.getDemands().clear();
        service.getDemandQueue().clear();

        try (Connection conn = getConnection()) {
            loadStops(conn, service);
            loadBuses(conn, service);
            loadDemands(conn, service);

        } catch (SQLException e) {
            System.err.println(
                "Unable to load data from database: " + e.getMessage()
            );
        }
    }

    private static void loadStops(
            Connection conn, TransportService service) throws SQLException {

        String sql = "SELECT stop_id, stop_name FROM stops ORDER BY stop_id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                service.addStopInternal(
                    new Stop(
                        rs.getInt("stop_id"),
                        rs.getString("stop_name")
                    )
                );
            }
        }
    }

    private static void loadBuses(
            Connection conn, TransportService service) throws SQLException {

        String sql =
            "SELECT bus_id, bus_type, capacity, available " +
            "FROM buses ORDER BY bus_id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int busId = rs.getInt("bus_id");
                int capacity = rs.getInt("capacity");
                boolean available = rs.getBoolean("available");
                String busType = rs.getString("bus_type");

                Bus bus;

                if ("MiniBus".equalsIgnoreCase(busType)) {
                    bus = new MiniBus(busId, capacity);
                } else {
                    bus = new StandardBus(busId, capacity);
                }

                bus.setAvailable(available);
                service.addBusInternal(bus);
            }
        }
    }

    private static void loadDemands(
            Connection conn, TransportService service) throws SQLException {

        String sql =
            "SELECT request_id, pickup_stop, destination_stop, passengers " +
            "FROM demands ORDER BY priority DESC, created_at ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int arrivalOrder = 1;

            while (rs.next()) {
                Stop pickup = service.getStop(rs.getInt("pickup_stop"));
                Stop destination =
                    service.getStop(rs.getInt("destination_stop"));

                if (pickup == null || destination == null) {
                    continue;
                }

                DemandRequest demand = new DemandRequest(
                    rs.getInt("request_id"),
                    pickup,
                    destination,
                    rs.getInt("passengers"),
                    arrivalOrder++
                );

                service.addDemandInternal(demand);
            }
        }
    }
}
