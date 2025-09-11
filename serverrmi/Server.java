package serverrmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {

	public static void main(String[] args) {
		//1. Định nghĩa Interface: những phương thức mà Server chia sẻ (Cả Client và Server đều phải có Interface này)
		//2. Bên phía Server:
		//		- Định nghĩa đối tượng kế thừa từ Interface sau đó định nghĩa các phương thức chia sẻ
		//		hoạt động như thế nào?
		//		- Khởi tạo đối tượng chứa phương thức chia sẻ
		//		- Đăng ký port chia sẻ
		//		- Public Method đó ra ngoài internet
		//3. Bên phía Client:
		//		- Remote đến Public Method trên ServerRMI
		//		- Sử dụng Public Method
		//Bài toán: Public Method tính tổng hai phân số
		try {
			//Khởi tạo đối tượng chứa Public Method
			TinhToan tt = new TinhToan();
			//Đăng ký port
			LocateRegistry.createRegistry(1001);
			//Đưa method ra port
			Naming.rebind("rmi://localhost:1001/tinhtoan", tt);
		} catch (RemoteException | MalformedURLException e) {
			System.out.println("Error Remote: " + e);
		}
	}

}
