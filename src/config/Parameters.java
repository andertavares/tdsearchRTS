package config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Parameters {
	/**
	 * Retrieve the command line options used on training sessions
	 * @return
	 */
	public static Options trainCommandLineOptions() {
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
        
        return options;
	}
	
	/**
	 * Retrieve the command line options used on test sessions
	 * @return
	 */
	public static Options testCommandLineOptions() {
		Options options = trainCommandLineOptions();
		options.addOption(new Option(null, "save_replay", false, "If omitted, does not generate replay (trace) files."));
		options.addOption(new Option("m", "test_matches", true, "Number of test matches."));
		options.addOption(new Option(null, "test_position", true, "0 or 1 (the player index of the agent under test)"));
		
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
				"initial_rep", "final_rep", "train_opponent", "test_opponent", 
				"test_matches", "rewards", "features", "train_matches", "strategies",
				"test_position"
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
		
		// the strategies parameters requires a special treatment if the user specified "all"
		String csvStrategies = null;
		if(cmd.hasOption("strategies")) {
			
			if("all".contentEquals(cmd.getOptionValue("strategies"))) {
				csvStrategies = "CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M"; 
			}
			
			if("basic".contentEquals(cmd.getOptionValue("strategies"))) {
				csvStrategies = "CE,FE,HP-,HP+,AV+"; 
			}
			
			logger.info("Parameter 'strategies' set to '{}'", csvStrategies);
			prop.setProperty("strategies", csvStrategies);
		}
		
		
		// the portfolio parameter requires a special treatment:
		// retrieves the portfolio from prop file, with the default as basic8 (4 rush, 4 offense)
		String csvPortfolio = prop.getProperty("portfolio", "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense");
		
		// overrides portfolio if specified via command line
		if(cmd.hasOption("portfolio")){
			
			if("basic4".equals(cmd.getOptionValue("portfolio"))) {
				logger.info("Using basic4 portfolio (only rush scripts)");
				csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush";
			}
		
			else if ("basic6".equals(cmd.getOptionValue("portfolio"))){
				logger.info("Using basic6 portfolio (rush+support scripts).");
				csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, BuildBase, BuildBarracks";
			}
			
			else if ("basic8".equals(cmd.getOptionValue("portfolio"))){
				logger.info("Using basic8 portfolio (rush+defense scripts).");
				csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense";
			}
			
			else if ("basic10".equals(cmd.getOptionValue("portfolio"))){
				logger.info("Using basic10 portfolio (rush+defense+support scripts).");
				csvPortfolio = "WorkerRush, LightRush, RangedRush, HeavyRush, WorkerDefense, LightDefense, RangedDefense, HeavyDefense, BuildBase, BuildBarracks";
			}
		}
		logger.info("Parameter 'portfolio' overridden to '{}'", csvPortfolio);
		prop.setProperty("portfolio", csvPortfolio);	//stores the chosen portfolio back into prop
		
		
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
			put("train_opponent", "selfplay");
			put("portfolio",  "basic4");
			put("rewards",  "winlossdraw");
			put("features",  "materialdistancehp");
			put("test_opponent",  "players.A3N");
			put("activation",  "identity");
			put("strategies",  "CE,FE,HP-,HP+,AV+");
			put("gui",  "false");
			put("train_matches", "100" );
			put("search.timebudget", "100" );
			put("td.alpha.initial",  "0.01");
			put("td.lambda",  "0.1");
		}};
		
		for(Entry<String, String> param : defaults.entrySet()) {
			if(!prop.containsKey(param.getKey())) {
				prop.setProperty(param.getKey(), param.getValue());
				logger.info("Parameter '{}' defaulting to '{}'", param.getKey(), param.getValue());
			}
		}
		
		
	}
	
}
