package client;

import javax.swing.*;
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
        setTitle("🔑 Đăng nhập Client");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblUser = new JLabel("Tên đăng nhập:");
        txtUsername = new JTextField();
        JLabel lblPass = new JLabel("Mật khẩu:");
        txtPassword = new JPasswordField();

        btnLogin = new JButton("Đăng nhập");
        btnRegister = new JButton("Đăng ký");

        btnLogin.addActionListener(e -> handleAuth("LOGIN"));
        btnRegister.addActionListener(e -> handleAuth("REGISTER"));

        panel.add(lblUser);
        panel.add(txtUsername);
        panel.add(lblPass);
        panel.add(txtPassword);
        panel.add(btnLogin);
        panel.add(btnRegister);

        add(panel);
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
                JOptionPane.showMessageDialog(this, "✅ " + (action.equals("LOGIN") ? "Đăng nhập" : "Đăng ký") + " thành công!");
                this.dispose(); // đóng form login

                // Mở giao diện client
                SwingUtilities.invokeLater(() -> {
                    Client clientUI = new Client(socket, username, dis, dos);
                    clientUI.setVisible(true);
                });
            } else {
                String msg = "❌ " + (action.equals("LOGIN") ? "Đăng nhập thất bại!" : "Đăng ký thất bại!");
                JOptionPane.showMessageDialog(this, msg);
                socket.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠ Không kết nối được server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}
