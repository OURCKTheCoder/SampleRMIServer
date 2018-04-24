package ourck.rmi.server;

import java.rmi.*;
import java.rmi.server.*;

import java.rmi.registry.*;
import java.io.*;
import java.net.MalformedURLException;

import ourck.utils.*;

import static ourck.utils.ScreenReader.jin;

@SuppressWarnings("serial")
public class MyRMIServer extends UnicastRemoteObject implements MyRMIInterface {
//								   ^继承这个类是在表明“我是一个服务器端的对象”。
	private static final int REG_PORT = 1099;
	private static final int STUBEXP_PORT = 1099;
	
	public MyRMIServer() throws RemoteException, IOException {}
	
	@Override
	public PartedFile getPartedFileInfo(String resPath) throws IOException {
		File file = new File(resPath);
		System.out.print(resPath + " isExists: " + file.exists());
		return new PartedFile(file);
	}
	
	@Override
	public byte[] sendFilePart(String resPath, int partIndex) throws IOException {
		System.out.println(" - sendLocalFile() request from client." + partIndex);
		PartedFile f = new PartedFile(resPath);
		PartedFile.FilePart p = f.getPart(partIndex);
		return p.getData();
	}
	
	@Override // DEBUG-ONLY
	public File sendLocalFile() throws IOException {
		System.out.println(" - sendLocalFile() request from cient: Please choose a file to send.");
		String resFile = "/media/ourck/Backup/ISO/ubuntu-16.04.3-desktop-amd64.iso";
		return new File(resFile);
	}
	
	@Override
	public LinearFileTree sendLocalFileTree() {
		System.out.println(" - sendLocalFileTree() request from cient: Please choose a path to send.");
		String resPath = null;
		while(true) { // Until no exceptions.
			try {
				resPath = jin();
				if(!new File(resPath).exists()) 
					throw new IOException(" [!] File not exists! ");
				break;
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println(" - Try again!");
			}
		}
		return new LinearFileTree(resPath);
	}
	
	@Override
	public String doSomething() throws RemoteException {
		System.out.println("doIt!!!!!!!!!!!!");
		return "This is A greeting from doItServer."; 
	}
	
	public static void main(String[] args) {
		MyRMIServer server;
		try {
			LocateRegistry.createRegistry(REG_PORT);
			server = new MyRMIServer();
			Naming.rebind("rmi://localhost:" + REG_PORT + "/doItServer", server);
			// TODO How to delete the old stub?
//			try { UnicastRemoteObject.unexportObject(server, false);}
//			catch(NoSuchObjectException e) {e.printStackTrace();} 
			// Save the old-styled rmic.
			UnicastRemoteObject.exportObject(server, STUBEXP_PORT);
			// ------------Now this server is ready for RPC.------------
		} catch(ExportException e) {
			System.out.println(" [!] ExportException - "
					+ "Maybe the old stub has already exists, or the port is accupied."
					+ e.getMessage());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
}
