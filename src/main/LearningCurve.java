package main;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import learner.UnrestrictedPolicySelectionLearner;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import utils.AILoader;

/**
 * Almost identical to the Test class, but runs matches with different parameters to create
 * data to plot learning curves (number of victories at every training checkpoint).
 * @author anderson
 *
 */
public class LearningCurve extends Test {
	
	/**
	 * This is copied from Test.main. I thought it was not necessary to override main too, but
	 * it is to ensure that LearningCurve.runTestMatches is called rather than Test.runTestMatches
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		Properties config = Parameters.parseParameters(args); //ConfigManager.loadConfig(configFile);
        
		String baseDir = config.getProperty("working_dir");
		
		// retrieves some parameters to set into the repConfig below
		int numMatches = Integer.parseInt(config.getProperty("test_matches"));
		String saveReplay = config.getProperty("save_replay");
		
		// retrieves initial and final reps		
		int initialRep = Integer.parseInt(config.getProperty("initial_rep", "0"));
		int finalRep = Integer.parseInt(config.getProperty("final_rep", "0"));
				
		String testPartnerName = config.getProperty("test_opponent");
						
		boolean writeReplay = "true".equals(saveReplay);
		logger.info("Will {}save replays (.trace files).", writeReplay ? "" : "NOT ");
		
		
		for(int rep = initialRep; rep <= finalRep; rep++ ) {	
			String repDir = String.format("%s/rep%d", baseDir, rep);

			// loads the configuration, ensuring default values are set
			Properties repConfig = ConfigManager.loadConfig(repDir + "/settings.properties");
			repConfig = Parameters.ensureDefaults(repConfig);
			
			// puts the number of test matches and whether to save replays into the config
			repConfig.setProperty("test_matches", ""+numMatches); //""+ is just to easily convert to string
			repConfig.setProperty("save_replay", saveReplay);
			
			// runs one repetition
			// random seed = 0 should make no difference (no greedy actions)  
			runTestMatches(repConfig, testPartnerName, repDir, 0, 0, writeReplay);
		}
			
	}
		
	/**
	 * Almost the same as Test.runTestMatches, but uses custom parameters to create
	 * data for the learning curve
	 * TODO make the code more modular to avoid duplication
	 * 
	 * @param configPath
	 * @param testPartnerName
	 * @param workingDir
	 * @param randomSeedP0
	 * @param randomSeedP1
	 * @param writeReplay write the replay (traces) for each match?
	 * @throws Exception
	 */
	public static void runTestMatches(Properties config, String testPartnerName, String workingDir, int randomSeedP0, int randomSeedP1, boolean writeReplay) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		int testMatches = Integer.parseInt(config.getProperty("test_matches"));
		
		// voids learning and exploration
		config.setProperty("td.alpha.initial", "0");
		config.setProperty("td.epsilon.initial", "0");
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable types = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        logger.info("This experiment's config: ");
		logger.info(config.toString());
		
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		
		// which training checkpoint am I going to test?
		int checkpoint = Integer.parseInt(config.getProperty("checkpoint"));
        
		logger.info("{} write replay.", writeReplay ? "Will" : "Will not");
		
		UnrestrictedPolicySelectionLearner player = UnrestrictedPolicySelectionLearner.fromConfig(
    		types, randomSeedP0, config
        );
		
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
		logger.info("THIS IS LEARNING CURVE");
		
        // tests the learner both as player 0 and 1
        for (int testPosition = 0; testPosition < 2; testPosition++) {
        	// creates the player instance and loads weights according to its position
            
            String weightsFile = String.format("%s/weights_%d-m%d.bin", workingDir, testPosition, checkpoint);
            logger.info("Loading weights from {}", weightsFile);
            try {
            	player.loadWeights(weightsFile);
            }
            catch (IOException ioe) {
            	logger.error("Unable to load weights, ignoring {}.", weightsFile, ioe);
            	continue;
            }
    		
    		// if write replay (trace) is activated, sets the prefix to write files
    		String tracePrefix = null;
    		if(writeReplay) {
    			tracePrefix = String.format(
    				"%s/checkpoint-trace-vs-%s_p%d_m%d", 
    				workingDir, testOpponent.getClass().getSimpleName(), 
    				testPosition, checkpoint
    			); 
    		}
    				
    		AI p0 = player, p1 = testOpponent;
    		if(testPosition == 1) { //swaps the player and opponent if testPosition is 1
    			p0 = testOpponent;
    			p1 = player;
    		}
    		
    		logger.info("Testing: Player0={}, Player1={}", p0.getClass().getSimpleName(), p1.getClass().getSimpleName());
    		
    		Runner.repeatedMatches(
    			types, workingDir,
    			testMatches, // differently from Test class, runs all rather than half matches in each position
    			String.format("%s/lcurve-vs-%s_p%d_m%d.csv", workingDir, testOpponent.getClass().getSimpleName(), testPosition, checkpoint), //summary output
    			null, //choices prefix //String.format("%s/lcurve-vs-%s_p%d_m%d", workingDir, testOpponent.getClass().getSimpleName(), testPosition, checkpoint)
    			p0, p1, visualizeTest, settings, tracePrefix, 
    			0 // no checkpoints (we're already testing existing ones)
    		);
        }
        
        
		logger.info("Created learning curve data for checkpoint {} at {}.", checkpoint, workingDir);
	}
}
