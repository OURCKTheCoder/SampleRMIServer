package ourck.utils;

import java.nio.*;
import java.nio.channels.FileChannel;
import java.io.*;
import java.util.*;

public class PartedFile extends File implements Iterable<PartedFile.FilePart>{

	private static final long serialVersionUID = 7603063081818473489L;

	private static final int PART_SIZE = 5 * 1024 * 1024;

	private int partNum;
	private int restBytesSize;
	private long cksum;
	
	public PartedFile(String pathname) throws IOException {
		super(pathname);
		if(!exists()) createNewFile(); 
		partNum = (int) (length() / PART_SIZE); //long -> int is safe here.
		restBytesSize = (int) (length() % PART_SIZE);
		cksum = ChecksumVerify.getValue(this);
	}
	
	public PartedFile(File res) throws IOException {
		super(res.getPath()); //为何不能在调用父类构造器后再调用自己的？
		if(!exists()) createNewFile(); 
		partNum = (int) (length() / PART_SIZE); //long -> int is safe here.
		restBytesSize = (int) (length() % PART_SIZE);
		cksum = ChecksumVerify.getValue(this);
	}
	
	public long getChecksum() { return cksum; } 
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PartedFile)) return false;
		else {
			return ((PartedFile)obj).getChecksum() == this.getChecksum();
		}
	}
	
	public int partNum() { return partNum; }
	
	public int partSize(int partIndex) {
		int currentPartSize = PART_SIZE;
		if(partIndex == partNum)
			currentPartSize = restBytesSize;
		return currentPartSize;
	}
	
	public FilePart getPart(int partIndex) throws IOException { // 0 < partIndex
		byte[] data = new byte[partSize(partIndex)]; 
		BufferedInputStream bis  = 
				new BufferedInputStream(
						new FileInputStream(this));
		/*
		 * "将读取的第一个字节存储在元素 b[off] 中，下一个存储在 b[off+1] 中，依次类推。"
		 * 																——JDK文档
		 * 也就是说，从new流中的每一次读取必定都是从头开始，只不过data[off]作为第一个字节。
		 * 那么需要读取流中间的数据的时候，可以使用InputStream.skip(n).
		 * 这将导致读取动作跳过前面的n个字节再开始
		 * 
		 * 其实，最简单的办法就是把InputStream作为类成员，这样连续读取会更方便。
		 * 但在这里，PartedFile是可序列化的对象，流对象是不可序列化的。
		 */
		long skipnum = 0;
		for(int i = 0; i < partIndex; i++) skipnum += partSize(i);
		bis.skip(skipnum);
		bis.read(data, 0, partSize(partIndex));
//		System.out.println("" + data[0] + data[data.length - 1]);
		bis.close();
		return new FilePart(data);
	}
	
	public List<FilePart> getAllParts() {
		List<FilePart> list = new ArrayList<FilePart>();
		for(FilePart fp : this) {
			list.add(fp);
		}
		return list;
	}
	
	public void buildFromParts(FilePart... fileParts) throws IOException {
		FileOutputStream os = new FileOutputStream(this);
		FileChannel out = os.getChannel();
		// Reuse instead of "new" to improve performance
		// Use the maximum (PART_SIZE) to initialize.
		ByteBuffer buff = ByteBuffer.allocate(PART_SIZE);
		
		for(FilePart fp : fileParts) {
			buff.clear();
//			System.out.println(fp);
			buff.put(fp.getData());
			buff.flip();
			out.write(buff);
		}
		
		out.close();
		os.close();
	}
	
	private class PartedFileIterator implements Iterator<FilePart>, Serializable{
		private static final long serialVersionUID = -6286831998871294230L;
		private int partPtr = 0;
		@Override
		public boolean hasNext() {
			return partPtr <= partNum;
		}

		@Override
		public FilePart next() {
			FilePart p = null;
			try {
				p = getPart(partPtr++);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return p;
		}
	}
	
	@Override
	public Iterator<FilePart> iterator() {
		return new PartedFileIterator(); 
	}

	public class FilePart implements Serializable{ // ReadOnly!
		private static final long serialVersionUID = 8834406274399430969L;
		private byte[] data;
		public FilePart(byte[] data) {
			this.data = data;
		}
		public byte[] getData() { 
//			System.out.println(data[0] + data[1] + data[2] + data[3] + data[4] + data[5]);
			return data;
		}
		public String toString() { return data.toString(); }
		public int length() { return data.length; }
	}
	
	public static void main(String[] args) throws IOException {
		PartedFile f = new PartedFile("/home/ourck/图片/57922595_p0.png");
		List<FilePart> list = new ArrayList<FilePart>();//f.getAllParts();
		for(FilePart fp : f) {
			System.out.println(fp + "  " + fp.length());
			list.add(fp);
		}
		System.out.println("--------------------------------");
		
		PartedFile newf = new PartedFile("/home/ourck/666.png");
		FilePart[] fps = list.toArray(new FilePart[list.size()]);
		for(FilePart fp : fps)
			System.out.println(fp + "  " + fp.length());
		newf.buildFromParts(fps);
	}
}

