package ourck.rmi.server;

import java.rmi.*;
import java.io.*;
import ourck.utils.*;

public interface MyRMIInterface extends Remote {
	String doSomething() throws RemoteException;
	File sendLocalFile() throws IOException;
	LinearFileTree sendLocalFileTree() throws IOException;
	public byte[] sendFilePart(String resPath, int partIndex) throws IOException;
	PartedFile getPartedFileInfo(String resPath) throws IOException;
}
