package main;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.RewardModelFactory;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;
import utils.AILoader;

public class Test {
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
	 * This is basically a copy-and-paste from {@link Train.run} adapted to test matches
	 * TODO make the code more modular
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
        
		logger.info("{} write replay.", writeReplay ? "Will" : "Will not");
		
		UnrestrictedPolicySelectionLearner player = UnrestrictedPolicySelectionLearner.fromConfig(
    		types, randomSeedP0, config
        );
		
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
        // tests the learner both as player 0 and 1
        for (int testPosition = 0; testPosition < 2; testPosition++) {
        	// creates the player instance and loads weights according to its position
            
            String weightsFile = String.format("%s/weights_%d.bin", workingDir, testPosition);
            logger.info("Loading weights from {}", weightsFile);
    		player.loadWeights(weightsFile);
    		
    		// if write replay (trace) is activated, sets the prefix to write files
    		String tracePrefix = null;
    		if(writeReplay) {
    			tracePrefix = String.format(
    				"%s/test-trace-vs-%s_p%d", 
    				workingDir, testOpponent.getClass().getSimpleName(), 
    				testPosition
    			); 
    		}
    				
    		AI p0 = player, p1 = testOpponent;
    		if(testPosition == 1) { //swaps the player and opponent if testPosition is activated
    			p0 = testOpponent;
    			p1 = player;
    		}
    		
    		logger.info("Testing: Player0={}, Player1={}", p0.getClass().getSimpleName(), p1.getClass().getSimpleName());
    		
    		Runner.repeatedMatches(
    			types, workingDir,
    			testMatches / 2, //half the matches in each position
    			String.format("%s/test-vs-%s_p%d.csv", workingDir, testOpponent.getClass().getSimpleName(), testPosition),
    			String.format("%s/test-vs-%s", workingDir, testOpponent.getClass().getSimpleName()), //runner infers the test position, no need to pass in the prefix
    			p0, p1, visualizeTest, settings, tracePrefix, 
    			0 // no checkpoints
    		);
        }
        
        
		logger.info("Test finished.");
	}
}
