package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.core.AI;
import gui.PhysicalGameStateJFrame;
import gui.PhysicalGameStatePanel;
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
	
	/**
	 * Runs a match between two AIs with the specified settings, without the GUI.
	 * Saves the trace to re-play the match if traceOutput is not null
	 * @param ai1
	 * @param ai2
	 * @param visualize
	 * @param config
	 * @param traceOutput
	 * @return
	 * @throws Exception
	 */
	public static int match(AI ai1, AI ai2, boolean visualize, GameSettings config, String traceOutput) throws Exception{
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		
		UnitTypeTable types = new UnitTypeTable(config.getUTTVersion(), config.getConflictPolicy());
		
		// the AIs were created with a different unit type table. Reset them with this one
		ai1.reset(types);
		ai2.reset(types);
		
		PhysicalGameState pgs;
        
		try {
			pgs = PhysicalGameState.load(config.getMapLocation(), types);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while loading map from file: " + config.getMapLocation(), e);
			logger.severe("Aborting match execution...");
			return MATCH_ERROR;
		}
		
		GameState state = new GameState(pgs, types);
		
		// creates the visualizer, if needed
		PhysicalGameStateJFrame w = null;
		if (visualize) w = PhysicalGameStatePanel.newVisualizer(state, 600, 600, config.isPartiallyObservable());
		
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
        		player2State = new PartiallyObservableGameState(state, 1);
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
			
			// updates GUI if needed
			if (visualize) {
                w.setStateCloning(state);
                w.repaint();
                try {
                    Thread.sleep(1);    // give time to the window to repaint
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
		} //end of the match
        
        if (visualize) w.dispose(); //clears visualizer if necessary
        
		ai1.gameOver(state.winner());
		ai2.gameOver(state.winner());
		
		//traces the final state
		replay.addEntry(new TraceEntry(state.getPhysicalGameState().clone(), state.getTime()));
		
		// writes the trace
		if (traceOutput != null){
			// creates missing parent directories if needed
			File f = new File(traceOutput);
    		if (f.getParentFile() != null) {
    			  f.getParentFile().mkdirs();
			}
    		
    		// writes the trace for real
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
	 * @param visualize
	 * @param config
	 * @param traceOutput
	 * @throws Exception
	 */
	public static void repeatedMatches(int numMatches, String summaryOutput, AI ai1, AI ai2, boolean visualize, GameSettings config, String tracePrefix) throws Exception{
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
        	int result = match(ai1, ai2, visualize, config, traceOutput);
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
    		// creates missing parent directories as well
    		if (f.getParentFile() != null) {
    			  f.getParentFile().mkdirs();
			}
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
    
	
}

