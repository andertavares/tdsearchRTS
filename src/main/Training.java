package main;

import ai.RandomBiasedAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import learningeval.LearningStateEvaluator;
import rts.GameSettings;
import rts.GameSettings.LaunchMode;
import rts.units.UnitTypeTable;

public class Training {
	
	public static void main(String[] args) throws Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED);
		
		//train with PGS -- original lookahead is 100, we'll divide 1 to PGS  and 99 to the learning eval
		LearningStateEvaluator learningEval = new LearningStateEvaluator(0.1, 99, types);
		
		
		// creates one AI to train the regression and an opponent
		AI player = new PGSAI(100, -1, 1, 1, 1, learningEval, types, new AStarPathFinding()); 
		/*AI player = new NaiveMCTS( //third parameter is lookahead - set to one
			100,-1,1,10,0.3f, 0.0f, 0.4f,new RandomBiasedAI(),
			learningEval, true
        );*/
		//AI opponent = new PGSAI(100, -1, 100, 1, 1, new SimpleSqrtEvaluationFunction3(), types, new AStarPathFinding());
		AI opponent = new NaiveMCTS(types);
		//AI pgsDefault = new LightRush(types);
		
		// loads the two forms of configuration object
		GameSettings settings = new GameSettings(
			LaunchMode.STANDALONE, null, 0, 2, 
			"maps/8x8/basesWorkers8x8.xml", 3000, false, 
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH, 
			null, null
		);
		
		// training matches
		learningEval.activateTraining();
		Runner.repeatedHeadlessMatches(100, "results/pgs-train.csv", player, opponent, settings, null);
    	
		// test matches
		learningEval.activateTest();
		Runner.repeatedHeadlessMatches(10, "results/pgs-test.csv", player, opponent, settings, "trace/pgs-test.xml");
	}

}
