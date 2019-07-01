package main;

import ai.abstraction.LightRush;
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
import java.util.Date;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.VictoryOnly;
import reward.WinLossDraw;
import reward.WinLossTiesBroken;
import rts.GameSettings;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import tdsearch.CompetitionSarsaSearch;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;
import utils.AILoader;

public class TrainCompetition {
	
	public static void main(String[] args) throws Exception {
            Logger logger = LogManager.getRootLogger();
		 		
            // creates a UnitTypeTable that should be overwritten by the one in config
            UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_ALTERNATING);
            GameState state = new GameState(PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), types);

            // creates the player instance
            TDSearch player = new CompetitionSarsaSearch(types);

            // training matches
            logger.info("Starting training...");
            player.preGameAnalysis(state, 1000);
            logger.info("Training finished!");       
            
            //test time: resets the player
            player = new CompetitionSarsaSearch(types);
            AI opponent = new LightRush(types);
            
            GameState initialState = state.clone();
            
            for(int match = 0; match < 10; match++) {
 
                state = initialState.clone();
        
                boolean gameover = false;

                while (!gameover && state.getTime() < 3000) {

                    // initializes state equally for the players 
                    GameState player1State = state; 
                    GameState player2State = state; 

                    // retrieves the players' actions
                    PlayerAction player1Action = player.getAction(0, player1State);
                    PlayerAction player2Action = opponent.getAction(1, player2State);

                    // issues the players' actions
                    state.issueSafe(player1Action);
                    state.issueSafe(player2Action);

                    // runs one cycle of the game
                    gameover = state.cycle();
                    System.out.print(String.format("\rframe %8d", state.getTime()));
                } //end of the match
        
		System.out.print(String.format("\rMatch %8d finished with result %3d.", match, state.winner()));
            }
	}

}
        
