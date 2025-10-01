package sql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SQL {
    private static final HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault(
                "DB_URL", "jdbc:mysql://localhost:3306/ltm_1604_d03_file_tcp?useSSL=false&serverTimezone=UTC"));
        config.setUsername(System.getenv().getOrDefault("DB_USER", "root"));
        config.setPassword(System.getenv().getOrDefault("DB_PASS", "Luck2004!"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        ds = new HikariDataSource(config);
    }

    private static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    // ====== Tạo bảng ======
    public static void createUsersTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password CHAR(60) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Bảng 'users' đã sẵn sàng!");
        } catch (SQLException e) {
            System.err.println("Lỗi tạo bảng users: " + e.getMessage());
        }
    }

    public static void createHistoryTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS file_history (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sender VARCHAR(50) NOT NULL,
                receiver VARCHAR(50) NOT NULL,
                file_name VARCHAR(255) NOT NULL,
                file_size BIGINT NOT NULL,
                status ENUM('SENT','RECEIVED') NOT NULL,
                save_path VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Bảng 'file_history' đã sẵn sàng!");
        } catch (SQLException e) {
            System.err.println("Lỗi tạo bảng file_history: " + e.getMessage());
        }
    }

    // ====== Validate ======
    private static boolean validateUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9_]{3,20}");
    }

    private static boolean validatePassword(String password) {
        return password != null && password.length() >= 8;
    }

    private static boolean userExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ====== Đăng ký & đăng nhập ======
    public static String tryRegister(String username, String password) {
        if (!validateUsername(username)) return "USERNAME_INVALID";
        if (!validatePassword(password)) return "PASSWORD_TOO_SHORT";

        try (Connection conn = getConnection()) {
            if (userExists(conn, username)) return "USERNAME_EXISTS";

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashed);
                ps.executeUpdate();
                return "REGISTER_OK";
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký: " + e.getMessage());
            return "REGISTER_FAIL";
        }
    }

    public static boolean login(String username, String password) {
        if (!validateUsername(username) || password == null || password.isEmpty()) return false;

        String sql = "SELECT password FROM users WHERE username=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashed = rs.getString("password");
                    return BCrypt.checkpw(password, hashed);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đăng nhập: " + e.getMessage());
        }
        return false;
    }

    // ====== Lịch sử gửi nhận file ======
    public static void saveFileHistory(String sender, String receiver, String fileName,
                                       long fileSize, String status, String savePath) {
        if (!"SENT".equals(status) && !"RECEIVED".equals(status)) status = "SENT"; // default
        String sql = "INSERT INTO file_history(sender, receiver, file_name, file_size, status, save_path) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, fileName);
            ps.setLong(4, fileSize);
            ps.setString(5, status);
            ps.setString(6, savePath);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi lưu lịch sử: " + e.getMessage());
        }
    }

    public static List<String[]> getHistory(String username) {
        List<String[]> historyList = new ArrayList<>();
        String sql = """
            SELECT created_at, sender, receiver, file_name, file_size, status, save_path
            FROM file_history
            WHERE sender=? OR receiver=?
            ORDER BY created_at DESC
            LIMIT 50
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historyList.add(new String[]{
                        rs.getTimestamp("created_at").toString(),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("file_name"),
                        String.valueOf(rs.getLong("file_size")),
                        rs.getString("status"),
                        rs.getString("save_path")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử: " + e.getMessage());
        }
        return historyList;
    }

    // ====== Main Test ======
    public static void main(String[] args) {
        createUsersTableIfNotExists();
        createHistoryTableIfNotExists();

        String result = tryRegister("admin_01", "12345678");
        System.out.println("Kết quả đăng ký: " + result);

        if (login("admin_01", "12345678")) {
            System.out.println("Login admin OK.");
        } else {
            System.out.println("Login admin FAIL.");
        }

        saveFileHistory("clientA", "clientB", "abc.txt", 12345, "SENT", "C:/downloads/abc.txt");

        List<String[]> history = getHistory("clientA");
        if (history.isEmpty()) {
            System.out.println("Không có lịch sử.");
        } else {
            for (String[] record : history) {
                System.out.println("[" + record[0] + "] " + record[1] + " → " + record[2] +
                        " | File: " + record[3] + " (" + record[4] + " bytes) | Trạng thái: " + record[5] +
                        " | Lưu tại: " + record[6]);
            }
        }
    }
}
