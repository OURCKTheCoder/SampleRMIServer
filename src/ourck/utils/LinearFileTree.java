package ourck.utils;

import java.io.*;
import java.util.*;

public class LinearFileTree implements Serializable {
	private static final long serialVersionUID = 7925055270804738354L;
	private File root;
	private ArrayList<String> originalFilePath = new ArrayList<>();
	private ArrayList<File> dirs = new ArrayList<>();
	private ArrayList<File> files = new ArrayList<>();
	private ArrayList<File> tidyLocalFileList = new ArrayList<>(); // TODO 还有更简单的办法解决平台无关性吗？
	public LinearFileTree(String path) {
		root = new File(path);
		traverse(root);
	}
	
	public HashMap<String, PartedFile> buildFileTreeStructure(String newRoot, boolean isOverwrite) throws IOException {
		String absPrefix = root.getAbsolutePath();
		// 1.Creating folders.
		for(File dir : dirs) {
			StringBuilder stb = new StringBuilder();
			stb.append(newRoot);
			stb.append(dir.getAbsolutePath().substring(absPrefix.length()));
			new File(stb.toString()).mkdirs();
		}
		
		// 2.Creating files.
		
		for(File file : files) {
			StringBuilder stb = new StringBuilder();
			stb.append(newRoot);
			stb.append(file.getAbsolutePath().substring(absPrefix.length()));
			File target = new File(stb.toString());
			target.createNewFile();
			
			/* --------------------------------------------------------------*/
			
			tidyLocalFileList.add(target);
			
		}
		
		HashMap<String, PartedFile> map = new HashMap<>();
		for(int i = 0; i < tidyLocalFileList.size(); i++) {
			map.put(originalFilePath.get(i), 
					new PartedFile(tidyLocalFileList.get(i)));
			// File on server -> Local file
		}
		return map;
	}
	
	public void traverse(File fileAsNode) {
		if(fileAsNode.isDirectory()) {
			dirs.add(fileAsNode);
			for(File childNode : fileAsNode.listFiles())
				traverse(childNode);
		} else {
			originalFilePath.add(fileAsNode.getAbsolutePath());
			files.add(fileAsNode);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder();
		stb.append("-------------------File tree info-------------------\n");
		stb.append("\"" + root.getAbsolutePath() + "\":\n");
		stb.append("All directories (" + dirs.size() + " dirs)"+ ":\n");
		stb.append("\t" + dirs + "\n");
		stb.append("All files (" + files.size() + " files)"+ ":\n");
		stb.append("\t" + files + "\n");
		stb.append("----------------------------------------------------\n");
		return stb.toString();
	}
	
	public static void main(String[] args) {
		LinearFileTree ft = new LinearFileTree("/media/ourck/Backup/Test/r");
		System.out.println(ft);
		try {
			ft.buildFileTreeStructure("/media/ourck/Backup/Test/t", false);  // [!] DO NOT ADD '\' or '/' !
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
