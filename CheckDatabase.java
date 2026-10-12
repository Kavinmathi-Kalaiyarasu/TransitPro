
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDatabase {
    public static void main(String[] args) {
        String[] tables = {"buses", "stops", "demands"};

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to smart_transport!");

            for (String table : tables) {
                System.out.println("\n--- " + table.toUpperCase() + " ---");

                try (ResultSet rs =
                         stmt.executeQuery("SELECT * FROM " + table)) {

                    int columns = rs.getMetaData().getColumnCount();
                    int count = 0;

                    for (int i = 1; i <= columns; i++) {
                        System.out.print(rs.getMetaData().getColumnName(i) + "\t");
                    }
                    System.out.println();

                    while (rs.next()) {
                        for (int i = 1; i <= columns; i++) {
                            System.out.print(rs.getString(i) + "\t");
                        }
                        System.out.println();
                        count++;
                    }

                    System.out.println("Total saved records: " + count);
                }
            }

        } catch (Exception e) {
            System.out.println("Database check failed: " + e.getMessage());
        }
    }
}
