package main;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
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

public class Main {
	
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
        String outputPrefix = cmd.getOptionValue("output", "results/example/");
		int p0Seed = Integer.parseInt(cmd.getOptionValue("p0-seed", "0"));
		int p1Seed = Integer.parseInt(cmd.getOptionValue("p1-seed", "1"));
		selfPlay(configFile, outputPrefix, p0Seed, p1Seed);
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
        UnitTypeTable dummyTypes = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
		TDSearch player = new SarsaSearch(dummyTypes, timeBudget, alpha, epsilon, gamma, lambda, randomSeedP0);
		TDSearch trainingOpponent = new SarsaSearch(dummyTypes, timeBudget, alpha, epsilon, gamma, lambda, randomSeedP1);
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		config.setProperty("AI1", player.getClass().getName());
		config.setProperty("AI2", trainingOpponent.getClass().getName());
		
		Logger logger = LogManager.getRootLogger();
		
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
		boolean visualizeTraining = Boolean.parseBoolean(config.getProperty("visualize_training", "false"));
		Runner.repeatedMatches(trainMatches, outputPrefix + "/train.csv", player, trainingOpponent, visualizeTraining, settings, null);
		logger.info("Training finished. Saving weights to " + outputPrefix + "/weights_0.bin and weights_1.bin");
		player.saveWeights(outputPrefix + "/weights_0.bin");
		trainingOpponent.saveWeights(outputPrefix + "/weights_1.bin");
		
    	
		// test matches
		logger.info("Starting test...");
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		AI testOpponent = loadAI(config.getProperty("test_opponent"), dummyTypes);
		player.prepareForTest();
		Runner.repeatedMatches(testMatches, outputPrefix + "/test.csv", player, testOpponent, visualizeTest, settings, outputPrefix + "/test-trace");
		logger.info("Test finished.");
	}
	
	/**
	 * Loads an {@link AI} according to its name, using the provided UnitTypeTable.
	 * @param aiName
	 * @param types
	 * @return
	 * @throws Exception if unable to instantiate the AI instance
	 */
	public static AI loadAI(String aiName, UnitTypeTable types) throws Exception {
		AI ai;
		
		Logger logger = LogManager.getRootLogger();
		logger.info("Loading {}", aiName);
		
		Constructor<?> cons1 = Class.forName(aiName).getConstructor(UnitTypeTable.class);
		ai = (AI)cons1.newInstance(types);
		return ai;
	}

}
