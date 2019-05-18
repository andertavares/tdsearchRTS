package tdsearch;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import config.ConfigManager;
import learningeval.FeatureExtractor;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class TDSearch extends AI {
	
	/**
	 * The learning rate for weight update
	 */
	protected double alpha;
	
	/**
     * Probability of exploration
     */
	protected double epsilon;
	
	/**
	 * The eligibility trace for weight update
	 */
    protected double lambda;
	
	/**
	 * The discount factor of future rewards
	 */
	protected double gamma;
	
	/**
	 * Time budget to return an action
	 */
	protected int timeBudget;
	
	/**
	 * Weight vector for state-value predictor
	 */
	private double[] weights;
	
	/**
	 * The random number generator
	 */
	protected Random random;
	
	/**
	 * The state feature extractor
	 */
	protected FeatureExtractor featureExtractor;
	
	/**
	 * This maps the AI name to its instance.
     * Each AI filters out the possible actions to consider at each state.
     * Thus they're called the action abstractions.
     */
    protected Map<String,AI> abstractions;
	
	protected Logger logger;
	
	/**
	 * Initializes TDSearch with default parameters, except for the UnitTypeTable
	 * @param types
	 */
	public TDSearch(UnitTypeTable types) {
		this(types, 100, 0.01, 0.1, 1, 0.1, 0);
	}
	
	/**
     * Returns a TDSearch with parameters specified in a file
     * @param types
     * @param configPath
     */
    public static TDSearch fromConfigFile(UnitTypeTable types, String configPath){
    	
    	Logger logger = LogManager.getLogger();
    	Properties config = null;
    	
        // loads the configuration
		try {
			config = ConfigManager.loadConfig(configPath);
		} catch (IOException e) {
			logger.error("Error while loading configuration from '" + configPath+ "'. Using defaults.", e);
		}
        
		int timeBudget = Integer.parseInt(config.getProperty("search.timebudget", "100"));
		
		int randomSeed = Integer.parseInt(config.getProperty("random.seed", "0"));
        
        double epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial", "0.1"));
        //epsilonDecayRate = Double.parseDouble(config.getProperty("td.epsilon.decay", "1.0"));
        
        double alpha = Double.parseDouble(config.getProperty("td.alpha.initial", "0.01"));
        //alphaDecayRate = Double.parseDouble(config.getProperty("td.alpha.decay", "1.0"));
        
        double gamma = Double.parseDouble(config.getProperty("td.gamma", "1.0"));
        
        double lambda = Double.parseDouble(config.getProperty("td.lambda", "0.0"));
        
        TDSearch newInstance = new TDSearch(types, timeBudget, alpha, epsilon, gamma, lambda, randomSeed);
        
        if (config.containsKey("td.input.weights")){
        	try {
				newInstance.loadWeights(config.getProperty("td.input.weights"));
			} catch (IOException e) {
				logger.error("Error while loading weights from " + config.getProperty("td.input.weights"), e);
				logger.error("Weights initialized randomly.");
				e.printStackTrace();
			}
        }
        
        return newInstance;
    }
    
	
	/**
	 * Initializes TDSearch with the given parameters 
	 * @param types the rules defining unit types
	 * @param alpha learning rate
	 * @param epsilon exploration probability
	 * @param gamma the discount factor for future rewards
	 * @param lambda eligibility trace
	 * @param randomSeed 
	 */
	public TDSearch(UnitTypeTable types, int timeBudget, double alpha, double epsilon, double gamma, double lambda, int randomSeed) {
		this.timeBudget = timeBudget;
		this.alpha = alpha;
		this.epsilon = epsilon;
		this.gamma = gamma;
		this.lambda = lambda;
		random = new Random(randomSeed);
		
		featureExtractor = new FeatureExtractor(types);
		
		weights = new double[featureExtractor.getNumFeatures()];
		
		//weight initialization
		for(int i = 0; i < weights.length; i++) {
			weights[i] = (random.nextDouble() * 2) - 1 ; //randomly initialized in [-1,1]
		}
		
		// uses logistic with log loss by default
		//activation = new LogisticLogLoss();
		
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
	
	
	/**
	 * Prepares for test matches: disables exploration and learning
	 */
	public void prepareForTest() {
		alpha = 0;
		epsilon = 0;
	}
	
	/**
	 * Saves weights to a binary file
	 * @param path
	 * @throws IOException 
	 */
	public void saveWeights(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(weights);
        oos.close();
        fos.close();
	}
	
	/**
	 * Load weights from a binary file
	 * @param path
	 * @throws IOException
	 */
	public void loadWeights(String path) throws IOException {
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
				nextState.issueSafe(epsilonGreedy(state, 1 - player));
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
			if(!candidate.integrityCheck()) {
				logger.error("Integrity check for an action failed!");
				logger.error("Current abstraction: " + abstraction + ", current state " + gs);
				logger.error("Dumping state to inconsistentAction.xml");
				gs.toxml("inconsistentAction.xml");
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
				logger.error("Error: candidateValue is NaN for state: " + nextState + ". Dumping it to nanCandidate.xml.");
				nextState.toxml("nanCandidate.xml");
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
			reward = reachedState.winner() == player ? 1 : 0;
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
	protected double linearCombination(double[] features, double[] weights) {
		assert features.length == weights.length;
		
		double value = 0;
		for(int i = 0; i < features.length; i++) {
			value += features[i] * weights[i];
		}
		return value;
	}

	private PlayerAction epsilonGreedy(GameState gs, int player) {

		if(random.nextDouble() < epsilon){ // random choice with probability epsilon
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
