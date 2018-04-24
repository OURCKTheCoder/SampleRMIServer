package ourck.utils;

import java.io.*;

/*-------------------------------------
 *	OURCK - 动态文件字节传输流任务
 *	2018年4月19日 下午4:19:56
 *-------------------------------------

/* 
 * 建立一个文件传输任务。该任务绑定一个本地文件，
 *`动态地向该文件传输byte[]数据。(覆写式）
 * 该任务应该在传输结束后被手动关闭。
 * （类似流）
 */

/*
 * 由于一些原因，这里不考虑使用继承自FileTransferTask的方法构建类。
 * 因为FileTransferTask是老早就写好的的工具类，
 * 里面的方法更多的是面向对本地文件的操作，并不是“面向字节的”传输类
 * 
 * 因此，考虑使用代理。
 */
public class ByteFileTransferTask { 
	private File target;
	private FileTransferTask task;
	
	private BufferedInputStream in; // [!] Allocate instance for "in" & "bytesTransferTask"
									 //		when its about to transfer(), not in the constructor.
	private BufferedOutputStream out;
	
	public class ProgressBar {
		private long totalSizeOnSpotted;
		private static final int FIELD_WIDTH = 50;
		private static final double STEP = FIELD_WIDTH / 100.0;

		private PartedFile resRef, targetRef;
		double currentProgress;
		int percent;
		
		public ProgressBar(PartedFile resRef, PartedFile targetRef) throws IOException {
			this.resRef = resRef;
			this.targetRef = targetRef;
			totalSizeOnSpotted = resRef.length();
		}
		
		public void header() {
			StringBuilder stb = new StringBuilder();
			stb.append("\n-------------------FileTransferTask-------------------" + "\n");
			stb.append(" From ( " + resRef.length() + " bytes ) :" + "\n");
			stb.append("\t" + resRef.getName() + "\n");
			stb.append(" To :" + "\n");
			stb.append("\t" + targetRef.getAbsolutePath() + "\n");
			stb.append("------------------------------------------------------" + "\n");
			stb.append("[ ");
			System.out.print(stb);
		}
		
		public void refresh() {
			try {
				currentProgress = targetRef.length() / (double)totalSizeOnSpotted;
				
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
				if(percent == 100) {
					System.out.print(" ] FINISHED!");
					// Verify checksum.
					System.out.print(" - Checksum...");
					if(targetRef.equals(resRef)) System.out.println("OK!");
					else System.out.println("NOT OK!");
					
//					System.out.println(
//							ChecksumVerify.getValue(targetRef) 
//							+ " " + resRef.getChecksum()); // DEBUG-ONLY
				}
			} catch(Exception e) {
				System.out.println(" ( x_x ) Oops! Failed to get progress! \n");
			}
		}
		
	}
	
	public ByteFileTransferTask(File targetRef) // Can't use ProgressBar!
	throws IOException, FileNotFoundException {
		target = targetRef;
		out = new BufferedOutputStream(
				new FileOutputStream(this.target));
	}
	
	public void transfer(byte[] part) throws IOException {
		in = new BufferedInputStream(
				new ByteArrayInputStream(part));
		task = new FileTransferTask(in, out, true);
		task.transferOnce(0, part.length);// [!] 用底层的方法而不直接调用transfer():因为要灵活传输
	}
	
	public void close() throws IOException { task.close(); }
	
	public static void main(String[] args) {
		try {
			PartedFile tmp = new PartedFile("/home/ourck/文档/Local/Project TIJ4/WorkSpace/666");
			tmp.createNewFile();
			ByteFileTransferTask task = new ByteFileTransferTask(tmp);
			task.transfer(new byte[] {1,1,1,1,1,1,1,1,1,1});
			task.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
