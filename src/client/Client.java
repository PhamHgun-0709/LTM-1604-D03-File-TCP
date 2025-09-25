package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {
    private String clientId;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private JTextArea logArea;
    private JTextField txtTargetId;
    private JButton chooseFileButton, sendFileButton, logoutButton;
    private JLabel chosenFileLabel;
    private File selectedFile;

    // Constructor nhận từ LoginUI
    public Client(Socket socket, String clientId, DataInputStream dis, DataOutputStream dos) {
        this.socket = socket;
        this.clientId = clientId;
        this.dis = dis;
        this.dos = dos;

        setTitle("Client - " + clientId);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // ==== Khu vực log ====
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // ==== Panel control ====
        JPanel controlPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        JPanel targetPanel = new JPanel(new BorderLayout(5, 5));
        targetPanel.add(new JLabel("ID Người nhận:"), BorderLayout.WEST);
        txtTargetId = new JTextField();
        targetPanel.add(txtTargetId, BorderLayout.CENTER);
        controlPanel.add(targetPanel);

        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        chosenFileLabel = new JLabel("Chưa chọn file");
        chooseFileButton = new JButton("Chọn File...");
        chooseFileButton.addActionListener(e -> chooseFile());
        filePanel.add(chosenFileLabel, BorderLayout.CENTER);
        filePanel.add(chooseFileButton, BorderLayout.EAST);
        controlPanel.add(filePanel);

        sendFileButton = new JButton("Gửi File");
        sendFileButton.addActionListener(e -> sendFile());
        controlPanel.add(sendFileButton);

        // ==== Nút Đăng xuất ====
        logoutButton = new JButton("Đăng xuất");
        logoutButton.addActionListener(e -> logout());
        controlPanel.add(logoutButton);

        contentPane.add(controlPanel, BorderLayout.SOUTH);

        // ==== Thread nhận dữ liệu từ server ====
        new Thread(this::listenFromServer).start();
    }

    // chọn file
    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            chosenFileLabel.setText(selectedFile.getName());
        }
    }

    // gửi YÊU CẦU gửi file
    private void sendFile() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn file!");
            return;
        }
        String targetId = txtTargetId.getText().trim();
        if (targetId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập ID người nhận!");
            return;
        }
        try {
            dos.writeUTF("SEND_REQUEST");
            dos.writeUTF(targetId);
            dos.writeUTF(selectedFile.getName());
            dos.writeLong(selectedFile.length());
            dos.flush();

            log("Đã gửi yêu cầu gửi file: " + selectedFile.getName() + " → " + targetId);
        } catch (IOException e) {
            log("Lỗi gửi yêu cầu: " + e.getMessage());
        }
    }

    // gửi yêu cầu logout
    private void logout() {
        try {
            dos.writeUTF("LOGOUT");
            dos.flush();
        } catch (IOException e) {
            log("Lỗi khi gửi LOGOUT: " + e.getMessage());
            closeClient();
        }
    }

    // thread nhận dữ liệu từ server
    private void listenFromServer() {
        try {
            while (true) {
                String command = dis.readUTF();

                switch (command) {
                    case "REQUEST_RECEIVE": {
                        String senderId = dis.readUTF();
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        int choice = JOptionPane.showConfirmDialog(
                                this,
                                "Người dùng " + senderId + " muốn gửi file:\n"
                                        + fileName + " (" + fileSize + " bytes)\nBạn có đồng ý?",
                                "Xác nhận nhận file",
                                JOptionPane.YES_NO_OPTION
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            dos.writeUTF("ACCEPT");
                            dos.writeUTF(senderId);
                            dos.flush();
                            log("Đồng ý nhận file từ " + senderId);
                        } else {
                            dos.writeUTF("DECLINE");
                            dos.writeUTF(senderId);
                            dos.flush();
                            log("Từ chối nhận file từ " + senderId);
                        }
                        break;
                    }
                    case "ACCEPTED": {
                        String targetId = dis.readUTF();
                        log("Người dùng " + targetId + " đã đồng ý nhận file. Đang gửi...");

                        FileInputStream fis = new FileInputStream(selectedFile);
                        dos.writeUTF("SEND_FILE");
                        dos.writeUTF(targetId);
                        dos.writeUTF(selectedFile.getName());
                        dos.writeLong(selectedFile.length());

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                        dos.flush();

                        log("Đã gửi file: " + selectedFile.getName());
                        break;
                    }
                    case "DECLINED": {
                        String targetId = dis.readUTF();
                        log("Người dùng " + targetId + " đã từ chối nhận file.");
                        break;
                    }
                    case "RECEIVE_FILE": {
                        String senderId = dis.readUTF();
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        File outFile = new File("downloads", fileName);
                        outFile.getParentFile().mkdirs();

                        FileOutputStream fos = new FileOutputStream(outFile);
                        byte[] buffer = new byte[4096];
                        long remaining = fileSize;
                        while (remaining > 0) {
                            int bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                            if (bytesRead == -1) break;
                            fos.write(buffer, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                        fos.close();
                        log("Nhận file từ " + senderId + ": " + fileName);
                        break;
                    }
                    case "CLIENT_LIST": {
                        String list = dis.readUTF();
                        log("👥 Danh sách online: " + list);
                        break;
                    }
                    case "AUTH_LOGOUT": {
                        log("Bạn đã đăng xuất.");
                        closeClient();
                        SwingUtilities.invokeLater(() -> {
                            dispose();
                            new LoginUI().setVisible(true);
                        });
                        return;
                    }
                    case "AUTH_FAIL": {
                        log("❌ Xác thực thất bại.");
                        closeClient();
                        SwingUtilities.invokeLater(() -> {
                            dispose();
                            new LoginUI().setVisible(true);
                        });
                        return;
                    }
                }
            }
        } catch (IOException e) {
            log("Ngắt kết nối với server: " + e.getMessage());
            closeClient();
        }
    }

    private void closeClient() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}
