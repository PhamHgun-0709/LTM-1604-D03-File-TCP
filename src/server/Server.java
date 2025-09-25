package server;

import sql.SQL;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server extends JFrame {
    private JTextArea logArea;
    private JButton startButton, stopButton;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Map<String, ClientHandler> clients = new HashMap<>();

    public Server() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf()); // Giao diện sáng
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo FlatLaf");
        }

        setTitle("File Transfer Server");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ===== Main Panel =====
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // ===== Top Buttons =====
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.setBackground(Color.WHITE);

        startButton = createButton("Bắt đầu Server", new Color(46, 204, 113));
        stopButton = createButton("Dừng Server", new Color(231, 76, 60));
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        topPanel.add(startButton);
        topPanel.add(stopButton);

        // ===== Log Area =====
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 30));  // nền đen than
        logArea.setForeground(new Color(0, 255, 128)); // chữ xanh lá sáng
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void startServer() {
        try {
            SQL.connect();
            SQL.createUsersTableIfNotExists();

            serverSocket = new ServerSocket(12345);
            running = true;
            log("Server đang chạy tại cổng 12345");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        new ClientHandler(socket).start();
                    } catch (IOException e) {
                        if (running) log("Lỗi khi chấp nhận client: " + e.getMessage());
                    }
                }
            }).start();

        } catch (IOException e) {
            log("Không thể khởi động server: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            running = false;
            if (serverSocket != null) serverSocket.close();

            SQL.close();

            log("Server đã dừng.");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (IOException e) {
            log("Lỗi khi dừng server: " + e.getMessage());
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    private void updateClientList() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String id : clients.keySet()) {
                sb.append(id).append(",");
            }
            String list = sb.toString();

            for (ClientHandler ch : clients.values()) {
                ch.dos.writeUTF("CLIENT_LIST");
                ch.dos.writeUTF(list);
                ch.dos.flush();
            }
        } catch (IOException e) {
            log("Lỗi khi gửi danh sách client: " + e.getMessage());
        }
    }

    class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                String action = dis.readUTF();
                String username = dis.readUTF();
                String password = dis.readUTF();

                boolean success = false;
                if (action.equals("REGISTER")) {
                    success = SQL.register(username, password);
                    log("REGISTER từ user: " + username + " → " + (success ? "OK" : "FAIL"));
                } else if (action.equals("LOGIN")) {
                    success = SQL.login(username, password);
                    log("LOGIN từ user: " + username + " → " + (success ? "OK" : "FAIL"));
                }

                if (!success) {
                    dos.writeUTF("AUTH_FAIL");
                    dos.flush();
                    socket.close();
                    return;
                } else {
                    dos.writeUTF("AUTH_OK");
                    dos.flush();
                    clientId = username;
                }

                synchronized (clients) {
                    clients.put(clientId, this);
                    updateClientList();
                }
                log("User " + clientId + " đã đăng nhập và kết nối.");

                while (running && !socket.isClosed()) {
                    String cmd = dis.readUTF();

                    switch (cmd) {
                        case "SEND_REQUEST": {
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

                                log(clientId + " muốn gửi file '" + fileName + "' cho " + targetId);
                            } else {
                                dos.writeUTF("ERROR");
                                dos.writeUTF("Client " + targetId + " không online.");
                                dos.flush();
                            }
                            break;
                        }
                        case "ACCEPT": {
                            String fromId = dis.readUTF();
                            ClientHandler sender = clients.get(fromId);
                            if (sender != null) {
                                sender.dos.writeUTF("ACCEPTED");
                                sender.dos.writeUTF(clientId);
                                sender.dos.flush();
                                log(clientId + " chấp nhận nhận file từ " + fromId);
                            }
                            break;
                        }
                        case "DECLINE": {
                            String fromId = dis.readUTF();
                            ClientHandler sender = clients.get(fromId);
                            if (sender != null) {
                                sender.dos.writeUTF("DECLINED");
                                sender.dos.writeUTF(clientId);
                                sender.dos.flush();
                                log(clientId + " từ chối nhận file từ " + fromId);
                            }
                            break;
                        }
                        case "SEND_FILE": {
                            String targetId = dis.readUTF();
                            String fileName = dis.readUTF();
                            long fileSize = dis.readLong();

                            ClientHandler target = clients.get(targetId);
                            if (target != null) {
                                target.dos.writeUTF("RECEIVE_FILE");
                                target.dos.writeUTF(clientId);
                                target.dos.writeUTF(fileName);
                                target.dos.writeLong(fileSize);

                                byte[] buffer = new byte[4096];
                                long remaining = fileSize;
                                while (remaining > 0) {
                                    int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                                    if (read == -1) break;
                                    target.dos.write(buffer, 0, read);
                                    remaining -= read;
                                }
                                target.dos.flush();

                                log("File '" + fileName + "' từ " + clientId + " → " + targetId + " đã gửi xong.");
                            } else {
                                dos.writeUTF("ERROR");
                                dos.writeUTF("Client " + targetId + " không online.");
                                dos.flush();
                            }
                            break;
                        }
                        case "LOGOUT": {
                            log("User " + clientId + " đã yêu cầu đăng xuất.");
                            dos.writeUTF("AUTH_LOGOUT");
                            dos.flush();
                            return;
                        }
                    }
                }

            } catch (IOException e) {
                log("Client " + clientId + " ngắt kết nối.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                synchronized (clients) {
                    if (clientId != null) {
                        clients.remove(clientId);
                        updateClientList();
                        log("Client " + clientId + " đã rời.");
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}
