package server;

import sql.SQL;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Server extends JFrame {
    private JTextPane logPane;
    private StyledDocument logDoc;
    private JButton startButton, stopButton;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ExecutorService clientPool;

    public Server() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
            UIManager.put("Button.arc", 18);
            UIManager.put("Component.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo FlatLaf");
        }

        setTitle("File Transfer Server");
        setSize(820, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopServer();
            logInfo("Server JVM exit, tất cả kết nối đóng.");
        }));
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("File Transfer Server");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(52, 152, 219));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        logPane.setBackground(new Color(25, 27, 35));
        logPane.setCaretColor(Color.WHITE);
        logDoc = logPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        startButton = createButton("Bắt đầu Server", new Color(39, 174, 96));
        stopButton = createButton("Dừng Server", new Color(231, 76, 60));
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ==== Logging helper ====
    private void log(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            Style style = logPane.addStyle("Style", null);
            StyleConstants.setForeground(style, color);
            try {
                logDoc.insertString(logDoc.getLength(), msg + "\n", style);
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void logInfo(String msg) { log(msg, new Color(39, 174, 96)); }
    private void logWarn(String msg) { log(msg, new Color(243, 156, 18)); }
    private void logError(String msg) { log(msg, new Color(231, 76, 60)); }

    // ==== Server control ====
    private void startServer() {
        try {
            SQL.createUsersTableIfNotExists();
            SQL.createHistoryTableIfNotExists();

            serverSocket = new ServerSocket(12345);
            running = true;
            clientPool = Executors.newCachedThreadPool();
            logInfo("Server đang chạy tại cổng 12345");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        clientPool.submit(handler);
                    } catch (IOException e) {
                        if (running) logError("Lỗi khi chấp nhận client: " + e.getMessage());
                    }
                }
            }).start();

        } catch (IOException e) {
            logError("Không thể khởi động server: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            running = false;
            if (clientPool != null) clientPool.shutdownNow();

            for (ClientHandler ch : clients.values()) {
                try {
                    ch.dos.writeUTF("SERVER_STOPPED");
                    ch.dos.flush();
                    ch.socket.close();
                } catch (IOException ignored) {}
            }
            clients.clear();

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

            logWarn("Server đã dừng.");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (IOException e) {
            logError("Lỗi khi dừng server: " + e.getMessage());
        }
    }

    private void updateClientList() {
        try {
            String list = String.join(",", clients.keySet());
            for (ClientHandler ch : clients.values()) {
                ch.dos.writeUTF("CLIENT_LIST");
                ch.dos.writeUTF(list);
                ch.dos.flush();
            }
        } catch (IOException e) {
            logError("Lỗi khi gửi danh sách client: " + e.getMessage());
        }
    }

    // ==== Client handler ====
    class ClientHandler implements Runnable {
        final Socket socket;
        DataInputStream dis;
        DataOutputStream dos;
        String clientId;
        boolean loggedOut = false;

        public ClientHandler(Socket socket) { this.socket = socket; }

        private boolean handleAuth(String action, String username, String password) throws IOException {
            if ("REGISTER".equals(action)) {
                String result = SQL.tryRegister(username, password);
                dos.writeUTF(result);
                dos.flush();
                if ("REGISTER_OK".equals(result)) logInfo("Đăng ký thành công: " + username);
                else logError("Đăng ký thất bại: " + username);
                return "REGISTER_OK".equals(result);

            } else if ("LOGIN".equals(action)) {
                boolean success = SQL.login(username, password);
                dos.writeUTF(success ? "AUTH_OK" : "AUTH_FAIL");
                dos.flush();
                if (!success) logError("Đăng nhập thất bại: " + username);
                return success;
            }
            return false;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                String action = dis.readUTF();
                String username = dis.readUTF();
                String password = dis.readUTF();

                if (!handleAuth(action, username, password)) {
                    socket.close();
                    return;
                }

                clientId = username;
                clients.put(clientId, this);
                updateClientList();
                logInfo("User " + clientId + " đã đăng nhập.");

                while (running && !socket.isClosed()) {
                    String cmd = dis.readUTF();
                    switch (cmd) {
                        case "SEND_REQUEST" -> handleSendRequest();
                        case "ACCEPT" -> handleAccept();
                        case "DECLINE" -> handleDecline();
                        case "SEND_FILE" -> handleSendFile();
                        case "GET_HISTORY" -> handleGetHistory();
                        case "LOGOUT" -> {
                            logWarn("User " + clientId + " đã đăng xuất.");
                            dos.writeUTF("AUTH_LOGOUT");
                            dos.flush();
                            loggedOut = true;
                            socket.close();
                            return;
                        }
                        default -> logWarn("Lệnh không rõ từ " + clientId + ": " + cmd);
                    }
                }

            } catch (IOException ignored) {}
            finally { safeDisconnect(); }
        }

        private void safeDisconnect() {
            try { socket.close(); } catch (IOException ignored) {}
            clients.remove(clientId);
            updateClientList();
            if (!loggedOut && clientId != null) logError("Client " + clientId + " đã rời.");
        }

        private void handleSendRequest() throws IOException {
            String targetId = dis.readUTF();
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            ClientHandler target = clients.get(targetId);
            if (target != null) {
                target.dos.writeUTF("REQUEST_RECEIVE");
                target.dos.writeUTF(clientId);
                target.dos.writeUTF(fileName);
                target.dos.writeLong(fileSize);
                target.dos.flush();
                logWarn(clientId + " muốn gửi '" + fileName + "' → " + targetId);
            } else logError("Client " + targetId + " không online.");
        }

        private void handleAccept() throws IOException {
            String senderId = dis.readUTF();
            ClientHandler sender = clients.get(senderId);
            if (sender != null) {
                sender.dos.writeUTF("ACCEPTED");
                sender.dos.writeUTF(clientId);
                sender.dos.flush();
                logInfo(clientId + " đồng ý nhận file từ " + senderId);
            }
        }

        private void handleDecline() throws IOException {
            String senderId = dis.readUTF();
            ClientHandler sender = clients.get(senderId);
            if (sender != null) {
                sender.dos.writeUTF("DECLINED");
                sender.dos.flush();
                logWarn(clientId + " từ chối file từ " + senderId);
            }
        }

        private void handleSendFile() throws IOException {
            String targetId = dis.readUTF();
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            ClientHandler target = clients.get(targetId);
            if (target != null) {
                target.dos.writeUTF("RECEIVE_FILE");
                target.dos.writeUTF(clientId);
                target.dos.writeUTF(fileName);
                target.dos.writeLong(fileSize);
                target.dos.flush();

                byte[] buffer = new byte[16 * 1024];
                long remaining = fileSize;
                while (remaining > 0) {
                    int read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    target.dos.write(buffer, 0, read);
                    remaining -= read;
                }
                target.dos.flush();
                logInfo(clientId + " đã gửi file '" + fileName + "' → " + targetId);

                SQL.saveFileHistory(clientId, targetId, fileName, fileSize, "SENT", null);
            } else logError("Client đích " + targetId + " không online, không thể gửi file.");
        }

        private void handleGetHistory() throws IOException {
            List<String[]> history = SQL.getHistory(clientId);
            StringBuilder sb = new StringBuilder();
            for (String[] row : history) sb.append(String.join("|", row)).append(";");
            dos.writeUTF("HISTORY_DATA");
            dos.writeUTF(sb.toString());
            dos.flush();
            logInfo("Đã gửi lịch sử cho " + clientId);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}
