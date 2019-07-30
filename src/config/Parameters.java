package config;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class Parameters {
	
	public static Properties parseParameters(String[] args) throws IOException {
		// parses command line options
		Options commandLine = commandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(commandLine, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", commandLine);
            System.exit(1);
        }
        
        // opens the configuration file
        String configFile = cmd.getOptionValue("config_input");
        Properties config = ConfigManager.loadConfig(configFile);
        
        // overrides config with command line parameters
        mergeCommandLineIntoProperties(cmd, config);
        
        // ensures non-specified parameters are set to default values
        ensureDefaults(config);
        
        // parses special parameters (e.g. portfolio, strategies)
        parseSpecialParameters(config);
        
        // config is good to go
        return config;
		
	}
	
	
	/**
	 * Retrieve the command line options used on training sessions
	 * @return
	 */
	public static Options commandLineOptions() {
		Options options = new Options();

        options.addOption(new Option("c", "config_input", true, "Path of configuration file"));
        options.addOption(new Option("d", "working_dir", true, "Working directory (where to save and load data)"));
        options.addOption(new Option("f", "final_rep", true, "Number of the final repetition (useful to parallelize executions). Assumes 0 if omitted"));
        options.addOption(new Option("i", "initial_rep", true, "Number of the initial repetition (useful to parallelize executions). Assumes 0 if omitted"));
        options.addOption(new Option("t", "train_opponent", true, "Full name of the AI to train against (overrides the one specified in file)."));
        options.addOption(new Option("p", "portfolio", true, "The type of portfolio to use: basic4 (4 rush), basic6 (rush+support), basic8 (default: 4 rush + 4 defense) or basic10 (rush+defense+support)"));
        options.addOption(new Option("r", "rewards", true, "The reward model:  winloss-tiebreak, winlossdraw or victory-only"));
        options.addOption(new Option("e", "features", true, "The feature model:  material, distance, materialdistancehp or mapaware"));
        options.addOption(new Option("o", "test_opponent", true, "Full name of the AI to test against (overrides the one specified in file)."));
        options.addOption(new Option("a", "activation", true, "Activation function for the value function approximator (default: identity)"));
        options.addOption(new Option("s", "strategies", true, "Strategies to consider for selecting the unrestricted unit"));
        options.addOption(new Option("g", "gui", false, "Activate GUI to visualize matches (if omitted, no GUI)."));
        options.addOption(new Option(null, "train_matches", true, "Number of training matches."));
        options.addOption(new Option(null, "search_timebudget", true, "Milisseconds of planning time."));
        options.addOption(new Option(null, "td_alpha_initial", true, "Initial learning rate (held constant throughout experiment by now)"));
        options.addOption(new Option(null, "td_lambda", true, "Eligibility trace parameter"));
        options.addOption(new Option(null, "decision_interval", true, "Number of frames to decision_interval a selection (this will be the interval between decision points)."));
		options.addOption(new Option(null, "save_replay", false, "If omitted, does not generate replay (trace) files."));
		options.addOption(new Option(null, "test_matches", true, "Number of test matches."));
		options.addOption(new Option(null, "test_position", true, "0 or 1 (the player index of the agent under test)"));
		options.addOption(new Option(null, "checkpoint", true, "Saves the weights every 'checkpoint' matches."));
		
		options.addOption(new Option(null, "restart", true, "(must indicate true or false) Restart an unfinished experiment (make sure it is not running in another program instance!)"));
        
        return options;
	}
	
	/**
	 * Merge parameters from command line and properties. 
	 * Command line parameters override those on properties. 
	 * @param cmd
	 * @param prop
	 */
	public static void mergeCommandLineIntoProperties(CommandLine cmd, Properties prop) {
		Logger logger = LogManager.getRootLogger();

		// overrides 'direct' parameters
		List<String> overrideList = Arrays.asList(
				"working_dir", "initial_rep", "final_rep", "train_opponent", "test_opponent", 
				"test_matches", "rewards", "features", "train_matches", "strategies",
				"test_position", "decision_interval", "restart", "checkpoint"
		);
		
		for(String paramName : overrideList) {
			if(cmd.hasOption(paramName)) {
				logger.info("Parameter '{}' overridden to '{}'", paramName, cmd.getOptionValue(paramName));
				prop.setProperty(paramName, cmd.getOptionValue(paramName));
			}
		}
		
		// overrides GUI parameter
		if(cmd.hasOption("gui")) {
			prop.setProperty("visualize_training", "true");
			prop.setProperty("visualize_test", "true");
		}
		
		//parameters whose _ must be replaced by .
		List<String> underscoreToDot = Arrays.asList(
				"td_alpha_initial", "td_lambda", "search_timebudget"
		);
		for(String paramName : underscoreToDot) {
			if(cmd.hasOption(paramName)) {
				String dotParamName = paramName.replace('_', '.');
				logger.info("Parameter '{}' overridden to '{}'", dotParamName, cmd.getOptionValue(paramName));
				prop.setProperty(dotParamName, cmd.getOptionValue(paramName));
			}
		}
	}

	/**
	 * Interprets parameters with special meanings
	 * @param cmd
	 * @param prop
	 */
	private static void parseSpecialParameters(Properties prop) {
		Logger logger = LogManager.getRootLogger();
		
		// the strategies parameters requires a special treatment if the user specified some key words
		String csvStrategies = null;
		if("all".equals(prop.getProperty("strategies"))) {
			csvStrategies = "CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M"; 
		}
		
		if("basic".equals(prop.getProperty("strategies"))) {
			csvStrategies = "CE,FE,HP-,HP+,AV+"; 
		}
		
		if(csvStrategies != null) { // detects and effects the change in the parameter
			logger.info("Parameter 'strategies' set to '{}'", csvStrategies);
			prop.setProperty("strategies", csvStrategies);
		}
		
		
		// the portfolio parameter requires a special treatment:
		// retrieves the portfolio from prop file, with the default as basic8 (4 rush, 4 offense)
		String csvPortfolio = null; //prop.getProperty("portfolio", "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense");
		if("basic4".equals(prop.getProperty("portfolio"))) {
			logger.info("Using basic4 portfolio (only rush scripts)");
			csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush";
		}
	
		else if ("basic6".equals(prop.getProperty("portfolio"))){
			logger.info("Using basic6 portfolio (rush+support scripts).");
			csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, BuildBase, BuildBarracks";
		}
		
		else if ("basic8".equals(prop.getProperty("portfolio"))){
			logger.info("Using basic8 portfolio (rush+defense scripts).");
			csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense";
		}
		
		else if ("basic10".equals(prop.getProperty("portfolio"))){
			logger.info("Using basic10 portfolio (rush+defense+support scripts).");
			csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense, BuildBase, BuildBarracks";
		} 
		
		if (csvPortfolio != null) { // detects and effects the change in the parameter
			logger.info("Parameter 'portfolio' overridden to '{}'", csvPortfolio);
			prop.setProperty("portfolio", csvPortfolio);	//stores the chosen portfolio back into prop	
		}
		
	}
	
	/**
	 * Ensures that default values are in properties. This makes it easier to track
	 * experiment parameters when a properties loaded from a file with missing values is 
	 * saved afterwards: with this method, all parameters will be saved.
	 * Must be called after {@link #mergeCommandLineIntoProperties(CommandLine, Properties)}
	 * Only config_input and working_dir are not set
	 * @param prop
	 */
	public static void ensureDefaults(Properties prop) {
		Logger logger = LogManager.getRootLogger();
		/**
		 * Maps a parameter name to its default value
		 */
		@SuppressWarnings("serial")
		Map<String,String> defaults = new HashMap<>() {{
			put("final_rep", "0");
			put("initial_rep", "0");
			put("gui",  "false");
			put("save_replay", "false");
			
			put("train_opponent", "selfplay");
			put("train_matches", "100" );
			
			put("portfolio",  "basic4");
			put("rewards",  "winlossdraw");
			put("features",  "materialdistancehp");
			put("activation",  "identity");
			put("strategies",  "all");
			
			put("test_opponent",  "players.A3N");
			put("test_matches", "10");
			put("test_position", "0");
			
			put("search.timebudget", "100" );
			put("td.alpha.initial",  "0.01");
			put("td.lambda",  "0.1");
			
			put("decision_interval", "1");
			put("checkpoint", "10");
			
			put("restart", "false");
		}};
		
		for(Entry<String, String> param : defaults.entrySet()) {
			if(!prop.containsKey(param.getKey())) {
				prop.setProperty(param.getKey(), param.getValue());
				logger.info("Parameter '{}' defaulting to '{}'", param.getKey(), param.getValue());
			}
		}
		
		
	}
	
}
