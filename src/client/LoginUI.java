package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class LoginUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegister;

    public LoginUI() {
        setTitle("Đăng nhập Client");
        setSize(420, 230);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // ==== Panel nhập liệu ====
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 15));
        inputPanel.setBackground(Color.WHITE);

        JLabel lblUser = new JLabel("Tên đăng nhập:");
        txtUsername = new JTextField();

        JLabel lblPass = new JLabel("Mật khẩu:");
        txtPassword = new JPasswordField();

        inputPanel.add(lblUser);
        inputPanel.add(txtUsername);
        inputPanel.add(lblPass);
        inputPanel.add(txtPassword);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // ==== Panel nút ====
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonPanel.setBackground(Color.WHITE);

        btnLogin = createButton("Đăng nhập", new Color(46, 204, 113));
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
        return button;
    }

    private void handleAuth(String action) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin!");
            return;
        }

        try {
            // Kết nối tới server
            Socket socket = new Socket("localhost", 12345);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            // Gửi yêu cầu LOGIN / REGISTER
            dos.writeUTF(action);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.flush();

            // Nhận phản hồi từ server
            String response = dis.readUTF();
            if (response.equals("AUTH_OK")) {
                JOptionPane.showMessageDialog(this,
                        (action.equals("LOGIN") ? "Đăng nhập" : "Đăng ký") + " thành công!");
                this.dispose(); // đóng form login

                // Mở giao diện client
                SwingUtilities.invokeLater(() -> {
                    Client clientUI = new Client(socket, username, dis, dos);
                    clientUI.setVisible(true);
                });
            } else {
                String msg = (action.equals("LOGIN") ? "Đăng nhập thất bại!" : "Đăng ký thất bại!");
                JOptionPane.showMessageDialog(this, msg);
                socket.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}
