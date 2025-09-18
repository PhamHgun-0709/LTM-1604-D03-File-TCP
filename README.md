<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   TRUYỀN FILE QUA TCP
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>


## 📖 1. Giới thiệu
Ứng dụng được xây dựng nhằm mô phỏng quá trình truyền file giữa các máy tính trong mạng thông qua giao thức TCP.  
Trong mô hình này, **Server** đóng vai trò trung gian, chịu trách nhiệm lắng nghe kết nối từ các **Client**, tiếp nhận dữ liệu (file) từ Client gửi đến và chuyển tiếp cho Client nhận.  

**Mục tiêu chính:**  
- Hiểu rõ cách hoạt động của giao thức TCP trong việc truyền dữ liệu.  
- Nắm vững cơ chế kết nối Client – Server.  
- Thực hành xử lý dữ liệu file (gửi/nhận).  

**Chức năng cơ bản:**  
- Gửi file từ một Client lên Server.  
- Server lưu trữ và cho phép Client khác tải file về.  
- Quản lý nhiều kết nối Client đồng thời.  

---

## 🛠️ 2. Công nghệ sử dụng  

- **Ngôn ngữ lập trình**: Java (JDK 8+).  
- **Giao thức mạng**: TCP Socket (`java.net.Socket`, `java.net.ServerSocket`).  
- **Xử lý đa luồng**: `Thread` để phục vụ nhiều Client đồng thời.  
- **Công nghệ giao diện**: Java Swing (JTable, JButton, JTextArea, JSplitPane).  
- **Cơ chế truyền dữ liệu**: `DataInputStream` và `DataOutputStream` để truyền file và lệnh (UPLOAD, DOWNLOAD, LIST).  
- **Thư viện sử dụng**:  
  - `java.net`  
  - `java.io`  
  - `javax.swing`, `java.awt`  
- **Công cụ phát triển**: IntelliJ IDEA / Eclipse / NetBeans (tùy chọn).  
- **Phiên bản JDK**: Java SE (JDK 8 trở lên).  
- **Hệ điều hành**: Windows 10 (có thể chạy đa nền tảng Linux, macOS).  


---

## 3. Một số hình ảnh hệ thống
## 4. Các bước cài đặt

## 5. Liên hệ cá nhân
Nếu có bất kỳ thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ:

📧 Email: pthung0709@gmail.com






