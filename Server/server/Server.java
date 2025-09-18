package server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server extends JFrame {
    private JButton startBtn, stopBtn, refreshBtn, deleteBtn;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean running = false;

    private static final int SERVER_PORT = 1234;
    private static final String SERVER_FOLDER = "server_files";

    public Server() {
        setTitle("File Server");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        File folder = new File(SERVER_FOLDER);
        if (!folder.exists()) folder.mkdir();

        // table
        tableModel = new DefaultTableModel(new String[]{"Tên file", "Dung lượng", "Ngày upload"}, 0);
        fileTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(fileTable);

        // log
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        // buttons
        startBtn = new JButton("Bật Server");
        stopBtn = new JButton("Tắt Server");
        refreshBtn = new JButton("Refresh");
        deleteBtn = new JButton("Xóa file");

        styleButton(startBtn, new Color(46, 204, 113));
        styleButton(stopBtn, new Color(231, 76, 60));
        styleButton(refreshBtn, new Color(52, 152, 219));
        styleButton(deleteBtn, new Color(241, 196, 15));

        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        refreshBtn.addActionListener(e -> loadFileList());
        deleteBtn.addActionListener(e -> deleteFile());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(deleteBtn);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, logScroll);
        splitPane.setDividerLocation(300);

        add(buttonPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        setVisible(true);
        loadFileList();
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(120, 40));
    }

    private void startServer() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(SERVER_PORT));

            running = true;
            serverThread = new Thread(this::acceptClients);
            serverThread.start();

            log("✅ Server đang chạy trên cổng " + SERVER_PORT);
            log("📡 Địa chỉ LAN: " + InetAddress.getLocalHost().getHostAddress());
        } catch (IOException e) {
            log("❌ Lỗi server: " + e.getMessage());
        }
    }

    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            log("🛑 Server đã tắt.");
        } catch (IOException e) {
            log("❌ Lỗi dừng server: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            } catch (IOException e) {
                if (running) log("❌ Lỗi accept: " + e.getMessage());
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String clientName = "Unknown";

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                log("❌ Lỗi tạo stream: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String cmd = dis.readUTF();
                    if ("HELLO".equals(cmd)) {
                        clientName = dis.readUTF();
                        log("🔗 Client đã kết nối: " + clientName);
                    } else if ("DISCONNECT".equals(cmd)) {
                        log("❌ Client ngắt kết nối: " + clientName);
                        break;
                    } else if ("LIST".equals(cmd)) {
                        sendFileList();
                    } else if ("UPLOAD".equals(cmd)) {
                        receiveFile();
                    } else if ("DOWNLOAD".equals(cmd)) {
                        sendFile();
                    }
                }
            } catch (IOException e) {
                log("⚠ Mất kết nối với " + clientName);
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void sendFileList() throws IOException {
            File folder = new File(SERVER_FOLDER);
            File[] files = folder.listFiles();
            if (files == null) files = new File[0];

            dos.writeInt(files.length);
            for (File f : files) {
                dos.writeUTF(f.getName());
                dos.writeLong(f.length());
                dos.writeUTF(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(f.lastModified())));
            }
        }

        private void receiveFile() throws IOException {
            String name = dis.readUTF();
            long size = dis.readLong();

            File file = new File(SERVER_FOLDER, name);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                long received = 0;
                int read;
                while (received < size && (read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    received += read;
                }
            }
            dos.writeUTF("UPLOAD_OK");
            log("⬆ Upload thành công từ " + clientName + ": " + name);
            loadFileList();
        }

        private void sendFile() throws IOException {
            String name = dis.readUTF();
            File file = new File(SERVER_FOLDER, name);

            if (file.exists()) {
                dos.writeUTF("FOUND");
                dos.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                }
                log("⬇ " + clientName + " tải xuống: " + name);
            } else {
                dos.writeUTF("NOT_FOUND");
            }
        }
    }

    private void loadFileList() {
        File folder = new File(SERVER_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) files = new File[0];

        tableModel.setRowCount(0);
        for (File f : files) {
            tableModel.addRow(new Object[]{
                f.getName(),
                formatSize(f.length()),
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(f.lastModified()))
            });
        }
        log("📂 Danh sách file được cập nhật.");
    }

    private void deleteFile() {
        int row = fileTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn file để xóa!");
            return;
        }
        String name = (String) tableModel.getValueAt(row, 0);
        File file = new File(SERVER_FOLDER, name);
        if (file.delete()) {
            log("🗑 Xóa file: " + name);
            loadFileList();
        } else {
            log("❌ Không thể xóa file: " + name);
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append("[" +
                new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n"));
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}
