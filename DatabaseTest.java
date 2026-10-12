
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseTest {
    public static void main(String[] args) {
        try (Connection con = DatabaseManager.getConnection();
             Statement stmt = con.createStatement()) {

            System.out.println("Connected to: " + con.getCatalog());

            String[] tables = {"stops", "demands", "buses"};

            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM " + table)) {

                    if (rs.next()) {
                        System.out.println(
                            table + " table: " +
                            rs.getInt(1) + " records"
                        );
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Database test failed!");
            e.printStackTrace();
        }
    }
}
