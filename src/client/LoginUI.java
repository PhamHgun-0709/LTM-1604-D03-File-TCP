package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class LoginUI extends JFrame {
    private JTextField txtUsername, txtHost, txtPort;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegister;

    public LoginUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 20);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextComponent.arc", 15);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        } catch (Exception ignored) {}

        setTitle("Đăng nhập Client");
        setSize(460, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // Input Panel dùng GridBagLayout
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // --- Hàng 1: Server IP + Port ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        inputPanel.add(new JLabel("Server IP:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.6;
        txtHost = new JTextField("127.0.0.1");
        inputPanel.add(txtHost, gbc);

        gbc.gridx = 2; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.2;
        txtPort = new JTextField("12345");
        inputPanel.add(txtPort, gbc);

        // --- Hàng 2: Username ---
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Tên đăng nhập:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.7;
        txtUsername = new JTextField();
        inputPanel.add(txtUsername, gbc);
        gbc.gridwidth = 1;

        // --- Hàng 3: Password ---
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Mật khẩu:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.7;
        txtPassword = new JPasswordField();
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) btnLogin.doClick();
            }
        });
        inputPanel.add(txtPassword, gbc);
        gbc.gridwidth = 1;

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonPanel.setBackground(Color.WHITE);

        btnLogin = createButton("Đăng nhập", new Color(39, 174, 96));
        btnRegister = createButton("Đăng ký", new Color(52, 152, 219));

        btnLogin.addActionListener(e -> handleAuth("LOGIN"));
        btnRegister.addActionListener(e -> handleAuth("REGISTER"));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void handleAuth(String action) {
        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if(host.isEmpty() || portStr.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            Socket socket = new Socket(host, port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF(action);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.flush();

            String response = dis.readUTF();

            if ("LOGIN".equals(action)) {
                if ("AUTH_OK".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
                    this.dispose();
                    SwingUtilities.invokeLater(() ->
                            // Truyền thêm host + port sang Client
                            new Client(socket, username, password, dis, dos, host, port).setVisible(true));
                } else {
                    JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu.");
                    txtPassword.setText("");
                    safeClose(socket, dis, dos);
                }
            } else if ("REGISTER".equals(action)) {
                if ("REGISTER_OK".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Đăng ký thành công!");
                    this.dispose();
                    SwingUtilities.invokeLater(() ->
                            new Client(socket, username, password, dis, dos, host, port).setVisible(true));
                } else {
                    JOptionPane.showMessageDialog(this, "Đăng ký thất bại. Tên đã tồn tại hoặc mật khẩu không hợp lệ.");
                    txtPassword.setText("");
                    safeClose(socket, dis, dos);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,"Không kết nối được tới server: " + e.getMessage());
        }
    }

    private void safeClose(Socket socket, DataInputStream dis, DataOutputStream dos) {
        try { if (dos != null) dos.close(); } catch (Exception ignored) {}
        try { if (dis != null) dis.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}
