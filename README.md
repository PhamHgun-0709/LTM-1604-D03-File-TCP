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
Ứng dụng này mô phỏng việc **truyền file qua mạng** bằng mô hình **Client – Server**.  

- **Server**: đóng vai trò trung tâm, nơi lưu trữ các file và lắng nghe yêu cầu từ các Client.  
- **Client**: có thể upload file lên Server hoặc tải file từ Server về máy.  

Nói cách khác, chương trình cho phép nhiều máy tính trong cùng mạng (hoặc thông qua Internet) **chia sẻ file qua lại** một cách dễ dàng.  

**Mục tiêu chính:**  
- Hiểu nguyên lý hoạt động của giao thức TCP khi truyền dữ liệu.  
- Làm quen với cách xây dựng kết nối **Client – Server** trong Java.  
- Thực hành thao tác với file: gửi, nhận, lưu trữ.  

**Các chức năng chính:**  
- Upload file từ Client lên Server.  
- Download file từ Server về Client.  
- Server quản lý và hiển thị danh sách file.  
- Hỗ trợ nhiều Client kết nối cùng lúc.  

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
### Giao diện Server
<p align="center">
  <img src="docs/Server.png" alt="Server UI" width="800" height="800"/>
</p>
<p align="center"><i>Hình ảnh 1</i></p>
### Giao diện ClientA
<p align="center">
  <img src="docs/ClientA.png" alt="Server UI" width="800" height="800"/>
</p>
<p align="center"><i>Hình ảnh 2</i></p>
### Giao diện ClientB
<p align="center">
  <img src="docs/ClientB.png" alt="Server UI" width="800" height="800"/>
</p>
<p align="center"><i>Hình ảnh 3</i></p>
### Giao diện Upload 
<p align="center">
  <img src="docs/Upload.png" alt="Server UI" width="800" height="800"/>
</p>
<p align="center"><i>Hình ảnh 4</i></p>
## 4. Các bước cài đặt

## 5. Liên hệ cá nhân
Nếu có bất kỳ thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ:

📧 Email: pthung0709@gmail.com







