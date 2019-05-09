package main;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import learningeval.LearningStateEvaluator;
import rts.GameSettings;
import rts.units.UnitTypeTable;

public class Training {
	
	public static void main(String[] args) {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED);
		
		//train with PGS -- original lookahead is 100, we'll divide 50 to PGS  and 50 to the learning eval
		LearningStateEvaluator learningEval = new LearningStateEvaluator(0.1, 50, types);
		learningEval.activateTraining();
		
		AI pgsTrainer = new PGSAI(100, -1, 50, 1, 1, learningEval, types, new AStarPathFinding()); 
		
		// loads the two forms of configuration object
		//GameSettings settings = new GameSettings();
		//System.out.println(settings);
	}

}
