package main;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import learning.LinearSarsaLambda;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import utils.AILoader;

public class Test {
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		Properties config = Parameters.parseParameters(args); //ConfigManager.loadConfig(configFile);
        
		String baseDir = config.getProperty("working_dir");
		
		// retrieves some parameters to set into the repConfig below
		int numMatches = Integer.parseInt(config.getProperty("test_matches"));
		String saveReplay = config.getProperty("save_replay");
		
		// retrieves initial, final reps and test opponent		
		int initialRep = Integer.parseInt(config.getProperty("initial_rep", "0"));
		int finalRep = Integer.parseInt(config.getProperty("final_rep", "0"));
		String testOppName = config.getProperty("test_opponent");
						
		boolean writeReplay = "true".equals(config.getProperty("save_replay"));
		logger.info("Will {}save replays (.trace files).", writeReplay ? "" : "NOT ");
		
		
		for(int rep = initialRep; rep <= finalRep; rep++ ) {	
			String repDir = String.format("%s/rep%d", baseDir, rep);

			// loads the configuration, ensuring default values are set
			Properties repConfig = ConfigManager.loadConfig(repDir + "/settings.properties");
			repConfig = Parameters.ensureDefaults(repConfig);
			// FIXME must merge with command line parameters (e.g. to allow gui)!
			
			// puts the number of test matches, whether to save replays, GUI and search budget into the config
			repConfig.setProperty("test_matches", ""+numMatches); //""+ is just to easily convert to string
			repConfig.setProperty("save_replay", saveReplay);
			repConfig.setProperty("search.timebudget", config.getProperty("search.timebudget"));
			repConfig.setProperty("visualize_test", config.getProperty("visualize_test"));
			
			// runs one repetition
			runTestMatches(repConfig, testOppName, repDir, initialRep, initialRep+5000, writeReplay);
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
		//config.setProperty("td.alpha.initial", "0");
		//config.setProperty("td.epsilon.initial", "0");
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable types = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        logger.info("This experiment's config: ");
		logger.info(config.toString());
		
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		logger.info("{} write replay.", writeReplay ? "Will" : "Will not");
		
		// creates the planners and the player with them
		LinearSarsaLambda planner = LinearSarsaLambda.newPlanningAgent(types, config);
		LinearSarsaLambda planningOpponent = LinearSarsaLambda.newPlanningAgent(types, config);
		SarsaSearch player = new SarsaSearch(types, randomSeedP0, config, planner, planningOpponent);
		
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
        // tests the learner both as player 0 and 1
        for (int testPosition = 0; testPosition < 2; testPosition++) {
        	// creates the player instance and loads weights according to its position
            
            String weightsFile = String.format("%s/weights_%d.bin", workingDir, testPosition);
            logger.info("Loading weights from {}", weightsFile);
            player.loadWeights(weightsFile);
            planner.load(weightsFile);
            
            String oppWeightsFile = String.format("%s/weights_%d.bin", workingDir, 1 - testPosition);
            logger.info("Loading planningOpponent weights from {}", oppWeightsFile);
            planningOpponent.load(oppWeightsFile);
    		
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
    			String.format("%s/test-vs-%s_p%d_b%s.csv", workingDir, testOpponent.getClass().getSimpleName(), testPosition, config.getProperty("search.timebudget")),
    			String.format("%s/test-vs-%s_b%s", workingDir, testOpponent.getClass().getSimpleName(), config.getProperty("search.timebudget")), //runner infers the test position, no need to pass in the prefix
    			p0, p1, visualizeTest, settings, tracePrefix, 
    			0, // no checkpoints
    			0 //assumes no prior matches were played
    		);
        }
        
        
		logger.info("Test finished.");
	}
}
