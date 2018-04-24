package ourck.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/*-------------------------------------
 *	OURCK - 读取用户输入的模块
 *	2018年4月18日 上午8:59:57
 *-------------------------------------

/* 
 * 输入：用户的屏幕输入
 * 输出：String对象
 * 致敬cin。
 */
public class ScreenReader {
	
	public static final String jin()  {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));
		String str = null;
		try {
			str = reader.readLine();
		} catch (IOException e) {
			System.err.println(" [!] jin failed! Maybe try again?"); // Gulp.
			jin(); // Although this may cause dead circulation, But it's nearly a random event.
		}
		// This part just for something important in Java:
		if(str.equals(""))	// [!] "==" != equals() .
			str = "";		// [!] NULL != "" .
		return str;
	}
	
	public static void main(String[] args) {
		String str = jin();
		System.out.println(str);
	}

}
