package utils;

import java.io.File;

public class FileNameUtil {
	
	/**
	 * Finds the next 'number' such that a file named: prefix_number.extension,
	 * does not exist. Returns the entire file name (prefix_number.extension)
	 * @param prefix
	 * @param extension
	 * @return
	 */
	public static String nextAvailableFileName(String prefix, String extension){
		String filename = String.format("%s_%d.%s", prefix, 1, extension);
		File file = new File(filename); 

		// finds the next number to append to prefix
		for (int num = 1; file.exists(); num++) {
			filename = String.format("%s_%d.%s", prefix, num, extension);
			file = new File(filename); 
		}
		
		return filename;
	}
}
