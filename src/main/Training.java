package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

import ai.core.AI;
import config.ConfigManager;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;

public class Training {
	
	public static void main(String[] args) throws Exception {
		Options options = new Options();

        options.addOption(new Option("i", "input", true, "Input config path"));
        options.addOption(new Option("o", "output", true, "Output dir"));

        options.addOption(new Option("s0", "p0-seed", true, "Random seed for player 0"));
        options.addOption(new Option("s1", "p1-seed", true, "Random seed for player 1"));

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

        String configFile = cmd.getOptionValue("input", "config/example.properties");
        String outputFilePath = cmd.getOptionValue("output", "results/example/");
		int p0Seed = Integer.parseInt(cmd.getOptionValue("p0-seed", "0"));
		int p1Seed = Integer.parseInt(cmd.getOptionValue("p1-seed", "1"));
		selfPlay(configFile, outputFilePath, p0Seed, p1Seed);
	}
	
	public static void selfPlay(String configPath, String outputPrefix, int randomSeedP0, int randomSeedP1) throws Exception {
		
		Properties config = ConfigManager.loadConfig(configPath);
		
		int trainMatches = Integer.parseInt(config.getProperty("train_matches"));
		int testMatches = Integer.parseInt(config.getProperty("test_matches"));
		
		int timeBudget = Integer.parseInt(config.getProperty("search.timebudget"));
		
		//int randomSeedP0 = Integer.parseInt(config.getProperty("random.seed.p0", "0"));
		//int randomSeedP1 = Integer.parseInt(config.getProperty("random.seed.p1", "1"));
        
        double epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial"));
        //epsilonDecayRate = Double.parseDouble(config.getProperty("td.epsilon.decay", "1.0"));
        
        double alpha = Double.parseDouble(config.getProperty("td.alpha.initial"));
        //alphaDecayRate = Double.parseDouble(config.getProperty("td.alpha.decay", "1.0"));
        
        double gamma = Double.parseDouble(config.getProperty("td.gamma"));
        double lambda = Double.parseDouble(config.getProperty("td.lambda"));
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable dummy = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
		AI player = new SarsaSearch(dummy, timeBudget, alpha, epsilon, gamma, lambda, randomSeedP0);
		AI opponent = new SarsaSearch(dummy, timeBudget, alpha, epsilon, gamma, lambda, randomSeedP1);
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		config.setProperty("AI1", player.getClass().getName());
		config.setProperty("AI2", opponent.getClass().getName());
		
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		
		logger.info("This experiment's config (to be copied to "+ outputPrefix + "/settings.properties): ");
		logger.info(config.toString());
		
		// creates output directory if needed
		File f = new File(outputPrefix);
		if (!f.exists()) {
			logger.info("Creating directory " + outputPrefix);
			System.out.println();
			f.mkdirs();
		}
		
		config.store(new FileOutputStream(outputPrefix + "/settings.properties"), null);
		
		// training matches
		logger.info("Starting training...");
		Runner.repeatedHeadlessMatches(trainMatches, outputPrefix + "/train.csv", player, opponent, settings, null);
    	
		// test matches
		logger.info("Starting test...");
		Runner.repeatedHeadlessMatches(testMatches, outputPrefix + "/test.csv", player, opponent, settings, outputPrefix + "/test-trace");
		logger.info("Test finished.");
	}

}
