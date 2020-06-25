package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import tdsearch.SarsaSearch;
import utils.FileNameUtil;
import utils.MatchData;

/**
 * A class to run microRTS games to train and test RL agents
 * @author anderson
 */
public class Runner {

	/**
	 * Runs a match between two AIs with the specified settings, without the GUI.
	 * Saves the trace to re-play the match if traceOutput is not null
	 * @param types
	 * @param ai1
	 * @param ai2
	 * @param visualize
	 * @param config
	 * @param traceOutput
	 * @return
	 * @throws Exception
	 */
	public static MatchData match(UnitTypeTable types, AI ai1, AI ai2, boolean visualize, GameSettings config, String traceOutput) throws Exception{
		Logger logger = LogManager.getRootLogger();
		
		//UnitTypeTable types = new UnitTypeTable(config.getUTTVersion(), config.getConflictPolicy());
		
		// makes sure the AIs are reset
		ai1.reset(types);
		ai2.reset(types);
		ai1.reset();
		ai2.reset();
		
		PhysicalGameState pgs;
        
		try {
			pgs = PhysicalGameState.load(config.getMapLocation(), types);
		} catch (Exception e) {
			logger.error("Error while loading map from file: " + config.getMapLocation(), e);
			logger.error("Aborting match execution...");
			return new MatchData(MatchData.MATCH_ERROR, 0);
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
    		
    		// ensures that traceOutput ends with a .zip
    		if (! traceOutput.endsWith(".zip")) {
    			traceOutput += ".zip";
    		}
    		
    		// writes the zipped trace file (much smaller)
    		replay.toZip(traceOutput);
    		
		}
		
		return new MatchData(state.winner(), state.getTime());
    }

	/**
	 * Runs the specified number of matches, without the GUI, saving the summary to the specified file.
	 * Saves the trace of each match sequentially according to the tracePrefix is not null
	 * @param types
	 * @param workingDir
	 * @param numMatches
	 * @param summaryOutput
	 * @param choicesPrefix
	 * @param ai1
	 * @param ai2
	 * @param visualize
	 * @param gameSettings
	 * @param tracePrefix
	 * @param checkpoint
	 * @param latestMatch if there were matches played before, start from there
	 * @throws Exception
	 */
	public static void repeatedMatches(
			UnitTypeTable types, 
			String workingDir, 
			int numMatches, String summaryOutput, String choicesPrefix, 
			AI ai1, AI ai2, 
			boolean visualize, GameSettings gameSettings, String tracePrefix, 
			int checkpoint, int latestMatch
	) throws Exception {
		
		Logger logger = LogManager.getRootLogger();
		
		for(int matchNumber = latestMatch; matchNumber < numMatches; matchNumber++){
        	
        	//determines the trace output file. It is either null or the one calculated from the specified prefix
    		String traceOutput = null;
    				
    		if(tracePrefix != null){
    			// finds the file name
        		traceOutput = FileNameUtil.nextAvailableFileName(
    				tracePrefix, "trace.zip"	//trace files are saved as .trace.zip
    			);
    		}
        	
    		// runs the match, recording the initial and finishing times
        	Date begin = new Date(System.currentTimeMillis());
        	MatchData data = match(types, ai1, ai2, visualize, gameSettings, traceOutput);
        	Date end = new Date(System.currentTimeMillis());
        	
        	System.out.print(String.format("\rMatch %8d finished with result %3d, taking %8d frames.", matchNumber+1, data.winner, data.frames));
        	
        	// saves weights every 'checkpoint' matches (adds 1 to matchNumber because it is starts at 0
        	if (checkpoint > 0 && (matchNumber+1) % checkpoint == 0) {
        		checkpoint(new AI[] {ai1, ai2}, workingDir, matchNumber+1);
        	}
        	
        	// writes summary
        	long duration = end.getTime() - begin.getTime();
        	if (summaryOutput != null){
        		try{
        			outputSummary(summaryOutput, data.winner, duration, data.frames, begin, end);
        		}
        		catch(IOException ioe){
        			logger.error("Error while trying to write summary to '" + summaryOutput + "'", ioe);
        		}
        	}
        	
        	// appends choices
        	if (choicesPrefix != null) {
        		try{
        			//tries to output choices regardless of player position
        			if(ai1 instanceof SarsaSearch) {
	        			outputChoices(choicesPrefix + "_p0.choices", matchNumber, ((SarsaSearch)ai1).getChoices());
        			}
        			
        			if(ai2 instanceof SarsaSearch) {
	        			outputChoices(choicesPrefix + "_p1.choices", matchNumber, ((SarsaSearch)ai2).getChoices());
        			}
        		}
        		catch(IOException ioe){
        			logger.error("Error while trying to write choices to '" + summaryOutput + "'", ioe);
        		}
        	}
        	
        	
        	ai1.reset();
        	ai2.reset();
        }
        System.out.println(); //adds a trailing \n to the match count written in the loop.
        logger.info("Executed " + numMatches + " matches.");
	}
	
	/**
	 * Writes the weights of the AIs if they're able to save weights
	 * @param players an array with the two players
	 * @param workingDir
	 */
	private static void checkpoint(AI[] players, String workingDir, int matchNumber) {
		
		Logger logger = LogManager.getRootLogger();
		
		if (players.length != 2) {
			logger.error("checkpoint should receive an array with two players instead of {}!", players.length);
		}
		
		for (int p = 0; p < players.length; p++) {
			// retrieves the player, casts and save the weights
			AI player = players[p];
			if(player instanceof SarsaSearch) {
				try {
					((SarsaSearch) player).saveWeights(
						String.format("%s/weights_%d-m%d.bin", workingDir, p, matchNumber)
					);
				} catch (IOException e) {
					logger.error("Unable to save weights for player {}", p, e);
				}
				
			}
		}
	}

	public static void outputChoices(String path, int matchNumber, List<String> choices) throws IOException{

		// creates the choices file and required dirs if necessary
		File f = new File(path);
		FileWriter writer; 
		Logger logger = LogManager.getRootLogger();
		logger.debug("Attempting to write choices to " + path);
		
    	if(!f.exists()){ // creates a new file and writes the header
    		// creates missing parent directories as well
    		if (f.getParentFile() != null) {
    			  f.getParentFile().mkdirs();
			}
    		logger.debug("File didn't exist, creating and writing header");
    		writer = new FileWriter(f, false); //must be after the test, because it creates the file upon instantiation
    		writer.write("#frame: choice\n");
    		writer.close();
    	}
    	
    	// appends one line with each choice separated by a \t\n
    	writer = new FileWriter(f, true); 
    	writer.write("Match " + matchNumber + ": \n");
    	int frame = 0; //counts the frames
    	for(String choice : choices) {
    		writer.write(String.format("\t%d: %s\n", frame++, choice)); 
    	}
    	logger.debug("Successfully wrote to " + path); 
    	
    	writer.close();
	}
    
    public static void outputSummary(String path, int result, long milliseconds, int frames, Date start, Date finish) throws IOException{
    	File f = new File(path);
		FileWriter writer; 
		Logger logger = LogManager.getRootLogger();
		logger.debug("Attempting to write the output summary to " + path);
		
    	if(!f.exists()){ // creates a new file and writes the header
    		// creates missing parent directories as well
    		if (f.getParentFile() != null) {
    			  f.getParentFile().mkdirs();
			}
    		logger.debug("File didn't exist, creating and writing header");
    		writer = new FileWriter(f, false); //must be after the test, because it creates the file upon instantiation
    		writer.write("#result,duration(ms),duration(frames),initial_time,final_time\n");
    		writer.close();
    	}
    	
    	// appends one line with each weight value separated by a comma
    	writer = new FileWriter(f, true); 
    	writer.write(String.format("%d,%d,%d,%s,%s\n", result, milliseconds, frames, start, finish));
    	logger.debug("Successfully wrote to " + path); 
    	
    	writer.close();
	}
    
	
}

