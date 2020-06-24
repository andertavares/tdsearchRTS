package main;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import ensemble.MajorityVotingEnsemble;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import utils.AILoader;

public class TestEnsemble {
	public static void main(String[] args) throws Exception {
		
		/*
		 * Example of call:
		 * ./ensemble_test.sh -c config/ensemble_all.properties -d results/selflambda1M/selfplay/basesWorkers16x16A/fmaterialdistancehp_pWR,LR,RR,HR,WD,LD,RD,HD,BB,BK_rwinlossdraw/m1000000/d100/a0.01_e0.1_g0.99_l0.5/ --test_matches 40 --save_replay true --test_opponent ai.abstraction.WorkerRush --search_timebudget 0  -i 0 -f 0 
		 */
		
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
			repConfig.setProperty("gui", config.getProperty("gui"));
			
			// runs one repetition
			runTestMatches(repConfig, config, testOppName, repDir, writeReplay);
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
	public static void runTestMatches(Properties config, Properties ensembleConfig, String testPartnerName, String workingDir, boolean writeReplay) throws Exception {
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
		logger.info("{} write replay.", writeReplay ? "Will" : "Will not");
		
		// instantiates the players
		MajorityVotingEnsemble player = new MajorityVotingEnsemble(types, ensembleConfig);
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
		// path to weight files:
		// results/EXP/selfplay/MAP/fFEAT_pWR,LR,RR,HR,WD,LD,RD,HD,BB,BK_rwinlossdraw/
		//mMATCHES/d100/a0.01_e0.1_g0.99_l0.5/rep0/weights_p-mM.bin

		
        // tests the learner both as player 0 and 1
        for (int testPosition = 0; testPosition < 2; testPosition++) {
        	// loads the player's ensemble policies 
            loadPolicies(config, ensembleConfig, workingDir, player, testPosition);
            
    		// if write replay (trace) is activated, sets the prefix to write files
    		String tracePrefix = null;
    		if(writeReplay) {
    			tracePrefix = String.format(
    				"%s/test-trace-ensemble-vs-%s_p%d", 
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
    		
    		String choicesPrefix = null;/*"true".equalsIgnoreCase(config.getProperty("save_choices", "false")) ? 
    				String.format("%s/test-vs-%s_b%s", workingDir, testOpponent.getClass().getSimpleName(), config.getProperty("search.timebudget")) : //runner infers the test position, no need to pass in the prefix
    				null;*/
    		
    		Runner.repeatedMatches(
    			types, workingDir,
    			testMatches / 2, //half the matches in each position
    			String.format("%s/test-%s-vs-%s_p%d_b%s.csv", workingDir, ensembleConfig.getProperty("ensemble.name"), testOpponent.getClass().getSimpleName(), testPosition, config.getProperty("search.timebudget")),
    			choicesPrefix,
    			p0, p1, visualizeTest, settings, tracePrefix, 
    			0, // no checkpoints
    			0 //assumes no prior matches were played
    		);
        }
        
        
		logger.info("Test finished.");
	}

	private static void loadPolicies(Properties config, Properties ensembleConfig, String workingDir, 
			MajorityVotingEnsemble player, int playerPosition) throws IOException {
		
		Logger logger = LogManager.getRootLogger();
		
		// if policy files are specified by a pattern 
		if ("glob".equalsIgnoreCase(ensembleConfig.getProperty("policy.path.type"))) {
			String glob = "glob:" + String.format(ensembleConfig.getProperty("policy.paths", "crowd_%dm*.bin"), playerPosition);
			final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
			
			Files.walkFileTree(Paths.get(workingDir), new SimpleFileVisitor<Path>() {
				
				int count = 0;
				
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
					if (pathMatcher.matches(path)) {
						logger.info("Loading policy from {}", path);
						player.addSarsaPolicy(config, "crowd" + count++, path.toString());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc)
						throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		}
		else { // policy files are explicitly defined in config:

			// loads the policies appended by the player position
			String[] paths = ensembleConfig.getProperty("policy.paths").split(",");
			String[] names = ensembleConfig.getProperty("policy.names").split(",");
			
			if (paths.length != names.length) {
				throw new RuntimeException("Names and policy paths have differing lengths");
			}
			
			for (int i = 0; i < paths.length; i++) {
				String path = workingDir + "/" + String.format(paths[i], playerPosition);
				logger.info("Loading policy from {}", path);
				player.addSarsaPolicy(config, names[i], path);
			}
		}
		
	}
}
