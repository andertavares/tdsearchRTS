package main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
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
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.VictoryOnly;
import reward.WinLossTiesBroken;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;
import utils.AILoader;

public class Train {
	
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		Options options = new Options();

        options.addOption(new Option("c", "config-input", true, "Input config path"));
        options.addOption(new Option("o", "output", true, "Output dir"));
        options.addOption(new Option("f", "final_rep", true, "Number of the final repetition (useful to parallelize executions). Assumes 0 if omitted"));
        options.addOption(new Option("i", "initial_rep", true, "Number of the initial repetition (useful to parallelize executions). Assumes 0 if omitted"));
        options.addOption(new Option("t", "train_opponent", true, "Full name of the AI to train against (overrides the one specified in file)."));
        options.addOption(new Option("p", "portfolio", false, "The type of portfolio to use: basic or standard (default, does not contain support scripts)"));
        options.addOption(new Option("r", "rewards", false, "The reward model:  winloss-tiebreak or victory-only (default)"));
        
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
        String outputPrefix = cmd.getOptionValue("output", "results/selfplay-example/");
        
        Properties config = ConfigManager.loadConfig(configFile);
		
		// if initial and final rep were specified via command line, ignore the ones in file		
		int initialRep = cmd.hasOption("initial_rep") ? 
				Integer.parseInt(cmd.getOptionValue("initial_rep")) : 
				Integer.parseInt(config.getProperty("initial_rep", "0"));
				
		int finalRep = cmd.hasOption("final_rep") ? 
				Integer.parseInt(cmd.getOptionValue("final_rep")) : 
				Integer.parseInt(config.getProperty("final_rep", "0"));
			
		// overrides training partner if speficied via command line
		if(cmd.hasOption("train_opponent")) {
			//logger.info("Setting train_opponent to {}", cmd.getOptionValue("train_opponent"));
			config.setProperty("train_opponent", cmd.getOptionValue("train_opponent"));
			logger.info("Train opponent is {}", config.getProperty("train_opponent"));
		}
		
		// if the basic portfolio was specified, uses the support scripts
		if(cmd.hasOption("portfolio") && "basic".equals(cmd.getOptionValue("portfolio"))) {
			logger.info("Using basic portfolio.");
			config.setProperty("portfolio", "BuildBase, BuildBarracks, WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense");
		}
		else {
			logger.info("Using standard portfolio (no supporting scripts).");
			config.setProperty("portfolio", "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense");
		}
		
		// reward model
		if(cmd.hasOption("rewards") && "winloss-tiebreak".equals(cmd.getOptionValue("rewards"))) {
			logger.info("Using winloss-tiebreak rewards");
			config.setProperty("rewards", "winloss-tiebreak");
		}
		else {
			logger.info("Using standard rewards (1 on victory, 0 otherwise)");
			config.setProperty("rewards", "victory-only");
		}
		
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
        
        // loads the reward model
        RewardModel rewards = config.getProperty("rewards").equals("victory-only") ? new VictoryOnly() : new WinLossTiesBroken();
        
        // creates the player instance
		TDSearch player = new SarsaSearch(
			types, 
			PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))), 
			rewards,
			timeBudget, alpha, epsilon, gamma, lambda, randomSeedP0
		);
		
		// creates the training opponent
		AI trainingOpponent = null;
		if("selfplay".equals(config.getProperty("train_opponent"))) {
			trainingOpponent = new SarsaSearch(
				types,
				PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))),
				rewards,
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
