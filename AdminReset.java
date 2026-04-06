import java.sql.*;

public class AdminReset {

    public static void resetSystem() {
        try {
            Connection con = DBConnection.getConnection();

            con.createStatement().executeUpdate("TRUNCATE TABLE cart");
            con.createStatement().executeUpdate("TRUNCATE TABLE order_history");
            con.createStatement().executeUpdate("UPDATE product SET stock = original_stock");

            System.out.println("✔ System reset successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }//hello//
}
