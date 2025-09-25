package sql;

import java.sql.*;

public class SQL {
    private static final String URL = "jdbc:mysql://localhost:3306/ltm-1604-d03-file-tcp";
    private static final String USER = "root";
    private static final String PASSWORD = "Luck2004!";

    private static Connection conn;

    public static void main(String[] args) {
        connect();
        createUsersTableIfNotExists();

        register("admin", "12345678");
        login("admin", "12345678");

        close();
    }

    public static void connect() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Kết nối MySQL thành công!");
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi kết nối: " + e.getMessage());
        }
    }

    public static void createUsersTableIfNotExists() {
        connect();
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Bảng `users` đã sẵn sàng!");
        } catch (SQLException e) {
            System.out.println("❌ Lỗi tạo bảng: " + e.getMessage());
        }
    }

    public static boolean register(String username, String password) {
        connect();
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            System.out.println("✅ Đăng ký thành công: " + username);
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("⚠️ Username đã tồn tại: " + username);
            } else {
                System.out.println("❌ Lỗi đăng ký: " + e.getMessage());
            }
            return false;
        }
    }

    public static boolean login(String username, String password) {
        connect();
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("✅ Đăng nhập thành công: " + username);
                return true;
            } else {
                System.out.println("❌ Sai tài khoản hoặc mật khẩu.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi đăng nhập: " + e.getMessage());
            return false;
        }
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("⛔ Đã đóng kết nối MySQL.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi đóng kết nối: " + e.getMessage());
        }
    }
}
