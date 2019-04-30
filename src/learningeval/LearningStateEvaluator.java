package learningeval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.units.UnitTypeTable;

/**
 * Learns to evaluate game states.
 * 
 * @author artavares
 */
public class LearningStateEvaluator extends EvaluationFunction {
	
	/**
	 * The learning rate for weight update
	 */
	private double alpha;
	
	/**
	 * Weight vector for state-value predictor
	 */
	private double[] stateWeights;
	
	/**
	 * Weight vector for the error predictor
	 */
	private double[] errorWeights;
	
	/**
	 * Number of states to look ahead when doing the rollout
	 */
	private int lookahead;
	
	/**
	 * AI that dictates the actions during the rollout
	 */
	private AI defaultPolicy; 
	
	/**
	 * The state feature extractor
	 */
	private FeatureExtractor featureExtractor;
	
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
		
		featureExtractor = new FeatureExtractor(unitTypeTable);
		
		stateWeights = new double[featureExtractor.getNumFeatures()];
		errorWeights = new double[featureExtractor.getNumFeatures()];
		
		//weight initialization
		for(int i = 0; i < stateWeights.length; i++) {
			stateWeights[i] = (Math.random() * 2) - 1 ; //randomly initialized in [-1,1]
		}
	}
	
	/**
	 * Saves weights to file
	 * @param path
	 * @throws IOException 
	 */
	public void save(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(stateWeights);
        oos.close();
        fos.close();
	}
	
	public void load(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
        	stateWeights = (double[]) ois.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Error while attempting to load weights.");
			e.printStackTrace();
		}
        ois.close();
        fis.close();
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
	
	/**
	 * Performs an update on the weight vector via gradient descent 
	 * 
	 * @param weights
	 * @param features
	 * @param error
	 * @param stepSize
	 */
	private static void updateWeights(double[] weights, double[] features, double error, double stepSize) {
		for(int i = 0; i < weights.length; i++) {
			weights[i] += stepSize * (error * features[i]);
		}
	}

	@Override
	public float evaluate(int maxplayer, int minplayer, GameState state) {
		
		//extract features from the point of view of the maxplayer
		double[] features = featureExtractor.extractFeatures(state, maxplayer);
	       
		assert features.length == stateWeights.length;
	       
		//linear combination
		double value = 0;
		for(int i = 0; i < features.length; i++) {
			value += features[i] * stateWeights[i];
		}
		return (float) value;
	       
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
		//return 0;
	}

	@Override
	public float upperBound(GameState gs) {
		return 1;
	}

}
