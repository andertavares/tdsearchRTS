package learningeval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import activation.DefaultActivationFunction;
import activation.LogisticLogLoss;
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
	private double[] weights;
	
	
	/**
	 * Number of states to look ahead when doing the rollout
	 */
	private int lookahead;
	
	/**
	 * An instance of {@link RandomBiasedAI} that dictates the actions during the rollout
	 * if no other is specified
	 */
	private RandomBiasedAI randomBiasedPolicy; 
	
	/**
	 * The state feature extractor
	 */
	private FeatureExtractor featureExtractor;
	
	/**
	 * The activation function
	 */
	private DefaultActivationFunction activation;
	
	/**
	 * Flag to activate or deactivate training. 
	 * When active, performs a rollout and learns the value of the reached state.
	 * If inactive, rollouts are not performed and the predicted value for the given state is returned instead. 
	 */
	private boolean isTraining;
	
	/**
	 * Creates the LearningStateEvaluator 
	 * @param alpha the learning rate
	 * @param lookahead how much frames to look ahead when doing rollouts
	 * @param unitTypeTable the game 'rules' regarding its units
	 */
	public LearningStateEvaluator(double alpha, int lookahead, UnitTypeTable unitTypeTable) {
		this.alpha = alpha;
		this.lookahead = lookahead;
		isTraining = true;
		randomBiasedPolicy = new RandomBiasedAI(unitTypeTable);
		
		featureExtractor = new FeatureExtractor(unitTypeTable);
		
		weights = new double[featureExtractor.getNumFeatures()];
		
		//weight initialization
		for(int i = 0; i < weights.length; i++) {
			weights[i] = (Math.random() * 2) - 1 ; //randomly initialized in [-1,1]
		}
		
		// uses logistic with log loss by default
		activation = new LogisticLogLoss();
	}
	
	/**
	 * Saves weights to file
	 * @param path
	 * @throws IOException 
	 */
	public void save(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(weights);
        oos.close();
        fos.close();
	}
	
	public void load(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
        	weights = (double[]) ois.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Error while attempting to load weights.");
			e.printStackTrace();
		}
        ois.close();
        fis.close();
	}
	
	/**
	 * During training, when rollout called, an actual rollout is performed.
	 * The value of the reached state is used to update the predictor and then returned.
	 */
	public void activateTraining() {
		this.isTraining = true;
	}
	
	
	/**
	 * During test, when rollout is called, an acutal rollout is NOT performed.
	 * The predicted value of the given state is returned instead. 
	 * 
	 * The idea is to save time by not rolling out, such that more 
	 * states can be evaluated by the user.
	 */
	public void activateTest() {
		this.isTraining = false;
	}
	
	/**
	 * If training is activated:
	 * Performs the rollout using RandomBiasedAI as the default policy for both players,
	 * uses the actual value of the reached state to update the predictor and returns it.
	 * 
	 * If training is deactivated:
	 * Returns the predicted value of the received state.
	 * 
	 * @param player the current player (the predicted value is w.r.t. his point of view)
	 * @param state the state to evaluate
	 * @return
	 * @throws Exception
	 */
	private double rollout(int player, GameState state) throws Exception {
		return rollout(player, state, randomBiasedPolicy, randomBiasedPolicy);
	}
	
	
	/**
	 * If training is activated:
	 * Performs a rollout using the specified policies for the player and enemy,
	 * uses the actual value of the reached state to update the predictor and returns it.
	 * 
	 * If training is deactivated:
	 * Returns the predicted value of the received state.
	 * 
	 * @param player the player ID (0 or 1)
	 * @param state
	 * @param playerPolicy
	 * @param enemyPolicy
	 * @return
	 * @throws Exception
	 */
	public double rollout(int player, GameState state, AI playerPolicy, AI enemyPolicy) throws Exception {
		//reminder: if I want an RL method to learn in self-play, I just need to plug them into player & enemy policy
		
		// if we're not training, don't rollout: return the predicted value for the given state instead
		if (!isTraining) {
			return evaluate(player, 1 - player, state);
		}
		
		int depthLimit = state.getTime() + lookahead;
		
		GameState reachedState = state.clone(); //preserves the received game state
		
		boolean gameover;
		do {
			// implements the previously issued actions
			gameover = reachedState.cycle();

			// issue the actions for the reached game state with a random biased AI
			reachedState.issueSafe(playerPolicy.getAction(player, reachedState));
			reachedState.issueSafe(enemyPolicy.getAction(1 - player, reachedState));

			// repeats until gameover, depthlimit or we reached a new decision point for the player
		} while (!gameover && reachedState.getTime() < depthLimit && !reachedState.canExecuteAnyAction(player));

		// raw reward value: -1, 0, 1 for defeat, loss and win, respectively
		int reward = 0;
		
		if (gameover) {
			reward = reachedState.winner() == player ? 1 : -1; 
		}
		
		// predicted value for the received (initial) state
		float predictedValue = evaluate(player, 1 - player, state); // cutoffEval.evaluate(player, 1 - player, gs2);
		
		
		// bootstrapped n-step return: TD target is the reward + gamma^n * v(s') -- with gamma equals 1 (undiscounted)
		// on gameover, the value of the terminal state (v(s')) is zero.
		double tdTarget = gameover ? reward : reward + evaluate(player, 1 - player, reachedState);
		
		// scales the actualValue to fit the range of the activation function
		double scaledTarget = activation.scaleTargetValue(tdTarget);
		
		// update our predictor towards the n-step return
		updateWeights(state, player, predictedValue, scaledTarget);

		return scaledTarget;
	}
	
	/**
	 * Performs an update on the weight vector via stochastic gradient descent.
	 * The weights are updated such that the next prediction will be closer to 
	 * the actual value for the given GameState and player 
	 * 
	 * @param state
	 * @param player
	 * @param predicted
	 * @param target
	 */
	private void updateWeights(GameState state, int player, double predicted, double target) {
		double[] features = featureExtractor.extractFeatures(state, player);
		
		// the prediction without the activation function (used in the derivative of the error function)
		double rawPrediction = linearCombination(features, weights);

		//if the error were predicted - actual, then the update rule would be weights -= ... instead of  +=		
		double error = target - predicted;
		double errorDerivative = activation.errorDerivative(rawPrediction);
		
		// finally, the update for each weight
		for(int i = 0; i < weights.length; i++) {
			weights[i] += alpha * error * errorDerivative *  features[i];	
		}
		
	}
	
	/*
	 * Performs an update on the weight vector via gradient descent 
	 * 
	 * @param weights the weight vector
	 * @param features the feature vector
	 * @param predicted the predicted value for the given input in the feature vector
	 * @param actual the true value for the given input in the feature vector
	 * @param stepSize the learning rate
	 *
	private void updateWeights(double[] weights, double[] features, double predicted, double actual, double stepSize) {
		double error = actual - predicted;
		//if the error were predicted - actual, then the update rule would be weights -= ... instead of  +=
		
		
		for(int i = 0; i < weights.length; i++) {
			weights[i] += stepSize * error * activation.errorDerivative(predicted) *  features[i];	
		}
	}*/

	@Override
	/**
	 * Evaluates the received game state from the point of view of the max player.
	 * Make sure you pass your player ID to max and the opponent's to min
	 */
	public float evaluate(int maxplayer, int minplayer, GameState state) {
		
		//extract features from the point of view of the maxplayer
		double[] features = featureExtractor.extractFeatures(state, maxplayer);
	       
		double value = linearCombination(features, weights);
		return (float) activation.function(value);
	       
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

	/**
	 * Linear combination between two vectors (i.e. sum(f[i] * w[i]) for i = 0, ..., length of the vectors
	 * @param features
	 * @param weights
	 * @return
	 */
	private double linearCombination(double[] features, double[] weights) {
		assert features.length == weights.length;
		
		double value = 0;
		for(int i = 0; i < features.length; i++) {
			value += features[i] * weights[i];
		}
		return value;
	}
	

	@Override
	public float upperBound(GameState gs) {
		return 1.0f;
	}

}
