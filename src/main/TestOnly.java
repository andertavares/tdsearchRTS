package main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;

public class TestOnly {
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		Options options = new Options();

        options.addOption(new Option("c", "config-input", true, "Input config path"));
        options.addOption(new Option("t", "test_opponent", true, "Name of the AI to test against"));
        options.addOption(new Option("d", "working-dir", true, "Directory to load weights in and save results"));
        options.addOption(new Option("f", "final_rep", true, "Number of the final repetition (useful to parallelize executions). Assumes 0 if omitted"));
        options.addOption(new Option("i", "initial_rep", true, "Number of the initial repetition (useful to parallelize executions). Assumes 0 if omitted"));
        

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        String configFile = cmd.getOptionValue("config-input", "config/selfplay-example.properties");
        String workingDir = cmd.getOptionValue("working-dir");
        
        Properties config = ConfigManager.loadConfig(configFile);
		
		// if initial and final rep were specified via command line, ignore the ones in file		
		int initialRep = cmd.hasOption("initial_rep") ? 
				Integer.parseInt(cmd.getOptionValue("initial_rep")) : 
				Integer.parseInt(config.getProperty("initial_rep", "0"));
				
		int finalRep = cmd.hasOption("final_rep") ? 
				Integer.parseInt(cmd.getOptionValue("final_rep")) : 
				Integer.parseInt(config.getProperty("final_rep", "0"));
				
		String testPartnerName = cmd.hasOption("test_opponent") ? 
				cmd.getOptionValue("test_opponent") : 
				config.getProperty("test_opponent", "ai.abstraction.WorkerRush");
				
		for (int rep = initialRep; rep <= finalRep; rep++) {
			// determines the output dir according to the current rep
			String currentDir = workingDir + "/rep" + rep;
			
			// checks if that repetition has finished (otherwise it is not a good idea to test
			File repFinished = new File(currentDir + "/finished");
			if(! repFinished.exists()) {
				logger.warn("Repetition {} has not finished! Skipping...", rep);
				continue;
			}
			
			// finally runs one repetition
			// player 0's random seed increases whereas player 1's decreases with the repetitions  
			runTestMatches(configFile, testPartnerName, currentDir, rep, finalRep - rep + 1);
			
		}
	}
		
	/**
	 * This is basically a copy-and-paste from {@link Main.run} adapted to test matches
	 * TODO make the code more modular
	 * 
	 * @param configPath
	 * @param testPartnerName
	 * @param workingDir
	 * @param randomSeedP0
	 * @param randomSeedP1
	 * @throws Exception
	 */
	public static void runTestMatches(String configPath, String testPartnerName, String workingDir, int randomSeedP0, int randomSeedP1) throws Exception {
		
		Properties config = ConfigManager.loadConfig(configPath);
		
		int testMatches = Integer.parseInt(config.getProperty("test_matches"));
		
		int timeBudget = 0; // no planning budget, just gimme the greedy action
		
        double epsilon = 0;
        double alpha = 0;
        
        double gamma = 0;
        double lambda = 0;
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable dummyTypes = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        // creates the player instance and loads weights
		TDSearch player = new SarsaSearch(dummyTypes, timeBudget, alpha, epsilon, gamma, lambda, randomSeedP0);
		player.loadWeights(workingDir + "/weights_0.bin");
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		
		Logger logger = LogManager.getRootLogger();
		
		logger.info("This experiment's config (to be copied to "+ workingDir + "/settings.properties): ");
		logger.info(config.toString());
		
		//config.store(new FileOutputStream(workingDir + "/settings.properties"), null);
		
		// test matches
		logger.info("Starting test...");
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		AI testOpponent = Main.loadAI(testPartnerName, dummyTypes);
		Runner.repeatedMatches(
			testMatches, workingDir + "/test-vs-" + testOpponent.getClass().getSimpleName() + ".csv", 
			player, testOpponent, visualizeTest, settings, workingDir + "/test-trace-vs-" + testOpponent.getClass().getSimpleName()
		);
		logger.info("Test finished.");
	}
}