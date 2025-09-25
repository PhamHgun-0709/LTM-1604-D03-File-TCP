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
Ứng dụng này mô phỏng việc **truyền file qua mạng** bằng mô hình **Client – Server P2P**.  

- **Server**: lắng nghe các client kết nối, quản lý danh sách client và trung gian truyền file giữa các client.  
- **Client**: gửi file tới client khác hoặc nhận file từ client khác thông qua server.  

**Mục tiêu chính:**  
- Hiểu nguyên lý hoạt động của giao thức TCP khi truyền dữ liệu.  
- Làm quen cách xây dựng kết nối **Client – Server** trong Java.  
- Thực hành thao tác với file: gửi, nhận, lưu trữ.  

**Các chức năng chính:**  
- Gửi file từ Client → Client khác thông qua Server.  
- Nhận file từ Client khác, đồng ý hoặc từ chối.  
- Cập nhật danh sách client online theo thời gian thực.  
- Hỗ trợ nhiều client kết nối đồng thời.  

---

## 🛠️ 2. Công nghệ sử dụng  

- **Ngôn ngữ lập trình:** Java  
- **Mạng & Giao thức:** TCP, Socket (`java.net.Socket`, `java.net.ServerSocket`)  
- **Xử lý dữ liệu:** `DataInputStream`, `DataOutputStream`, `FileInputStream`, `FileOutputStream`  
- **Giao diện người dùng (GUI):** Java Swing (`JFrame`, `JPanel`, `JTextArea`, `JButton`, `JLabel`, `JFileChooser`, `JTabbedPane`, `JList`)  
- **Layout managers:** BorderLayout, FlowLayout, GridBagLayout  
- **Look & Feel:** Nimbus  
- **Đa luồng:** Thread (xử lý client song song trên server và lắng nghe server trên client)  
- **Quản lý file & thư mục:** Thư mục `ReceivedFiles` để lưu file nhận được  

---

## 🚀 3. Một số hình ảnh hệ thống
### Giao diện Server

<p align="center">
  <img src="docs/Server.png" alt="Server" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 1</i></p>

### Giao diện Client

<p align="center">
  <img src="docs/Client.png" alt="Client" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 2</i></p>

### Danh sách Client online 

<p align="center">
  <img src="docs/Online.png" alt="Online" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 3</i></p>

### Chọn file để gửi

<p align="center">
  <img src="docs/Choose.png" alt="Choose" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 4</i></p>

### Nhận file từ client khác 

<p align="center">
  <img src="docs/Upload.png" alt="Upload" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 5</i></p>

### Đồng ý nhận file 

<p align="center">
  <img src="docs/Dongy.png" alt="Dongy" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 6</i></p>

### Từ chối nhận file 

<p align="center">
  <img src="docs/Tuchoi.png" alt="Tuchoi" width="600" height="600"/>
</p>
<p align="center"><i>Hình ảnh 7</i></p>

---

## 📝 4. Các bước cài đặt

#### Bước 1: Chuẩn bị môi trường
1. **Kiểm tra Java**: Mở terminal/command prompt và chạy:
```bash
java -version
javac -version
```
Đảm bảo cả hai lệnh đều hiển thị phiên bản Java 8 trở lên.

#### Bước 2: Biên dịch mã nguồn
1. Mở terminal và điều hướng đến thư mục chứa mã nguồn.
2. Biên dịch các file Java:
```bash
javac server/Server.java
javac clienta/Client.java
```
- Hoặc biên dịch toàn bộ dự án:
```bash
javac LTM-1604-D03-File-TCP/**/*.java
```
3. Kiểm tra kết quả: Nếu biên dịch thành công, sẽ tạo ra các file `.class` tương ứng.

#### Bước 3: Chạy ứng dụng
Khởi động Server:
```bash
java server.Server
```
- Server lắng nghe kết nối TCP trên port 12345.
- Giao diện server hiển thị danh sách client và log các kết nối.

Khởi động Client:

```bash
java client.Client
```
- Nhập ID của client và IP server (mặc định localhost) để kết nối.
- Giao diện Client hiển thị log, danh sách online, chọn file và gửi file.

### 🚀 Sử dụng ứng dụng
1. Bật Server: Server sẵn sàng nhận kết nối từ Client.
2. Kết nối: Client kết nối đến Server, log hiển thị các client đang online.
3. Gửi file: Client nhập ID người nhận, chọn file và gửi.
4. Nhận file: Client khác đồng ý hoặc từ chối nhận file.
5. Kiểm tra lịch sử gửi/nhận file.
6. Ngắt kết nối: Client có thể ngắt kết nối với Server.
7. Dừng Server: Server sẽ ngừng lắng nghe các client.

---

## 5. Liên hệ cá nhân
Nếu có bất kỳ thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ:

- 👨‍🎓 Họ và tên: Phạm Thành Hưng
- 🏫 Khoa: Công nghệ thông tin - Trường Đại học Đại Nam
- 📧 Email: pthung0709@gmail.com

© 2025 AIoTLab, Faculty of Information Technology, DaiNam University. All rights reserved.








