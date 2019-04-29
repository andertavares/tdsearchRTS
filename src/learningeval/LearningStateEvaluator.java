package learningeval;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.units.UnitTypeTable;

public class LearningStateEvaluator {
	
	/**
	 * The learning rate for weight update
	 */
	private double alpha;
	
	/**
	 * Weight vector for state-value predictor
	 */
	private double stateWeights[];
	
	/**
	 * Weight vector for the error predictor
	 */
	private double errorWeights[];
	
	/**
	 * Number of states to look ahead when doing the rollout
	 */
	private int lookahead;
	
	/**
	 * AI that dictates the actions during the rollout
	 */
	private AI defaultPolicy; 
	
	/**
	 * Simple evaluation function to be activated when rollout reaches the 
	 * lookahead limit
	 */
	EvaluationFunction cutoffEval;
	
	public LearningStateEvaluator(double alpha, int lookahead, UnitTypeTable unitTypeTable) {
		this.alpha = alpha;
		this.lookahead = lookahead;
		cutoffEval = new SimpleSqrtEvaluationFunction3();
		defaultPolicy = new RandomBiasedAI(unitTypeTable);
		
		//stateWeights = new double[featureExtractor.featureCount()];
		//errorWeights = new double[featureExtractor.featureCount()];
	}
	
	/**
    * 
    * @param player index of the player
    * @param gs state to evaluate
    * @param uScriptPlayer the script assignment for the player's units
    * @param aiEnemy AI to issue the first enemy actions
    * @return a value between [-1, 1], indicating the normalized material advantage of the state. See {@link SimpleSqrtEvaluationFunction3#evaluate}
    * @throws Exception
    */
   public double eval(int player, GameState gs) throws Exception {
       GameState gs2 = gs.clone();
       
       //calculate predicted error
       
       /*if i'm confident enough:
        	return stateRegression 
        else:
        	value = rollout(player, gs2);
        	predictedStateValue = regression
        	updateStateValue(phi(s), predictedSTateValue);
        	updateError(phi(s), Math.pow(predictedStateValue - value, 2));
        	return value;
        */

       return 0;
   }

	private double rollout(int player, GameState gs2) throws Exception {
		int depthLimit = gs2.getTime() + lookahead;
		   boolean gameover;
		   do {
		       // implements the previously issued actions
		       gameover = gs2.cycle();

		       // issue the actions for the reached game state with a random biased AI
		       gs2.issueSafe(defaultPolicy.getAction(player, gs2));
		       gs2.issueSafe(defaultPolicy.getAction(1 - player, gs2));

		       // repeats until gameover, depthlimit or we reached a new decision point for the player
		   } while (!gameover && gs2.getTime() < depthLimit && !gs2.canExecuteAnyAction(player) );

		   return cutoffEval.evaluate(player, 1 - player, gs2);
	}

}
