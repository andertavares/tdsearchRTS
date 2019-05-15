package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Facilitates loading a Properties object from a file
 * @author anderson
 *
 */
public class ConfigManager {
	
	/**
	 * Loads the configuration from the given file into 
	 * a Properties object and returns it 
	 *  
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
	public static Properties loadConfig(String filename) throws IOException{
		
		Properties config = new Properties();
		InputStream is = new FileInputStream(filename.trim());
		config.load(is);
		is.close();
		
        return config;
	}
}
