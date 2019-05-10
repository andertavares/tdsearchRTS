package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.core.AI;
import rts.GameSettings;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import rts.units.UnitTypeTable;
import util.XMLWriter;
import utils.FileNameUtil;

/**
 * A class to run microRTS games to train and test MetaBot
 * @author anderson
 */
public class Runner {

	public static final int MATCH_ERROR = 2;
	public static final int DRAW = -1;
	public static final int P1_WINS = 0;
	public static final int P2_WINS = 1;
	/*
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		String configFile;
		
		Logger logger = LogManager.getRootLogger();
		
		if(args.length > 0){
			logger.debug("Loading experiment configuration from {}", args[0]);
			configFile = args[0];
		}
		else {
			logger.debug("Input not specified, reading from 'config/microrts.properties'");
			logger.debug("args: " + Arrays.toString(args));
			configFile = "config/microrts.properties";
		}
		
		// loads the two forms of configuration object
        prop = ConfigManager.loadConfig(configFile);
		GameSettings settings = GameSettings.loadFromConfig(prop);
		System.out.println(settings);
		
		UnitTypeTable utt = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        AI ai1 = loadAI(settings.getAI1(), utt, 1, prop);
        AI ai2 = loadAI(settings.getAI2(), utt, 2, prop);
        
        int numGames = Integer.parseInt(prop.getProperty("runner.num_games", "1"));
        
        for(int i = 0; i < numGames; i++){
        	
        	//determines the trace output file. It is either null or the one calculated from the specified prefix
    		String traceOutput = null;
    				
    		if(prop.containsKey("runner.trace_prefix")){
    			// finds the file name
        		traceOutput = FileNameUtil.nextAvailableFileName(
    				prop.getProperty("runner.trace_prefix"), "trace"
    			);
        		
    		}
        	
        	Date begin = new Date(System.currentTimeMillis());
        	int result = headlessMatch(ai1, ai2, settings, utt, traceOutput);
        	Date end = new Date(System.currentTimeMillis());
        	
        	System.out.print(String.format("\rMatch %8d finished with result %3d.", i+1, result));
        	//logger.info(String.format("Match %8d finished.", i+1));
        	
        	long duration = end.getTime() - begin.getTime();
        	
        	if (prop.containsKey("runner.output")){
        		try{
        			outputSummary(prop.getProperty("runner.output"), result, duration, begin, end);
        		}
        		catch(IOException ioe){
        			logger.error("Error while trying to write summary to '" + prop.getProperty("runner.output") + "'", ioe);
        			
        		}
        		
        	}
        	
        	
        	ai1.reset();
        	ai2.reset();
        }
        System.out.println(); //adds a trailing \n to the match count written in the loop.
        logger.info("Executed " + numGames + " matches.");
	}
	*/
	
	/**
	 * Runs a match between two AIs with the specified settings, without the GUI.
	 * Saves the trace to re-play the match if traceOutput is not null
	 * @param ai1
	 * @param ai2
	 * @param config
	 * @param traceOutput
	 * @return
	 * @throws Exception
	 */
	public static int headlessMatch(AI ai1, AI ai2, GameSettings config, String traceOutput) throws Exception{
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		
		UnitTypeTable types = new UnitTypeTable(config.getUTTVersion(), config.getConflictPolicy());
		
		PhysicalGameState pgs;
        

		try {
			pgs = PhysicalGameState.load(config.getMapLocation(), types);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while loading map from file: " + config.getMapLocation(), e);
			//e.printStackTrace();
			logger.severe("Aborting match execution...");
			return MATCH_ERROR;
		}

		GameState state = new GameState(pgs, types);
		
		// creates the trace logger
		Trace replay = new Trace(types);
        
        boolean gameover = false;
    	
        while (!gameover && state.getTime() < config.getMaxCycles()) {
        	
        	// initializes state equally for the players 
        	GameState player1State = state; 
        	GameState player2State = state; 
        	
        	// places the fog of war if the state is partially observable
        	if (config.isPartiallyObservable()) {
        		player1State = new PartiallyObservableGameState(state, 0);
        		player2State = new PartiallyObservableGameState(state, 0);
        	}
        	
        	// retrieves the players' actions
        	PlayerAction player1Action = ai1.getAction(0, player1State);
        	PlayerAction player2Action = ai2.getAction(1, player2State);
        	
        	// creates a new trace entry, fills the actions and stores it
        	TraceEntry thisFrame = new TraceEntry(state.getPhysicalGameState().clone(), state.getTime());
        	if (!player1Action.isEmpty()){
        		thisFrame.addPlayerAction(player1Action.clone());
        	}
        	if (!player2Action.isEmpty()) {
                thisFrame.addPlayerAction(player2Action.clone());
            }
        	replay.addEntry(thisFrame);

			
        	// issues the players' actions
			state.issueSafe(player1Action);
			state.issueSafe(player2Action);

			// runs one cycle of the game
			gameover = state.cycle();
		} 
		ai1.gameOver(state.winner());
		ai2.gameOver(state.winner());
		
		//traces the final state
		replay.addEntry(new TraceEntry(state.getPhysicalGameState().clone(), state.getTime()));
		
		// writes the trace
		if (traceOutput != null){
			XMLWriter xml = new XMLWriter(new FileWriter(traceOutput));
	        replay.toxml(xml);
	        xml.flush();
		}
		
		return state.winner();
    }

	/**
	 * Runs the specified number of matches, without the GUI, saving the summary to the specified file.
	 * Saves the trace of each match sequentially according to the tracePrefix is not null
	 * @param numMatches
	 * @param summaryOutput
	 * @param ai1
	 * @param ai2
	 * @param config
	 * @param traceOutput
	 * @throws Exception
	 */
	public static void repeatedHeadlessMatches(int numMatches, String summaryOutput, AI ai1, AI ai2, GameSettings config, String tracePrefix) throws Exception{
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		
		for(int i = 0; i < numMatches; i++){
        	
        	//determines the trace output file. It is either null or the one calculated from the specified prefix
    		String traceOutput = null;
    				
    		if(tracePrefix != null){
    			// finds the file name
        		traceOutput = FileNameUtil.nextAvailableFileName(
    				tracePrefix, "trace"
    			);
    		}
        	
        	Date begin = new Date(System.currentTimeMillis());
        	int result = headlessMatch(ai1, ai2, config, traceOutput);
        	Date end = new Date(System.currentTimeMillis());
        	
        	System.out.print(String.format("\rMatch %8d finished with result %3d.", i+1, result));
        	//logger.info(String.format("Match %8d finished.", i+1));
        	
        	long duration = end.getTime() - begin.getTime();
        	
        	if (summaryOutput != null){
        		try{
        			outputSummary(summaryOutput, result, duration, begin, end);
        		}
        		catch(IOException ioe){
        			logger.log(Level.SEVERE, "Error while trying to write summary to '" + summaryOutput + "'", ioe);
        		}
        	}
        	
        	
        	ai1.reset();
        	ai2.reset();
        }
        System.out.println(); //adds a trailing \n to the match count written in the loop.
        logger.info("Executed " + numMatches + " matches.");
	}
	
    
    public static void outputSummary(String path, int result, long duration, Date start, Date finish) throws IOException{
    	File f = new File(path);
		FileWriter writer; 
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.fine("Attempting to write the output summary to " + path);
		
    	if(!f.exists()){ // creates a new file and writes the header
    		logger.fine("File didn't exist, creating and writing header");
    		writer = new FileWriter(f, false); //must be after the test, because it creates the file upon instantiation
    		writer.write("#result,duration(ms),initial_time,final_time\n");
    		writer.close();
    	}
    	
    	// appends one line with each weight value separated by a comma
    	writer = new FileWriter(f, true); 
    	writer.write(String.format("%d,%d,%s,%s\n", result, duration, start, finish));
    	logger.fine("Successfully wrote to " + path); 
    	
    	writer.close();
	}
    
	/* *
	 * Loads an {@link AI} according to its name, using the provided UnitTypeTable.
	 * If the AI is {@link MetaBot}, loads it with the configuration file specified in 
	 * entry 'metabot.config' of the received {@link Properties} 
	 * @param aiName
	 * @param utt
	 * @param playerNumber
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 *
	public static AI loadAI(String aiName, UnitTypeTable utt, int playerNumber, Properties config) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		AI ai;
		
		Logger logger = LogManager.getRootLogger();
		logger.info("Loading {}", aiName);
		
		// (custom) loads MetaBot with its configuration file
		if(aiName.equalsIgnoreCase("metabot.MetaBot")) {
			
			String configKey = String.format("player%d.config", playerNumber);
			if(config.containsKey(configKey)){
				ai = new MetaBot(utt, config.getProperty(configKey));
			}
			else {
				ai = new MetaBot(utt);
			}
			
		}
		else { // (default) loads the AI according to its name
			Constructor<?> cons1 = Class.forName(aiName).getConstructor(UnitTypeTable.class);
			ai = (AI)cons1.newInstance(utt);
		}
		return ai;
	}*/
}

