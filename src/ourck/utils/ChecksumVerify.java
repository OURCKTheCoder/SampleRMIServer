package ourck.utils;

import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ChecksumVerify {
	
	public static long getValue(File target) throws IOException {
		FileInputStream fis = new FileInputStream(target);
		CheckedInputStream cksum = new CheckedInputStream(fis, new CRC32());
		
		int i = 0;
		
		// 只取前面1G进行校验和（为了照顾大文件情形下的速度）。
		// 对于小于1G的文件直接取全部。
		int limit = fis.available() > 1024 * 1024 ? 1024 * 1024 : fis.available();
		
		// 有时使用整个文件进行校验和，内存占用不高，就是等的稍微久一点：
//		int limit = (int)target.length();
		while( i < limit) {
			cksum.read(); // 先读一部分，拿读了的这部分来校验
			i++;
		}
		
		long val = cksum.getChecksum().getValue();
		cksum.close();
		return val;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println(getValue(new File("/media/ourck/Backup/ISO/ubuntu-16.04.3-desktop-amd64.iso")));
	}

}
