package ourck.rmi.client;

import java.rmi.*;
import ourck.rmi.server.MyRMIInterface;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.io.*;

import ourck.utils.*;

import static ourck.utils.ScreenReader.jin;

public class MyRMIClient {
	private static int REG_PORT = 1099;
	private static String SERVER_ADDRESS =
			"rmi://" + "127.0.0.1" + ":" + REG_PORT + "/doItServer";
	/*-------------------------------------
	 *	OURCK - 关于Naming
	 *	2018年4月14日 上午10:24:04
	 *-------------------------------------
	
	/* 
	 * Naming 类提供在对象注册表中存储和获得远程对远程对象引用的方法。
	 * Naming 类的每个方法都可将某个名称作为其一个参数，
	 * 该名称是使用以下形式的 URL 格式的 java.lang.String：
 		//host:port/name
	 * host 是注册表所在的主机（远程或本地），
	 * port 是注册表接受调用的端口号，
	 * name 是未经注册表解释的简单字符串，指向与这个name绑定的服务器端远程对象。
	 * 		该远程对象将向注册表中键为name的条目导出Stub。
	 */
	
	private void modifyServerAddr() { 
		StringBuilder addr = new StringBuilder();
		String inputstr = new String();
		
		addr.append("rmi://");
		System.out.println(" - Enter the server's IP Address: (empty for default)");
		if((inputstr = jin()) == "") addr.append("127.0.0.1");
		else addr.append(inputstr);
		
		addr.append(":");
		System.out.println(" - Enter connection port: (empty for default 1099)");
		if((inputstr = jin()) == "") { addr.append(REG_PORT); }
		else { REG_PORT = Integer.parseInt(inputstr);  addr.append(REG_PORT); }
		
		addr.append("/doItServer");
		System.out.println(" + "  + addr.toString());
		SERVER_ADDRESS = addr.toString();
	}

	public MyRMIClient() {
		try {
			modifyServerAddr();
		} catch(NumberFormatException e) {
			System.err.println(" [!] Make sure the port is valid! Try again?");
			switch(jin()) { case "Y": case"y": modifyServerAddr();}
		}
	}
	
//	public void getFileFromServer(MyRMIInterface serverStub, String path, boolean isOverwrite) throws IOException {
//		try {
//			File gotten = serverStub.sendLocalFile();
//			FileTransferTask task = new FileTransferTask(gotten, path, isOverwrite);
//			task.transfer();
////			task.close(); // Remember it!!
//		} catch(IOException e) {
//			System.err.println(" [!] getFileFromServer(): IOException!");
//			e.printStackTrace();
//			throw e; // Exceptions chain 
//		}
//	} // Failed: Only get a local File reference.
	
	public void getBytesCtxtFromServer(MyRMIInterface serverStub, String pathOnServer, String localpath) throws IOException {
		PartedFile fileref = serverStub.getPartedFileInfo(pathOnServer);
		PartedFile target = new PartedFile(localpath);
		ByteFileTransferTask task = new ByteFileTransferTask(target);
		ByteFileTransferTask.ProgressBar p = task.new ProgressBar(fileref, target);
		
		p.header();
		for(int i = 0; i <= fileref.partNum(); i++) {
			task.transfer(serverStub.sendFilePart(pathOnServer, i));
			p.refresh();
		}
		task.close();
	}
	
	public void getFileTreeFromServer(MyRMIInterface serverStub, String path, boolean coverflag)
	throws IOException {
		HashMap<String, PartedFile> map = null; // Use this map to lookup files on server.
		try {
			LinearFileTree ft = serverStub.sendLocalFileTree();
			map = ft.buildFileTreeStructure(path, coverflag);
			for(String fileOnServer : map.keySet()) {
				getBytesCtxtFromServer(serverStub, 
										fileOnServer,
										map.get(fileOnServer).getPath());
			}
		} catch(IOException e) {
			System.err.println(" [!] getFileTreeFromServer() - IOException!");
			throw e;
		}
	}
	
	/* Method used for testing in main() */
	public static void letServerDOIT(MyRMIInterface serverStub) throws RemoteException {
		System.out.println(" -> Test 1. doItTest:");
		System.out.println(serverStub.doSomething());
		System.out.println();
	}
	
	public static void letServerSendFile(MyRMIClient client, MyRMIInterface serverStub) throws IOException {
		System.out.println(" -> Test 2. Get a BIG File from server: (a for abort)");
		if(!jin().equals("a")) {
			System.out.print(" [+] Enter remote file's path: ");
			String pathOnServer = jin();
			System.out.print(" [+] Enter saving path: ");
			String localpath = jin();
			client.getBytesCtxtFromServer(serverStub, pathOnServer, localpath);
		}
		System.out.println();
	}
	
	public static void letServerSendTree(MyRMIClient client, MyRMIInterface serverStub) throws IOException {
		System.out.println(" -> Test 3. Get the whole File tree from server: (a for abort)");
		if(!jin().equals("a")) {
			System.out.println("Again, enter the path for building tree");
			String path = null;
			path = jin();
			client.getFileTreeFromServer(serverStub, path, true);
		}
		System.out.println();
	}
	
	public static void main(String[] args) throws IOException {
		
		MyRMIInterface serverStub;
		MyRMIClient client = new MyRMIClient();
		try {
			System.out.print(" [-] Conecting... ");
			serverStub = (MyRMIInterface)Naming.lookup(SERVER_ADDRESS);
			System.out.println("Connected! WELCOME TO DOITSERVER! \n");
			System.out.println("==========================================================");
			
			// 1.
			letServerDOIT(serverStub);

			//			System.out.println(" -> Test 2. Get a BIG single local file instructed by server: (a for abort)");
//			if(!jin().equals("a")) {
//				System.out.println("... But first, enter the path for saving file:");
//				String savingpath1 = jin();
//				client.getFileFromServer(serverStub, savingpath1, true); // /media/ourck/Backup/ISO/testdata.bin
//			}
//			System.out.println();
			
			// 2.
			boolean flag = true;
			while(flag) { // Until no exception.
				flag = false;
				try {
					letServerSendFile(client, serverStub);
				} catch(IOException e) {
					System.err.println(" [!] IOException! check if the path is valid!");
					e.printStackTrace();
					System.err.println(" - Try again? (Y/y +1s)");
					switch(jin()) { 
						case "Y": 
						case "y": flag = true;
					}
				} 
			}

			// 3.
			flag = true;
			while(flag) {
				flag = false;
				try {
					letServerSendTree(client, serverStub);
				} catch(IOException e) {
					System.err.println(" [!] IOException! check if the path is valid!");
					e.printStackTrace();
					System.err.println(" - Try again? (Y/y +1s)");
					switch(jin()) { 
						case "Y": 
						case "y": flag = true;
					}
				} 
			}
			
		} catch(ConnectException e) {
			System.err.println(" [!] Connection failed! Try again...? Y/N");
			String is = jin();
			switch (is) {
				case "" :
				case "y":
				case "Y": main(args); break;
				case "n":
				case "N": System.exit(1); break;
				default : System.err.println("[!] Invalid input! Abort."); break; // Also exit.
			}
		}catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.err.println(" [!] Oops! Something happened on the server!");
			e.printStackTrace();
		}
	}
	
}
