package config;

import java.util.Arrays;
import java.util.List;
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
        options.addOption(new Option("r", "rewards", true, "The reward model:  winloss-tiebreak or victory-only (default)"));
        options.addOption(new Option("o", "test_opponent", true, "Full name of the AI to test against (overrides the one specified in file)."));
        return options;
	}
	
	/**
	 * Retrieve the command line options used on test sessions
	 * @return
	 */
	public static Options testCommandLineOptions() {
		Options options = trainCommandLineOptions();
		options.addOption(new Option("r", "save_replay", false, "If omitted, does not generate replay (trace) files."));
		options.addOption(new Option("m", "test_matches", true, "Number of test matches."));
		
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
				"initial_rep", "final_rep", "train_opponent", "test_opponent", "rewards"
		);
		
		for(String paramName : overrideList) {
			if(cmd.hasOption(paramName)) {
				logger.debug("Parameter '{}' overridden to '{}'", paramName, cmd.getOptionValue(paramName));
				prop.setProperty(paramName, cmd.getOptionValue(paramName));
			}
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
		prop.setProperty("portfolio", csvPortfolio);	//stores the chosen portfolio back into prop
		
		
	}
	
	
	
}
