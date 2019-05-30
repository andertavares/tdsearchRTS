package main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import features.FeatureExtractor;
import features.MapAware;
import features.MaterialAdvantage;
import features.UnitDistance;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.VictoryOnly;
import reward.WinLossDraw;
import reward.WinLossTiesBroken;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;
import utils.AILoader;

public class Train {
	
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
        Options options = Parameters.trainCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);

            System.exit(1);
        }

        String configFile = cmd.getOptionValue("config_input");
        String outputPrefix = cmd.getOptionValue("working_dir");
        
        Properties config = ConfigManager.loadConfig(configFile);
        
        // overrides config with command line parameters
        Parameters.mergeCommandLineIntoProperties(cmd, config);
		
		// retrieves initial and final reps		
		int initialRep = Integer.parseInt(config.getProperty("initial_rep", "0"));
		int finalRep = Integer.parseInt(config.getProperty("final_rep", "0"));
			
		// repCount counts the actual number of repetitions
		for (int rep = initialRep, repCount = 0; rep <= finalRep; rep++, repCount++) {
			// determines the output dir according to the current rep
			String outDir = outputPrefix + "/rep" + rep;
			
			// checks if that repetition has been played
			File repDir = new File(outDir);
			if(repDir.exists()) {
				File repFinished = new File(outDir + "/finished");
				if(repFinished.exists()) {
					logger.info("Repetition {} already finished, skipping...", rep);
					continue;
				}
				else {
					logger.info("Repetition {} started, but not finished. Overwriting and continuing from there.", rep);
					repDir.delete();
				}
			}
			
			// finally runs one repetition
			// player 0's random seed increases whereas player 1's decreases with the repetitions  
			run(config, outDir, rep, finalRep - repCount + 1);
			
			// writes a flag file named 'finished' to indicate this repetition ended
			File repFinished = new File(outDir + "/finished");
			if (!repFinished.createNewFile()) {
				logger.error("Unable to create file to indicate that repetition {} has finished!", rep);
			};
		}
	}
	
	public static void run(Properties config, String outputPrefix, int randomSeedP0, int randomSeedP1) throws Exception {
		
		int trainMatches = Integer.parseInt(config.getProperty("train_matches"));
		int testMatches = Integer.parseInt(config.getProperty("test_matches"));
		
		int maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
		
		int timeBudget = Integer.parseInt(config.getProperty("search.timebudget"));
		
        double epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial"));
        //epsilonDecayRate = Double.parseDouble(config.getProperty("td.epsilon.decay", "1.0"));
        
        double alpha = Double.parseDouble(config.getProperty("td.alpha.initial"));
        //alphaDecayRate = Double.parseDouble(config.getProperty("td.alpha.decay", "1.0"));
        
        double gamma = Double.parseDouble(config.getProperty("td.gamma"));
        double lambda = Double.parseDouble(config.getProperty("td.lambda"));
        
        String portfolioNames = config.getProperty("portfolio");
		
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable types = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        // loads the reward model (default=victory-only)
        RewardModel rewards = null;
        if(config.getProperty("rewards", "victory-only").equals("victory-only")) {
        	rewards = new VictoryOnly();
        }
        else if (config.getProperty("rewards", "victory-only").equals("winloss-tiebreak")) {
        	 rewards = new WinLossTiesBroken(maxCycles);
        }
        else if (config.getProperty("rewards", "victory-only").equals("winlossdraw")) {
        	rewards = new WinLossDraw(maxCycles);
        }
        
        FeatureExtractor featureExtractor = null;
        if(config.getProperty("features", "mapaware").equals("mapaware")) {
        	featureExtractor = new MapAware(types, maxCycles);
        }
        else if (config.getProperty("features", "mapaware").equals("material")) {
        	 featureExtractor = new MaterialAdvantage(types, maxCycles);
        }
        else if (config.getProperty("features", "mapaware").equals("distance")) {
        	featureExtractor = new UnitDistance(types, maxCycles);
        }
        
        // creates the player instance
		TDSearch player = new SarsaSearch(
			types, 
			PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))), 
			rewards,
			featureExtractor,
			maxCycles,
			timeBudget, alpha, epsilon, gamma, lambda, randomSeedP0
		);
		
		// creates the training opponent
		AI trainingOpponent = null;
		if("selfplay".equals(config.getProperty("train_opponent"))) {
			trainingOpponent = new SarsaSearch(
				types,
				PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))),
				rewards,
				featureExtractor,
				Integer.parseInt(config.getProperty("max_cycles")),
				timeBudget, alpha, epsilon, gamma, lambda, randomSeedP1
			);
		}
		else {
			trainingOpponent = AILoader.loadAI(config.getProperty("train_opponent"), types);
		}
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		
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
		Runner.repeatedMatches(types, trainMatches, outputPrefix + "/train.csv", player, trainingOpponent, visualizeTraining, settings, null);
		logger.info("Training finished. Saving weights to " + outputPrefix + "/weights_0.bin (and weights_1.bin if selfplay).");
		// save player weights
		player.saveWeights(outputPrefix + "/weights_0.bin");
		
		//save opponent weights if selfplay
		if (trainingOpponent instanceof TDSearch) {
			((TDSearch) trainingOpponent).saveWeights(outputPrefix + "/weights_1.bin");
		}
		
		// test matches
		logger.info("Starting test...");
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		AI testOpponent = AILoader.loadAI(config.getProperty("test_opponent"), types);
		player.prepareForTest();
		Runner.repeatedMatches(types, testMatches, outputPrefix + "/test.csv", player, testOpponent, visualizeTest, settings, null);
		logger.info("Test finished.");
	}

}
