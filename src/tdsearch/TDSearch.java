package tdsearch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activation.DefaultActivationFunction;
import activation.LogisticLogLoss;
import ai.abstraction.HeavyDefense;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightDefense;
import ai.abstraction.LightRush;
import ai.abstraction.RangedDefense;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerDefense;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import learningeval.FeatureExtractor;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;

public class TDSearch extends AI {
	
	/**
	 * The learning rate for weight update
	 */
	private double alpha;
	
	/**
     * Probability of exploration
     */
    private double epsilon;
	
	/**
	 * The eligibility trace for weight update
	 */
	private double lambda;
	
	/**
	 * The discount factor of future rewards
	 */
	private double gamma;
	
	/**
	 * Time budget to return an action
	 */
	private int timeBudget;
	
	/**
	 * Weight vector for state-value predictor
	 */
	private double[] weights;
	
	/**
	 * The random number generator
	 */
	private Random random;
	
	/**
	 * The state feature extractor
	 */
	private FeatureExtractor featureExtractor;
	
	/**
	 * This maps the AI name to its instance.
     * Each AI filters out the possible actions to consider at each state.
     * Thus they're called the action abstractions.
     */
    private Map<String,AI> abstractions;
	
	/**
	 * The activation function
	 */
	private DefaultActivationFunction activation;

	private Logger logger;
	
	/**
	 * Initializes TDSearch with default parameters, except for the UnitTypeTable
	 * @param types
	 */
	public TDSearch(UnitTypeTable types) {
		this(types, 100, 0.01, 1, 0.1);
		//this(types, 50, 0.01, 1, 0.1); //for testing purposes
	}
	
	/**
	 * Initializes TDSearch with the given parameters 
	 * @param types the rules defining unit types
	 * @param alpha learning rate
	 * @param gamma the discount factor for future rewards
	 * @param lambda eligibility trace
	 */
	public TDSearch(UnitTypeTable types, int timeBudget, double alpha, double gamma, double lambda) {
		this.timeBudget = timeBudget;
		this.alpha = alpha;
		this.gamma = gamma;
		this.lambda = lambda;
		random = new Random();
		
		featureExtractor = new FeatureExtractor(types);
		
		weights = new double[featureExtractor.getNumFeatures()];
		
		//weight initialization
		for(int i = 0; i < weights.length; i++) {
			weights[i] = (Math.random() * 2) - 1 ; //randomly initialized in [-1,1]
		}
		
		// uses logistic with log loss by default
		activation = new LogisticLogLoss();
		
    	logger = LogManager.getRootLogger();
    	
        //loads the portfolio according to the file specification
        abstractions = new HashMap<>();
        
        // rush scripts
		abstractions.put("WorkerRush", new WorkerRush (types));
		abstractions.put("LightRush", new LightRush (types));
		abstractions.put("RangedRush", new RangedRush (types));
		abstractions.put("HeavyRush", new HeavyRush (types));
		
		// defensive scripts
		abstractions.put("WorkerDefense", new WorkerDefense(types));
		abstractions.put("LightRush", new LightDefense(types));
		abstractions.put("RangedRush", new RangedDefense(types));
		abstractions.put("HeavyRush", new HeavyDefense(types));
	}

	@Override
	public void reset() {
		// does nothing
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		
		Date begin = new Date(System.currentTimeMillis());
		Date end;
    	int planningBudget =  (int) (.8 * timeBudget); //80% of budget to planning
    	long duration = 0;
    	
		while (duration < planningBudget) { // while time available
			// the vector of eligibility traces is initialized to zero (thanks, java)
			double[] eligibility = new double[weights.length];
			
			GameState state = gs.clone();
			PlayerAction action = epsilonGreedy(state, player);
			
			while (!state.gameover() && duration < planningBudget) { // go until game over or time is out
				
				// the features of this state
				double[] features = featureExtractor.extractFeatures(state, player);
				
				// issue the action to obtain the next state, issues a self-play move for the opponent
				GameState nextState = state.clone();
				logger.debug("Issuing action " + action);
				nextState.issueSafe(action);
				nextState.issueSafe(epsilonGreedy(gs, 1 - player));
				nextState.cycle();
				
				double tdError = tdTarget(nextState, player) - linearCombination(features,  weights);
				
				// weight vector update
				for(int i = 0; i < weights.length; i++) {
					weights[i] += alpha * tdError * eligibility[i];	
				}
				
				// eligibility vector update
				for(int i = 0; i < eligibility.length; i++) {
					eligibility[i] = eligibility[i] * lambda + features[i];
				}
				
				state = nextState; 
				action = epsilonGreedy(state, player);
				
				// updates duration 
				end = new Date(System.currentTimeMillis());
				duration = end.getTime() - begin.getTime();
			}
			
		} //end while (timeAvailable)
		
		return greedyAction(player, gs);
	}

	/**
	 * Returns the best action for the player in the given state, 
	 * under the current action abstraction scheme
	 * @param player
	 * @param gs
	 * @return
	 * @throws Exception
	 */
	private PlayerAction greedyAction(int player, GameState gs)  {
		PlayerAction bestAction = null; double bestActionValue = Double.NEGATIVE_INFINITY;
		
		// begin: argmax
		for(AI abstraction : abstractions.values()) {
			PlayerAction candidate;
			
			try {
				candidate = abstraction.getAction(player, gs);
			} catch (Exception e) {
				logger.error("Error while getting action from " + abstraction.getClass().getName(), e);
				candidate = new PlayerAction();
				candidate.fillWithNones(gs, player, 1);
			}
			GameState nextState = gs.clone();
			
			nextState.issueSafe(candidate);
			
			// creates a dummy action for the opponent
			/*PlayerAction dummy = new PlayerAction();
			dummy.fillWithNones(gs, 1-player, 1);
			nextState.issueSafe(dummy);*/
			nextState.cycle();
			
			double candidateValue = linearCombination(featureExtractor.extractFeatures(nextState, player), weights);
			
			if (Double.isNaN(candidateValue)) {
				logger.error("Error: candidateValue is NaN for state: " + nextState + ". Dumping it to errorState.xml.");
				try {
					XMLWriter dumper = new XMLWriter(new FileWriter("errorState.xml"));
					nextState.toxml(dumper);
					dumper.close();
				} catch (IOException e) {
					logger.error("Error while dumping state", e);
				}
				logger.error("Dumping the weight vector: " + weights);
			}
			
			//logger.info(String.format("candidateValue: %f, bestActionValue: %f", candidateValue, bestActionValue));
			
			if (candidateValue > bestActionValue) {
				bestActionValue = candidateValue;
				bestAction = candidate;
			}
		} // end: argmax
		if (bestAction == null) {
			logger.error("Error: bestAction is null! abstractions.values(): "+ abstractions.values());
		}
		return bestAction;
	}
	
	/**
	 * The temporal-difference target is, by definition, r + gamma * v(s'), where s' is the reached state.
	 * Here, we adopt no intermediate rewards. 
	 * If the game is over and the player won, r is 1 and v(s') is 0.
	 * If the game is over and the player lost or draw, r is 0 and v(s') is 0.
	 * If the game is not over, r is 0 and v(s') is the predicted value given by the function approximator
	 * 
	 * TODO at gameover, it might be interesting to break ties with in-game score rather than give zero reward
	 * @param reachedState
	 * @param player
	 * @return
	 */
	private double tdTarget(GameState reachedState, int player) {
		double reward, reachedStateValue;
		
		if (reachedState.gameover()) { 
			reachedStateValue = 0;
			reward = reachedState.winner() != player ? 1 : 0;
		}
		else {
			reward = 0;
			reachedStateValue = linearCombination(featureExtractor.extractFeatures(reachedState, player), weights);
		}
		return reward + gamma * reachedStateValue;
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

	private PlayerAction epsilonGreedy(GameState gs, int player) {

		if(random.nextFloat() < epsilon){ // random choice with probability epsilon
			logger.debug("epsilon action");
			
        	//trick to randomly select from HashMap adapted from: https://stackoverflow.com/a/9919827/1251716
        	List<String> keys = new ArrayList<String>(abstractions.keySet());
        	String choiceName = keys.get(random.nextInt(keys.size()));
        	
        	if(choiceName==null){
        		logger.error("ERROR while retrieving epsilon action!");
    		}
        	
        	try {
				return abstractions.get(choiceName).getAction(player, gs);
			} catch (Exception e) {
				logger.error("Error while returning epsilon action!", e);
				PlayerAction dummy = new PlayerAction();
				dummy.fillWithNones(gs, player, 1);
				return dummy;
			}
        }
        else { // greedy choice with probability 1 - epsilon
        	return greedyAction(player, gs);
        }
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		return null;
	}

}
