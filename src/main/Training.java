package main;

import ai.RandomBiasedAI;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import learningeval.LearningStateEvaluator;
import rts.GameSettings;
import rts.GameSettings.LaunchMode;
import rts.units.UnitTypeTable;
import tdsearch.SarsaSearch;
import tdsearch.TDSearch;

public class Training {
	
	public static void main(String[] args) throws Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED);
		
		//LearningStateEvaluator learningEval = new LearningStateEvaluator(0.1, 99, types);
		
		//AI player = new TDSearch(types); 
		AI player = new SarsaSearch(types, 100, 0.01, 0.1, 1, 0.1, 0);
		//AI opponent = new PGSAI(100, -1, 100, 1, 1, new SimpleSqrtEvaluationFunction3(), types, new AStarPathFinding());
		AI opponent = new SarsaSearch(types, 100, 0.01, 0.1, 1, 0.1, 0);
		//AI pgsDefault = new LightRush(types);
		
		// programatically creates the setting
		GameSettings settings = new GameSettings(
			LaunchMode.STANDALONE, null, 0, 2, 
			"maps/8x8/basesWorkers8x8.xml", 3000, false, 
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH, 
			null, null
		);
		
		// training matches
		//learningEval.activateTraining();
		Runner.repeatedHeadlessMatches(100, "results/pgs-train.csv", player, opponent, settings, null);
    	
		// test matches
		//learningEval.activateTest();
		Runner.repeatedHeadlessMatches(10, "results/pgs-test.csv", player, opponent, settings, "trace/pgs-test.xml");
	}

}
