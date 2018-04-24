package ourck.utils;
/*-------------------------------------
 *	OURCK - 文件传输模块
 *	2018年4月17日 下午8:27:04
 *-------------------------------------

/* 
 * TODO 文件传输。考虑采用如下方式：
 * 1.IOStream; [#]
 * 2.Reader / Writer;(Character-oriented) [X]
 * 3.RandomFileAccess;
 * 4.MappedByteBuffer;
 */


import java.io.*;

public class FileTransferTask {
	private static final int BLOCK_SIZE = 2 * 1024 * 1024; // 2M as a block.
	private boolean closeflag;
	private boolean isOverwrite;
	
	private File resfile;
	private File destfile;
	private long totalSize;
	
	private BufferedInputStream in;
	private BufferedOutputStream out;
	
	public class ProgressBar { // REAL ASK LIFE...orz
		// TODO Maybe another thread here?
		private long totalSizeOnSpotted;
		private static final int FIELD_WIDTH = 50;
		private static final double STEP = FIELD_WIDTH / 100.0;

		double currentProgress;
		int percent;
		
		public ProgressBar() throws IOException {
			totalSizeOnSpotted = totalSize;		// Get the total size.
		}
		
		public ProgressBar(long size) throws IOException {
			totalSizeOnSpotted = size;
		}
		
		public void header() {
			System.out.print(FileTransferTask.this + "[ ");
		}
		
		public void refresh() {
			try {
				long leftbytes = 0; 
				//两种情况：“FileTransferTask是以流的形式初始化和以文件的方式初始化？”
				if (resfile != null) { leftbytes = resfile.length() - destfile.length();}
				else if(in != null) {leftbytes = in.available();} 
				currentProgress = (totalSizeOnSpotted - leftbytes) / (double)totalSizeOnSpotted;
				
				int oldp = percent; 
				int tmp = (int)(currentProgress * 100);
				if(tmp - oldp >= (int)(1.0 /STEP)) {
					percent = (int)(tmp);
					int sharpnum = (int)((percent - oldp) * FIELD_WIDTH / 100.0);
					for(int i = 0; i < sharpnum; i++)
						System.out.print("#");
				}
				if(totalSizeOnSpotted == 0) { // If transfer an empty file to another
					int sharpnum = FIELD_WIDTH;
					for(int i = 0; i < sharpnum; i++)
						System.out.print("#");
				}
				if(leftbytes == 0) {
					System.out.println(" ] FINISHED!\n");
				}
			} catch(IOException e) {
				System.out.println(" ( x_x ) Oops! Failed to get progress! \n");
			}
		}
	}
	
	private void openStream() throws IOException {
		// Open stream for IO
		if(in == null || out == null) {
			closeflag = false;
			in = new BufferedInputStream(
					new FileInputStream(resfile));
			out = new BufferedOutputStream(
					new FileOutputStream(destfile));
		}
		totalSize = in.available();
	}
	
	public void close() throws IOException {
		in.close();
		out.close();
		closeflag = true;
	}
	
	public void transfer() throws IOException {
		openStream();
		/*关于文件大小计数的问题
		 * 
		 * 默认情况下File.length返回的是long类型的值，
		 * 而新建byte数组时不能使用long作为维度。
		 * 
		 * 因此在一些地方 采取了将long强制转换为int值用来声明数组长度的做法，
		 * 这是安全的，因为在这些地方文件将分块传输，
		 * 每一块的大小都是 2 * 1024 * 1024，可作为int值存放。
		 * 
		 * 唯一不好地地方大概就是long比int多占4个字节这一点了。
		 */
		if(isClosed()) throw new RuntimeException("[!] Stream closed!");
		if(!isOverwrite) {
			File target = destfile;
			if(target.exists()) {
				System.out.println(" [!] The file: "
						+ "\n     \'" + target.getAbsolutePath() + "\'"
						+ "\n     has already existed. Abort.\n");
				return;
			}
		}
		
		totalSize = in.available();		// Get the total size. TODO 多余？
		
		if(totalSize < BLOCK_SIZE) transferOnce(); // When the file is too small.
		else {
			
			int blockNum = (int) (totalSize / BLOCK_SIZE);
			int restBytes = (int) (totalSize % BLOCK_SIZE);	// Usually there's something left if divided
													// by blocks.
			ProgressBar p = this.new ProgressBar();
			p.header();
			for(int i = 0; i < blockNum; i++) {
				transferOnce(0, BLOCK_SIZE);		// [!]the transfer is continuous until the
													// stream members are closed.
				p.refresh();
			}
			if(restBytes != 0) transferOnce(0, restBytes); 
			p.refresh();
			close(); // Auto close.
		}
	}
	
	public void transferOnce() throws IOException {
		if(isClosed()) throw new RuntimeException("[!] Stream closed!");
		ProgressBar p = this.new ProgressBar();
		p.header();
		
		openStream();
		byte[] data = null;
		if(totalSize > Integer.MAX_VALUE) {
			System.err.println(" [!] This file is too big for single transfer process!");
			return;
		}
		data = new byte[(int)totalSize]; // Should be less than INT_MAX
		in.read(data); out.write(data);
		
		p.refresh();
		close();
	}
	
	public void transferOnce(int off, int len) throws IOException { 
		// int instead of long: Every call the arg "len" should be less than BLOCK_SIZE(int value)
		// Before calling this method make sure the stream is opened - AND WON'T CLOSE IT!
		if(isClosed()) throw new RuntimeException("[!] Stream closed!");
		byte[] data = new byte[(int)len];
		in.read(data, off, len); 
		out.write(data);
	}
	
	// +5 Overloaded constructor.
	public FileTransferTask(String resPath, String destPath, boolean isOverwrite) throws IOException {
		this.isOverwrite = isOverwrite;
		resfile = new File(resPath);
		destfile = new File(destPath);
	}
	
	public FileTransferTask(File resPath, File destPath, boolean isOverwrite) 
	throws IOException {
		this(resPath.getAbsolutePath(), destPath.getAbsolutePath(), isOverwrite);
	}
	
	public FileTransferTask(String resPath, File destPath, boolean isOverwrite)
	throws IOException {
		this(resPath, destPath.getAbsolutePath(), isOverwrite);
	}
	
	public FileTransferTask(File resPath, String destPath, boolean isOverwrite) 
	throws IOException {
		this(resPath.getAbsolutePath(), destPath, isOverwrite);
	}
	
	public FileTransferTask(BufferedInputStream res, BufferedOutputStream target, boolean isOverwrite) 
	throws IOException {
		in = res; out = target; this.isOverwrite = isOverwrite;
		openStream();
	}
	
	public String toString() {
		StringBuilder stb = new StringBuilder();
		stb.append("-------------------FileTransferTask-------------------" + "\n");
		stb.append(" From ( " + totalSize + " bytes ) :" + "\n");
		stb.append("\t" + resfile.getAbsolutePath() + "\n");
		stb.append(" To :" + "\n");
		stb.append("\t" + destfile.getAbsolutePath() + "\n");
		stb.append("------------------------------------------------------" + "\n");
		return stb.toString();
	}
	
	public boolean isClosed() { return closeflag; }
	
	public static void main(String[] args) throws IOException {
		if(args.length != 2) {
			System.out.println(" [!] Usage: FileTransferTask [res] [dest]");
			System.exit(1);
		}
		FileTransferTask task = new FileTransferTask(args[0], args[1], true);
//		task.transferOnce();	// Memory use: 3GB
		task.transfer();		// Memory use: 56.7MB
//		task.close(); //Remember this!
	}

}
