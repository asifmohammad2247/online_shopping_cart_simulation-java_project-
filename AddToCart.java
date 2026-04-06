import java.sql.*;
import java.util.Scanner;

public class AddToCart {

    public static void add() {
        Scanner sc = new Scanner(System.in);

        if (!UserSession.isLoggedIn()) {
            System.out.println("❌ Please login first!");
            return;
        }//hello//

        System.out.print("\nEnter Product ID: ");
        int productId = sc.nextInt();

        System.out.print("Enter Quantity: ");
        int qty = sc.nextInt();

        if (qty <= 0) {
            System.out.println("❌ Quantity must be greater than zero.");
            return;
        }

        try {
            Connection con = DBConnection.getConnection();

            // 🔥 0️⃣ DELETE EXPIRED RESERVATIONS (IMPORTANT FIX)
            String deleteExpired =
                "DELETE FROM cart WHERE reserved_at < NOW() - INTERVAL ? MINUTE";
            PreparedStatement psDel = con.prepareStatement(deleteExpired);
            psDel.setInt(1, ReservationConfig.RESERVATION_MINUTES);
            psDel.executeUpdate();

            // 1️⃣ Get product stock
            String stockSql = "SELECT stock FROM product WHERE id = ?";
            PreparedStatement ps1 = con.prepareStatement(stockSql);
            ps1.setInt(1, productId);
            ResultSet rs1 = ps1.executeQuery();

            if (!rs1.next()) {
                System.out.println("❌ Product not found!");
                return;
            }

            int stock = rs1.getInt("stock");

            // 2️⃣ Get ACTIVE reserved quantity (OTHER USERS ONLY)
            String reservedSql =
                "SELECT IFNULL(SUM(quantity), 0) FROM cart " +
                "WHERE product_id = ? AND user_id != ? " +
                "AND reserved_at > NOW() - INTERVAL ? MINUTE";

            PreparedStatement ps2 = con.prepareStatement(reservedSql);
            ps2.setInt(1, productId);
            ps2.setInt(2, UserSession.getUserId());
            ps2.setInt(3, ReservationConfig.RESERVATION_MINUTES);
            ResultSet rs2 = ps2.executeQuery();

            int reservedQty = 0;
            if (rs2.next()) {
                reservedQty = rs2.getInt(1);
            }

            int effectiveStock = stock - reservedQty;

            if (qty > effectiveStock) {
                System.out.println("❌ Cannot add to cart!");
                System.out.println("Available after reservation: " + effectiveStock);
                System.out.println("⏳ Stock is temporarily reserved. Try again later.");
                return;
            }

            // 3️⃣ Check if ACTIVE cart entry exists for THIS USER
            String checkSql =
                "SELECT quantity FROM cart WHERE product_id = ? AND user_id = ? " +
                "AND reserved_at > NOW() - INTERVAL ? MINUTE";

            PreparedStatement ps3 = con.prepareStatement(checkSql);
            ps3.setInt(1, productId);
            ps3.setInt(2, UserSession.getUserId());
            ps3.setInt(3, ReservationConfig.RESERVATION_MINUTES);
            ResultSet rs3 = ps3.executeQuery();

            if (rs3.next()) {
                // ✅ Update ACTIVE row only
                String updateSql =
                    "UPDATE cart SET quantity = quantity + ?, reserved_at = CURRENT_TIMESTAMP " +
                    "WHERE product_id = ? AND user_id = ? " +
                    "AND reserved_at > NOW() - INTERVAL ? MINUTE";

                PreparedStatement ps4 = con.prepareStatement(updateSql);
                ps4.setInt(1, qty);
                ps4.setInt(2, productId);
                ps4.setInt(3, UserSession.getUserId());
                ps4.setInt(4, ReservationConfig.RESERVATION_MINUTES);
                ps4.executeUpdate();

            } else {
                // ✅ Insert NEW row (expired ones already deleted)
                String insertSql =
                    "INSERT INTO cart(product_id, quantity, user_id, reserved_at) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

                PreparedStatement ps5 = con.prepareStatement(insertSql);
                ps5.setInt(1, productId);
                ps5.setInt(2, qty);
                ps5.setInt(3, UserSession.getUserId());
                ps5.executeUpdate();
            }

            System.out.println("✔ Added to cart successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
