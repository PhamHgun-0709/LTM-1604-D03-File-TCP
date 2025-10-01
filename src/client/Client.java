package client;

import sql.SQL;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;

public class Client extends JFrame {
    private final String clientId;
    private final String password;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private JTextPane logPane;
    private StyledDocument logDoc;
    private JComboBox<String> cbTargetId;
    private JButton chooseFileButton, sendFileButton, logoutButton, reconnectButton, historyButton, chooseFolderButton;
    private JLabel chosenFileLabel, saveDirLabel;
    private File selectedFile;
    private File downloadDir = new File("downloads");

    private volatile boolean listening = true;

    public Client(Socket socket, String clientId, String password,
                  DataInputStream dis, DataOutputStream dos) {
        this.socket = socket;
        this.clientId = clientId;
        this.password = password;
        this.dis = dis;
        this.dos = dos;

        setTitle("Client - " + clientId);
        setSize(800, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        startListening();
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(contentPane);

        // Log Pane
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        logPane.setBackground(new Color(25, 27, 35));
        logPane.setForeground(Color.WHITE);
        logDoc = logPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Client Log"));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(7, 1, 8, 8));

        // Target dropdown
        JPanel targetPanel = new JPanel(new BorderLayout(5, 5));
        targetPanel.add(new JLabel("Người nhận:"), BorderLayout.WEST);
        cbTargetId = new JComboBox<>();
        cbTargetId.setEditable(false);
        cbTargetId.addItem("— Chưa có client online —");
        targetPanel.add(cbTargetId, BorderLayout.CENTER);
        controlPanel.add(targetPanel);

        // File chooser
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        chosenFileLabel = new JLabel("Chưa chọn file");
        chooseFileButton = createButton("Chọn File...", new Color(52, 152, 219));
        chooseFileButton.addActionListener(e -> chooseFile());
        filePanel.add(chosenFileLabel, BorderLayout.CENTER);
        filePanel.add(chooseFileButton, BorderLayout.EAST);
        controlPanel.add(filePanel);

        // Folder chooser
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        saveDirLabel = new JLabel("Thư mục lưu: " + downloadDir.getAbsolutePath());
        chooseFolderButton = createButton("Chọn Thư Mục...", new Color(46, 204, 113));
        chooseFolderButton.addActionListener(e -> chooseFolder());
        folderPanel.add(saveDirLabel, BorderLayout.CENTER);
        folderPanel.add(chooseFolderButton, BorderLayout.EAST);
        controlPanel.add(folderPanel);

        // Send File
        sendFileButton = createButton("Gửi File", new Color(72, 201, 176));
        sendFileButton.addActionListener(e -> sendFile());
        controlPanel.add(sendFileButton);

        // History button
        historyButton = createButton("Xem lịch sử", new Color(241, 196, 15));
        historyButton.addActionListener(e -> showHistory());
        controlPanel.add(historyButton);

        // Logout
        logoutButton = createButton("Đăng xuất", new Color(231, 76, 60));
        logoutButton.addActionListener(e -> logout());
        controlPanel.add(logoutButton);

        // Reconnect
        reconnectButton = createButton("Kết nối lại", new Color(155, 89, 182));
        reconnectButton.addActionListener(e -> reconnect());
        reconnectButton.setEnabled(false);
        controlPanel.add(reconnectButton);

        contentPane.add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            String name = selectedFile.getName();
            chosenFileLabel.setText(name.length() > 25 ? name.substring(0, 25) + "..." : name);
        }
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            downloadDir = fc.getSelectedFile();
            saveDirLabel.setText("Thư mục lưu: " + downloadDir.getAbsolutePath());
        }
    }

    private String getFileChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void sendFile() {
        if (selectedFile == null) {
            showLog("Vui lòng chọn file!", Color.ORANGE);
            return;
        }
        String targetId = (String) cbTargetId.getSelectedItem();
        if (targetId == null || targetId.startsWith("—")) {
            showLog("Vui lòng chọn người nhận!", Color.ORANGE);
            return;
        }

        sendFileButton.setEnabled(false);
        new Thread(() -> {
            try {
                dos.writeUTF("SEND_REQUEST");
                dos.writeUTF(targetId);
                dos.writeUTF(selectedFile.getName());
                dos.writeLong(selectedFile.length());
                dos.flush();
                showLog("Đã gửi yêu cầu gửi file: " + selectedFile.getName() + " → " + targetId, Color.CYAN);
            } catch (IOException e) {
                showLog("Lỗi gửi yêu cầu: " + e.getMessage(), Color.RED);
                sendFileButton.setEnabled(true);
            }
        }).start();
    }

    private void handleAccepted() throws IOException {
        String target = dis.readUTF();
        showLog(target + " đã đồng ý. Đang gửi file...", Color.CYAN);

        new Thread(() -> {
            try {
                String checksum = getFileChecksum(selectedFile);
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    dos.writeUTF("SEND_FILE");
                    dos.writeUTF(target);
                    dos.writeUTF(selectedFile.getName());
                    dos.writeLong(selectedFile.length());
                    dos.flush();

                    byte[] buffer = new byte[16 * 1024];
                    int bytesRead;
                    long totalSent = 0;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                        totalSent += bytesRead;
                        int percent = (int) ((totalSent * 100) / selectedFile.length());
                        showLog("[Đang gửi] " + selectedFile.getName() + " " + percent + "%", Color.CYAN);
                    }
                    dos.flush();
                }
                showLog("[Hoàn tất] " + selectedFile.getName(), Color.GREEN);

                SQL.saveFileHistory(clientId, target, selectedFile.getName(), selectedFile.length(), "SENT",
                        selectedFile.getAbsolutePath());
            } catch (Exception e) {
                showLog("Lỗi gửi file: " + e.getMessage(), Color.RED);
            } finally {
                sendFileButton.setEnabled(true);
            }
        }).start();
    }

    private void handleFileReceive() throws IOException {
        String sender = dis.readUTF();
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        File outFile = new File(downloadDir, fileName);
        outFile.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[16 * 1024];
            long remaining = fileSize;
            long received = 0;
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                fos.write(buffer, 0, read);
                remaining -= read;
                received += read;
                int percent = (int) ((received * 100) / fileSize);
                showLog("[Đang nhận] " + fileName + " " + percent + "%", Color.ORANGE);
            }
        }

        showLog("[Hoàn tất] Nhận file từ " + sender + ": " + fileName, Color.GREEN);
        showPopup("Đã nhận file: " + fileName);

        SQL.saveFileHistory(sender, clientId, fileName, fileSize, "RECEIVED", outFile.getAbsolutePath());
    }

    private void handleFileRequest() throws IOException {
        String sender = dis.readUTF();
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        int choice = JOptionPane.showConfirmDialog(this,
                "Người dùng " + sender + " muốn gửi file:\n" +
                        fileName + " (" + fileSize + " bytes)\nBạn có muốn nhận không?",
                "Yêu cầu gửi file",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            dos.writeUTF("ACCEPT");
            dos.writeUTF(sender);
            dos.flush();
            showLog("Đã chấp nhận nhận file từ " + sender, Color.CYAN);
        } else {
            dos.writeUTF("DECLINE");
            dos.writeUTF(sender);
            dos.flush();
            showLog("Đã từ chối file từ " + sender, Color.ORANGE);
        }
    }

    private void showHistory() {
        List<String[]> history = SQL.getHistory(clientId);
        String[] cols = {"Thời gian", "Người gửi", "Người nhận", "Tên file", "Kích thước", "Trạng thái", "Đường dẫn"};
        String[][] data = history.toArray(new String[0][]);

        // Tạo JTable và tắt edit toàn bộ cell
        JTable table = new JTable(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // không cho phép edit
            }
        };
        table.setFillsViewportHeight(true);

        // Listener double click mở thư mục chứa file hoặc thông báo file mất
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        String path = (String) table.getValueAt(row, 6); // cột đường dẫn
                        if (path != null && !path.isBlank()) {
                            java.io.File file = new java.io.File(path);
                            if (file.exists()) {
                                try {
                                    Desktop.getDesktop().open(file.getParentFile());
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(null, "Không mở được thư mục chứa file: " + file.getName());
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "File đã bị xóa hoặc không tồn tại: " + file.getName());
                            }
                        }
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(700, 300));
        JOptionPane.showMessageDialog(this, sp, "Lịch sử gửi/nhận file", JOptionPane.PLAIN_MESSAGE);
    }


    private void showPopup(String msg) {
        JOptionPane optionPane = new JOptionPane(msg, JOptionPane.INFORMATION_MESSAGE);
        final JDialog dialog = optionPane.createDialog(this, "Thông báo");
        new Timer(3000, e -> dialog.dispose()).start();
        dialog.setVisible(true);
    }

    private void startListening() {
        listening = true;
        new Thread(() -> {
            try {
                while (listening && !socket.isClosed()) {
                    String cmd;
                    try {
                        cmd = dis.readUTF();
                    } catch (EOFException eof) {
                        break;
                    }
                    switch (cmd) {
                        case "ACCEPTED" -> handleAccepted();
                        case "RECEIVE_FILE" -> handleFileReceive();
                        case "CLIENT_LIST" -> updateClientList();
                        case "REQUEST_RECEIVE" -> handleFileRequest();
                        case "DECLINED" -> showLog("Người nhận từ chối nhận file.", Color.ORANGE);
                        case "SERVER_STOPPED" -> {
                            showLog("Server đã dừng!", Color.RED);
                            sendFileButton.setEnabled(false);
                            logoutButton.setEnabled(false);
                            reconnectButton.setEnabled(true);
                        }
                    }
                }
            } catch (IOException e) {
                if (listening) {
                    showLog("Mất kết nối với server.", Color.RED);
                    sendFileButton.setEnabled(false);
                    logoutButton.setEnabled(false);
                    reconnectButton.setEnabled(true);
                }
            }
        }, "Client-Listener-" + clientId).start();
    }

    private void updateClientList() throws IOException {
        String list = dis.readUTF();
        SwingUtilities.invokeLater(() -> {
            String[] ids = list.split(",");
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String id : ids) if (!id.isBlank() && !id.equals(clientId)) model.addElement(id);
            if (model.getSize() == 0) model.addElement("— Không có client online —");
            cbTargetId.setModel(model);
        });
    }

    private void reconnect() {
        showLog("Đang thử kết nối lại server...", Color.ORANGE);
        try {
            listening = false;
            if (socket != null && !socket.isClosed()) socket.close();

            socket = new Socket("localhost", 12345);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF("LOGIN");
            dos.writeUTF(clientId);
            dos.writeUTF(password);
            dos.flush();

            String response = dis.readUTF();
            if ("AUTH_OK".equals(response)) {
                showLog("Kết nối lại thành công!", Color.GREEN);
                reconnectButton.setEnabled(false);
                sendFileButton.setEnabled(true);
                logoutButton.setEnabled(true);
                startListening();
            } else {
                showLog("Sai tài khoản hoặc mật khẩu khi kết nối lại.", Color.RED);
                socket.close();
            }
        } catch (IOException e) {
            showLog("Không kết nối lại được: " + e.getMessage(), Color.RED);
        }
    }

    private void logout() {
        try {
            dos.writeUTF("LOGOUT");
            dos.flush();
            showLog("Đã gửi yêu cầu đăng xuất.", Color.CYAN);
        } catch (IOException ignored) {}
        closeClient();
        SwingUtilities.invokeLater(() -> {
            dispose();
            new LoginUI().setVisible(true);
        });
    }

    private void closeClient() {
        listening = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (dis != null) dis.close(); } catch (IOException ignored) {}
        try { if (dos != null) dos.close(); } catch (IOException ignored) {}
    }

    private void showLog(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                Style style = logPane.addStyle("Style", null);
                StyleConstants.setForeground(style, color);
                logDoc.insertString(logDoc.getLength(), msg + "\n", style);
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException e) { e.printStackTrace(); }
        });
    }
}
