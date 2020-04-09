package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
		
		// retrieves initial, final reps and test opponent		
		int initialRep = Integer.parseInt(config.getProperty("initial_rep", "0"));
		int finalRep = Integer.parseInt(config.getProperty("final_rep", "0"));
		String testOppName = config.getProperty("test_opponent");
						
		boolean writeReplay = "true".equals(saveReplay);
		logger.info("Will {}save replays (.trace files).", writeReplay ? "" : "NOT ");
		
		
		for(int rep = initialRep; rep <= finalRep; rep++ ) {	
			String repDir = String.format("%s/rep%d", baseDir, rep);

			// loads the configuration, ensuring default values are set
			Properties repConfig = ConfigManager.loadConfig(repDir + "/settings.properties");
			repConfig = Parameters.ensureDefaults(repConfig);
			
			// puts the number of test matches, whether to save replays and search budget into the config
			repConfig.setProperty("test_matches", ""+numMatches); //""+ is just to easily convert to string
			repConfig.setProperty("save_replay", saveReplay);
			repConfig.setProperty("checkpoint", config.getProperty("checkpoint"));
			repConfig.setProperty("search.timebudget", config.getProperty("search.timebudget"));
			
			// runs one repetition
			runTestMatches(repConfig, testOppName, repDir, initialRep, initialRep+5000, writeReplay);
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
		
		// voids the learning and exploration rates
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
		
		// creates the planners and the player that uses them
		LinearSarsaLambda planner = LinearSarsaLambda.newPlanningAgent(types, config);
		LinearSarsaLambda planningOpponent = LinearSarsaLambda.newPlanningAgent(types, config);
		SarsaSearch player = new SarsaSearch(types, randomSeedP0, config, planner, planningOpponent);
		
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
        // tests the learner both as player 0 and 1
        for (int testPosition = 0; testPosition < 2; testPosition++) {
        	// creates the player instance and loads weights according to its position
            
        	// loads weight files of player & planning opponent, except if testing w/o training (checkpoint is zero)
        	String weightsFile = String.format("%s/weights_%d-m%d.bin", workingDir, testPosition, checkpoint);
        	String oppWeightsFile = String.format("%s/weights_%d-m%d.bin", workingDir, 1 - testPosition, checkpoint);
        	if(checkpoint != 0) { 
        		logger.info("Loading weights from {}", weightsFile);
                try {
                	player.loadWeights(weightsFile);
                	planner.load(weightsFile);
                }
                catch (IOException ioe) {
                	logger.error("Unable to load weights, ignoring {}.", weightsFile, ioe);
                	continue;
                }
                logger.info("Loading planningOpponent weights from {}", oppWeightsFile);
                try {
                	planningOpponent.load(oppWeightsFile);
                }
                catch (IOException ioe) {
                	logger.error("Unable to load of opp. planner {}, using random", oppWeightsFile, ioe);
                }
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
    		
    		String lcurveOutput = String.format(
				"%s/lcurve-vs-%s_p%d_m%d_b%s.csv", workingDir, testOpponent.getClass().getSimpleName(), 
				testPosition, checkpoint, config.getProperty("search.timebudget")
			); //summary output
    		
    		String choicesPrefix = "true".equalsIgnoreCase(config.getProperty("save_choices", "false")) ? 
				String.format("%s/lcurve-vs-%s_b%s", workingDir, testOpponent.getClass().getSimpleName(), config.getProperty("search.timebudget")) : //runner infers the test position, no need to pass in the prefix
				null;
    		
    		Runner.repeatedMatches(
    			types, workingDir,
    			remainingMatches(testMatches / 2, lcurveOutput), // runs up to half the matches in each position  
    			lcurveOutput, 
    			choicesPrefix, //String.format("%s/lcurve-vs-%s_p%d_m%d", workingDir, testOpponent.getClass().getSimpleName(), testPosition, checkpoint)
    			p0, p1, visualizeTest, settings, tracePrefix, 
    			0, // no checkpoints (we're already testing existing ones)
    			0 // latestMatch is zero to don't interfere with remainingMatches
    		);
        }
        
        
		logger.info("Created learning curve data for checkpoint {} at {}.", checkpoint, workingDir);
	}

	/**
	 * Calculates how many matches are needed to fill the output file up to 
	 * the target number of matches. 
	 * Returns zero if the file has the target number of matches or more.
	 * @param targetNumMatches
	 * @param outputFile
	 * @return
	 */
	private static int remainingMatches(int targetNumMatches, String outputFile) {
		
		File output = new File(outputFile);
		if(!output.exists()) {
			return targetNumMatches; // no match was executed 
		}
		
		// decrement targetNumMatches for each non-empty line in the file
		// TODO use LineNumberReader and reduce this to about two lines of code...
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(outputFile));
			
			String line;
			while ((line = reader.readLine()) != null){
			    if(!"".equals(line.trim())){ //skips empty lines
			        targetNumMatches--;
			    }
			}
		
		} catch (FileNotFoundException e) { // should not happen as we test this before
			e.printStackTrace();
		} catch (IOException e) {
			LogManager.getRootLogger().error("An error happened when reading '" + outputFile +"'", e);
		}
		targetNumMatches = Math.max(0, targetNumMatches+1); //adds 1 to compensate for the file header
		LogManager.getRootLogger().info("{} matches left to run.", targetNumMatches);
		return targetNumMatches;
	}
}
