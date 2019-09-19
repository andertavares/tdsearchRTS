package main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.Parameters;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import utils.AILoader;
import utils.FileNameUtil;

public class Train {
	
	public Train() {
		// empty public constructor
	}
	
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
        Properties config = Parameters.parseParameters(args); //ConfigManager.loadConfig(configFile);
        
        String experimentDir = FileNameUtil.getExperimentDir(config);
       
        int repNumber = FileNameUtil.nextAvailableRepNumber(
    		experimentDir, "true".equals(config.getProperty("restart"))
    	);
        
        String fullDir = String.format("%s/rep%d", experimentDir, repNumber);
       		
		// runs one repetition
		// p0's random seed is the rep number, p1's is 5000 + repNumber 
        // TODO restore the experiment if the user requested so
        Train experiment = new Train();
		experiment.run(config, fullDir, repNumber, repNumber + 5000);
		
		// writes a flag file named 'finished' to indicate that this repetition ended
		File repFinished = new File(fullDir + "/finished");
		if (!repFinished.createNewFile()) {
			logger.error("Unable to create file to indicate that repetition {} has finished!", repNumber);
		};
	}
	
	public void run(Properties config, String workingDir, int randomSeedP0, int randomSeedP1) throws Exception {
		
		int trainMatches = Integer.parseInt(config.getProperty("train_matches"));
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable types = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        // creates the player instance
        SarsaSearch player = new SarsaSearch (
			types, randomSeedP0, config
		);
		
		// creates the training opponent
		AI trainingOpponent = null;
		if("selfplay".equals(config.getProperty("train_opponent"))) {
			trainingOpponent = new SarsaSearch(types, randomSeedP1, config);
		}
		else {
			trainingOpponent = AILoader.loadAI(config.getProperty("train_opponent"), types);
		}
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		
		Logger logger = LogManager.getRootLogger();
		
		logger.info("This experiment's config (to be copied to "+ workingDir + "/settings.properties): ");
		logger.info(config.toString());
		
		// creates output directory if needed
		File f = new File(workingDir);
		if (!f.exists()) {
			logger.info("Creating directory " + workingDir);
			System.out.println();
			f.mkdirs();
		}
		
		config.store(new FileOutputStream(workingDir + "/settings.properties"), null);
		
		// training matches
		logger.info("Starting training...");
		boolean visualizeTraining = Boolean.parseBoolean(config.getProperty("visualize_training", "false"));
		
		Runner.repeatedMatches(
			types, workingDir, 
			trainMatches, 
			workingDir + "/train.csv", 
			null, //won't record choices at training time 
			player, trainingOpponent, visualizeTraining, settings, null,
			Integer.parseInt(config.getProperty("checkpoint"))
		);
		
		logger.info("Training finished. Saving weights to " + workingDir + "/weights_0.bin (and weights_1.bin if selfplay).");
		// save player weights
		player.saveWeights(workingDir + "/weights_0.bin");
		
		//save opponent weights if selfplay
		if (trainingOpponent instanceof SarsaSearch) {
			((SarsaSearch) trainingOpponent).saveWeights(workingDir + "/weights_1.bin");
		}
		
	}

}
