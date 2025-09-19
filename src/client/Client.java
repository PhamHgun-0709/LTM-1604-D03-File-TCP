package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {
    private JTextField txtId, txtServerIp, txtTargetId;
    private JTextArea logArea;
    private JButton connectButton, disconnectButton, chooseFileButton, sendFileButton;
    private JLabel chosenFileLabel;
    private JFileChooser fileChooser;
    private JList<String> onlineList;
    private DefaultListModel<String> onlineListModel;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String clientId;
    private File selectedFile;
    private boolean connected = false;

    public Client() {
        setTitle("📤 Client Truyền File");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Nimbus Look & Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        connectPanel.setBorder(BorderFactory.createTitledBorder("🔗 Cài đặt kết nối"));

        connectPanel.add(new JLabel("ID của bạn:"));
        txtId = new JTextField(8);
        connectPanel.add(txtId);

        connectPanel.add(new JLabel("IP Server:"));
        txtServerIp = new JTextField("localhost", 10);
        connectPanel.add(txtServerIp);

        connectButton = new JButton("Kết nối");
        disconnectButton = new JButton("Ngắt kết nối");
        disconnectButton.setEnabled(false);

        connectButton.addActionListener(e -> connectServer());
        disconnectButton.addActionListener(e -> disconnectServer());

        connectPanel.add(connectButton);
        connectPanel.add(disconnectButton);

        // Tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // Send file tab
        JPanel sendPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        sendPanel.add(new JLabel("ID Người nhận:"), gbc);

        txtTargetId = new JTextField(10);
        gbc.gridx = 1; gbc.gridy = 0;
        sendPanel.add(txtTargetId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        sendPanel.add(new JLabel("Chọn File:"), gbc);

        chooseFileButton = new JButton("Chọn File...");
        chosenFileLabel = new JLabel("Chưa có file nào được chọn.");
        gbc.gridx = 1; gbc.gridy = 1;
        sendPanel.add(chooseFileButton, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        sendPanel.add(chosenFileLabel, gbc);

        sendFileButton = new JButton("📤 Gửi File");
        sendFileButton.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 3;
        sendPanel.add(sendFileButton, gbc);

        chooseFileButton.addActionListener(e -> chooseFile());
        sendFileButton.addActionListener(e -> requestSendFile());

        tabbedPane.addTab("📦 Gửi File", sendPanel);

        // Log tab
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(250, 250, 250));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        tabbedPane.addTab("📜 Lịch sử & Nhận", logScroll);

        // Online list tab
        onlineListModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineListModel);
        JScrollPane onlineScroll = new JScrollPane(onlineList);
        tabbedPane.addTab("👥 Danh sách Online", onlineScroll);

        mainPanel.add(connectPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void connectServer() {
        try {
            String ip = txtServerIp.getText().trim();
            clientId = txtId.getText().trim();
            socket = new Socket(ip, 12345);

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF(clientId);
            dos.flush();

            log("✅ Kết nối thành công tới server với ID: " + clientId);

            connected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            sendFileButton.setEnabled(true);

            new Thread(this::listenServer).start();

        } catch (IOException e) {
            log("❌ Không thể kết nối: " + e.getMessage());
        }
    }

    private void disconnectServer() {
        try {
            connected = false;
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();

            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            sendFileButton.setEnabled(false);

            log("⛔ Đã ngắt kết nối.");
        } catch (IOException e) {
            log("❌ Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    private void chooseFile() {
        if (fileChooser == null) fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            chosenFileLabel.setText("Đã chọn: " + selectedFile.getName());
        }
    }

    private void requestSendFile() {
        if (!connected || selectedFile == null) {
            log("⚠ Chưa chọn file hoặc chưa kết nối server.");
            return;
        }
        String targetId = txtTargetId.getText().trim();
        if (targetId.isEmpty()) {
            log("⚠ Vui lòng nhập ID người nhận.");
            return;
        }
        try {
            dos.writeUTF("SEND_REQUEST");
            dos.writeUTF(targetId);
            dos.writeUTF(selectedFile.getName());
            dos.writeLong(selectedFile.length());
            dos.flush();
            log("📤 Đang chờ " + targetId + " xác nhận nhận file...");
        } catch (IOException e) {
            log("❌ Lỗi khi gửi yêu cầu: " + e.getMessage());
        }
    }

    private void sendFile(String targetId, String fileName, long fileSize) {
        try {
            dos.writeUTF("SEND_FILE");
            dos.writeUTF(targetId);
            dos.writeUTF(fileName);
            dos.writeLong(fileSize);

            FileInputStream fis = new FileInputStream(selectedFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();
            fis.close();

            log("✅ File '" + fileName + "' đã gửi xong.");

        } catch (IOException e) {
            log("❌ Lỗi khi gửi file: " + e.getMessage());
        }
    }

    private void listenServer() {
        try {
            while (connected) {
                String cmd = dis.readUTF();

                if (cmd.equals("REQUEST_RECEIVE")) {
                    String fromId = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "Client " + fromId + " muốn gửi file '" + fileName + "' (" + fileSize + " bytes).\nBạn có đồng ý không?",
                            "Xác nhận nhận file",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (choice == JOptionPane.YES_OPTION) {
                        dos.writeUTF("ACCEPT");
                        dos.writeUTF(fromId);
                        log("📥 Đã chấp nhận nhận file từ " + fromId);
                    } else {
                        dos.writeUTF("DECLINE");
                        dos.writeUTF(fromId);
                        log("❌ Từ chối nhận file từ " + fromId);
                    }
                }
                else if (cmd.equals("ACCEPTED")) {
                    String targetId = txtTargetId.getText().trim();
                    sendFile(targetId, selectedFile.getName(), selectedFile.length());
                }
                else if (cmd.equals("DECLINED")) {
                    log("❌ Người nhận đã từ chối file.");
                }
                else if (cmd.equals("START_FILE")) {
                    String fromId = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    saveFile(fileName, fileSize, fromId);
                }
                else if (cmd.equals("CLIENT_LIST")) {
                    String list = dis.readUTF();
                    SwingUtilities.invokeLater(() -> {
                        onlineListModel.clear();
                        for (String id : list.split(",")) {
                            if (!id.isEmpty()) onlineListModel.addElement(id);
                        }
                    });
                }
                else if (cmd.equals("ERROR")) {
                    log("⚠ Server báo lỗi: " + dis.readUTF());
                }
            }
        } catch (IOException e) {
            if (connected) log("⚠ Mất kết nối: " + e.getMessage());
        }
    }

    private void saveFile(String fileName, long fileSize, String fromId) throws IOException {
        File saveDir = new File("ReceivedFiles");
        if (!saveDir.exists()) saveDir.mkdirs();

        File file = new File(saveDir, fileName);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        long remaining = fileSize;
        while (remaining > 0) {
            int read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining));
            if (read == -1) break;
            fos.write(buffer, 0, read);
            remaining -= read;
        }
        fos.close();

        log("✅ Đã nhận file '" + fileName + "' từ " + fromId);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
