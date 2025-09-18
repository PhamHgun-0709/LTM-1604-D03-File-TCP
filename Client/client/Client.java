package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private JButton uploadBtn, downloadBtn, refreshBtn, reconnectBtn;
    private JProgressBar progressBar;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int SERVER_PORT = 1234;
    private static final String CLIENT_FOLDER = "client_files"; // chỉ một thư mục cho client

    public Client() {
        setTitle("File Client - Upload/Download");
        setSize(780, 560);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        File folder = new File(CLIENT_FOLDER);
        if (!folder.exists()) folder.mkdir();

        // Bảng file
        tableModel = new DefaultTableModel(new String[]{"Tên file", "Dung lượng", "Ngày upload"}, 0);
        fileTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(fileTable);

        // Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        // Nút
        uploadBtn = new JButton("Upload");
        downloadBtn = new JButton("Download");
        refreshBtn = new JButton("Refresh");
        reconnectBtn = new JButton("Reconnect");

        styleButton(uploadBtn, new Color(46, 204, 113));
        styleButton(downloadBtn, new Color(52, 152, 219));
        styleButton(refreshBtn, new Color(241, 196, 15));
        styleButton(reconnectBtn, new Color(155, 89, 182));

        uploadBtn.addActionListener(e -> uploadFile());
        downloadBtn.addActionListener(e -> downloadFile());
        refreshBtn.addActionListener(e -> loadFileList());
        reconnectBtn.addActionListener(e -> reconnectServer());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(uploadBtn);
        buttonPanel.add(downloadBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(reconnectBtn);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, logScroll);
        splitPane.setDividerLocation(250);

        add(progressBar, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);

        setVisible(true);

        // thử kết nối ngay khi mở client
        connectServer();
        if (socket != null && dos != null) {
            loadFileList();
        }
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(130, 40));
    }

    private void connectServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("HELLO");
            dos.writeUTF("FileClient"); // chỉ còn 1 client name
            log("🔗 Đã kết nối server thành công!");

        } catch (IOException e) {
            log("⚠ Không thể kết nối server: " + e.getMessage());
            socket = null;
            dis = null;
            dos = null;
        }
    }

    private void loadFileList() {
        if (dos == null) {
            log("⚠ Không có kết nối tới server!");
            return;
        }
        try {
            dos.writeUTF("LIST");
            int count = dis.readInt();
            tableModel.setRowCount(0);
            for (int i = 0; i < count; i++) {
                String name = dis.readUTF();
                long size = dis.readLong();
                String date = dis.readUTF();
                tableModel.addRow(new Object[]{name, formatSize(size), date});
            }
            log("📂 Danh sách file cập nhật từ server");
        } catch (IOException e) {
            log("❌ Lỗi tải danh sách: " + e.getMessage());
        }
    }

    private void reconnectServer() {
        log("🔌 Đang thử kết nối lại server...");
        closeConnection();
        connectServer();
        if (socket != null && dos != null) {
            log("✅ Client đã kết nối lại server!");
            loadFileList();
        }
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (
                Socket upSocket = new Socket(SERVER_IP, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(upSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(upSocket.getInputStream());
                FileInputStream fis = new FileInputStream(file)
            ) {
                dos.writeUTF("UPLOAD");
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int read;
                long sent = 0;
                progressBar.setValue(0);

                while ((read = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, read);
                    sent += read;
                    int percent = (int) (sent * 100 / file.length());
                    progressBar.setValue(percent);
                    progressBar.setString(formatSize(sent) + " / " + formatSize(file.length()));
                }

                String response = dis.readUTF();
                if ("UPLOAD_OK".equals(response)) {
                    log("✅ Upload thành công: " + file.getName());
                    loadFileList();
                }
            } catch (IOException e) {
                log("❌ Lỗi upload: " + e.getMessage());
            }
        }
    }

    private void downloadFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file để download!");
            return;
        }
        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        File file = new File(CLIENT_FOLDER + "/" + fileName);

        try (
            Socket downSocket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream dos = new DataOutputStream(downSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(downSocket.getInputStream());
            FileOutputStream fos = new FileOutputStream(file)
        ) {
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);

            String response = dis.readUTF();
            if ("FOUND".equals(response)) {
                long fileSize = dis.readLong();
                byte[] buffer = new byte[4096];
                int read;
                long received = 0;
                progressBar.setValue(0);

                while (received < fileSize && (read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    received += read;
                    int percent = (int) (received * 100 / fileSize);
                    progressBar.setValue(percent);
                    progressBar.setString(formatSize(received) + " / " + formatSize(fileSize));
                }

                log("✅ Download thành công: " + fileName);
            } else {
                log("⚠ File không tồn tại: " + fileName);
            }
        } catch (IOException e) {
            log("❌ Lỗi download: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        dis = null;
        dos = null;
        socket = null;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
