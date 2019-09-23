package utils;

import java.io.File;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	
	/*public static String getExperimentDirWithRepNumber(Properties config) {
		String fullDirName = getExperimentDir(config);
       
        int repNumber = nextAvailableRepNumber(fullDirName, false);
        
        return String.format("%s/rep%d", fullDirName, repNumber);
	}*/

	public static String getExperimentDir(Properties config) {
		String workingDir = config.getProperty("working_dir");
        
        String dirName = String.format("%s/%s/%s/f%s_p%s_r%s/m%s/d%s/a%s_e%s_g%s_l%s", //all strings because they're retrieved from Property object 
    		workingDir, 
    		config.getProperty("train_opponent"),
    		new File(config.getProperty("map_location")).getName().replaceFirst("[.][^.]+$", ""),  //map name without extension
    		config.getProperty("features"),
    		config.getProperty("portfolio"),
    		config.getProperty("rewards"),
    		config.getProperty("train_matches"),
    		config.getProperty("decision_interval"),
    		config.getProperty("td.alpha.initial"),
    		config.getProperty("td.epsilon.initial"),
    		config.getProperty("td.gamma"),
    		config.getProperty("td.lambda")
		);
		return dirName;
	}

	/**
	 * Finds the next 'number' such that a file named namenumber does not exist.
	 * Useful for directories (e.g. rep0, rep1, ...) or files without extensions.
	 * 
	 * If restart is true: checks if a repetition has started but has not finished, deletes
	 * that directory and starts over from there
	 * 
	 * @param name
	 * @param restart
	 * @return
	 */
	public static int nextAvailableRepNumber(String workingDir, boolean restart) {

		Logger logger = LogManager.getRootLogger();
		for (int repNum = 0; true; repNum++) {
			String dirname = String.format("%s/rep%d", workingDir, repNum);
			File repDir = new File(dirname); 
			
			
			/*if(!repDir.exists() && !restart) {
				return repNum;
			}*/
			
			if(repDir.exists()) {
			
				if (!restart) continue; //skips if i don't need to check if the experiment has finished
				
				File repFinished = new File(repDir + "/finished");
				if(repFinished.exists()) {
					logger.info("Repetition {} already finished, skipping...", repNum);
					continue;
				}
				else { // repetition 
					logger.info("Repetition {} started, but not finished. Overwriting and continuing from there.", repNum);
					repDir.delete();
					return repNum;
				}
			}
			else { //repDir does not exist, return the reached number
				return repNum; 
			}
		} 
		
	}
}
