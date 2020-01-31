package learning;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import reward.RewardModel;
import reward.RewardModelFactory;
import rts.GameState;
import rts.units.UnitTypeTable;
import utils.MathHelper;

public class LinearSarsaLambda implements LearningAgent {
	
	/**
	 * The weights are per action
	 */
	private Map<String, double[]> weights;
	
	/**
	 * The vectors of eligibility traces (one per action)
	 */
	private Map<String, double[]> eligibility;
	
	/**
	 * A list of possible actions
	 */
	List<String> actions;

   /**
    * Previous and current action
    */
	private String previousAction, nextAction;
   
   
   /**
    * Previous and current game state
    */
	private GameState previousState, nextState;
	
	/**
	 * The player ID is part of the state
	 * TODO encapsulate GameState+playerID into a single State object
	 */
	int playerID;
   
   /**
    * The learning rate for weight update
    */
	protected double alpha;

   /**
    * Probability of exploration
    */
	protected double epsilon;

   /**
    * The decay factor of eligibility traces
    */
    protected double lambda;

   /**
    * The discount factor of future rewards
    */
    protected double gamma;

   /**
    * The random number generator
    */
    protected Random random;

   /**
    * The state feature extractor
    */
    protected FeatureExtractor featureExtractor;
	
    /**
     * The reward model used by the agent
     */
    protected RewardModel rewards;
    
    protected Logger logger;
    
    /**
     * Private empty constructor, used for cloning
     */
    private LinearSarsaLambda() {};
    
    /**
     * Returns a LinearSarsaLambda object configured with alpha, epsilon and
     * lambda from planning_* configuration parameters
     * @param types
     * @param config
     * @return
     */
    public static LinearSarsaLambda newPlanningAgent(UnitTypeTable types, Properties config) {
    	LinearSarsaLambda planningAgent = new LinearSarsaLambda(types, config);
    	planningAgent.alpha = Double.parseDouble(config.getProperty("planning_alpha"));
    	planningAgent.epsilon = Double.parseDouble(config.getProperty("planning_epsilon"));
    	planningAgent.lambda = Double.parseDouble(config.getProperty("planning_lambda"));
    	
    	return planningAgent;
    }
    
    /**
     * Creates a LinearSarsaLambda agent with all parameters specified via config file.
     * Random seed defaults to 0 if not specified in config.
     * @param types
     * @param config
     */
    public LinearSarsaLambda(UnitTypeTable types, Properties config) {
 	   
    	int maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
 		
        rewards = RewardModelFactory.getRewardModel(
 		   config.getProperty("rewards"), maxCycles
        );
        
        featureExtractor = FeatureExtractorFactory.getFeatureExtractor(
 		   config.getProperty("features"), types, maxCycles
 	    );
        
        actions = Arrays.asList(config.getProperty("portfolio").split(","));
        
        alpha = Double.parseDouble(config.getProperty("td.alpha.initial")); 
        epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial"));
        gamma = Double.parseDouble(config.getProperty("td.gamma")); 
 		lambda = Double.parseDouble(config.getProperty("td.lambda"));
 		
 		random = new Random(Integer.parseInt(config.getProperty("random_seed", "0")));
 		
 		initialize();
	}
    
    /**
     * Creates a LinearSarsaLambda agent with all parameters specified via config file, except the random seed
     * @param types
     * @param config
     * @param randomSeed
     */
    public LinearSarsaLambda(UnitTypeTable types, Properties config, int randomSeed) {
  	   
    	this(types, config);
 		
 		random = new Random(randomSeed);
 		
 		initialize();
	}
    
    /**
     * Creates a LinearSarsaLambda by explicitly specifying all parameters
     * @param types
     * @param rewardModel
     * @param featureExtractor
     * @param actions
     * @param alpha
     * @param epsilon
     * @param gamma
     * @param lambda
     * @param randomSeed
     */
    public LinearSarsaLambda(UnitTypeTable types, RewardModel rewardModel,
			FeatureExtractor featureExtractor, List<String> actions, double alpha, double epsilon, 
			double gamma, double lambda, int randomSeed) {
    	
    	this.rewards = rewardModel;
        this.featureExtractor = featureExtractor;
        this.actions = actions; 
        this.alpha = alpha; 
        this.epsilon = epsilon;
        this.gamma = gamma; 
        this.lambda = lambda;
 		
 		random = new Random(randomSeed);
 		
 		initialize();
    	
	}

    /**
     * Returns a clone of this object, except for the reset eligiblities and 
     * random number generator (which is overkill to clone: https://stackoverflow.com/a/54156572/1251716)
     */
    public LinearSarsaLambda cloneExceptEligibility() {
    	LinearSarsaLambda copy = new LinearSarsaLambda();
    	
    	copy.rewards = this.rewards;
    	copy.featureExtractor = this.featureExtractor;
    	copy.actions = this.actions; 
    	copy.alpha = this.alpha; 
    	copy.epsilon = this.epsilon;
    	copy.gamma = this.gamma; 
    	copy.lambda = this.lambda;
    	copy.random = new Random();
    	copy.initialize();
    	
    	copy.copyWeights(this.getWeights());
 		
 		return copy;
    }
    
    /**
     * Returns another LinearSarsaLambda object with the same parameters, but with new
     * weights, eligibility & random number generator
     * (which is overkill to clone: https://stackoverflow.com/a/54156572/1251716)
     */
    public LinearSarsaLambda cloneExceptWeightsAndEligibility() {
    	LinearSarsaLambda copy = new LinearSarsaLambda();
    	
    	copy.rewards = this.rewards;
    	copy.featureExtractor = this.featureExtractor;
    	copy.actions = this.actions; 
    	copy.alpha = this.alpha; 
    	copy.epsilon = this.epsilon;
    	copy.gamma = this.gamma; 
    	copy.lambda = this.lambda;
    	copy.random = new Random();
    	copy.initialize();
 		
 		return copy;
    }
    
	/**
     * Initializes internal variables (logger, weights, eligibility traces as well as
     * current and previous state and action)
     */
    private void initialize() {
    	logger = LogManager.getRootLogger();
    	nextAction = previousAction = null;
        previousState = null;
    	
        weights = new HashMap<>();
        eligibility = new HashMap<>();
        
        playerID = -1;	//initializes with a 'flag' value so that it is updated in the first call to 'act' 

        for (String action : actions) {

            eligibility.put(action, new double[featureExtractor.getNumFeatures()]);

            // initializes weights randomly within [-1, 1]
            double[] actionWeights = new double[featureExtractor.getNumFeatures()];
            for (int i = 0; i < actionWeights.length; i++) {
                    actionWeights[i] = (random.nextDouble() * 2) - 1; // randomly initialized in [-1,1]
            }
            weights.put(action, actionWeights);
        }
    }
    
    /**
     * Resets the eligibility traces
     */
    public void clearEligibility() {
    	for (String action : actions) {
            eligibility.put(action, new double[featureExtractor.getNumFeatures()]);	//TODO possible memory leak?
    	}
    }
    
    

	@Override
	public String act(GameState state, int player) {
		
		// sets my player ID on the first call
		if(playerID == -1) {
			playerID = player;
		}
		
		// updates the previous and current states, as well as previous and current actions
        previousState = nextState;
        previousAction = nextAction;
        nextAction = epsilonGreedy(state, player);
        nextState = state.clone();
        
        
        // gets the reward for this state
        double reward = rewards.reward(state, player);
        
        if(previousState != null && previousAction != null) {
        	// performs a sarsa update on the current experience tuple
            sarsaUpdate(previousState, player, previousAction, reward, nextState, nextAction, state.gameover());
        }
        
        // returns the next action 
		return nextAction;
	}
	
	@Override
	public void finish(int winner) {
		
		// updates the value of the last taken state-action pair
		double finalReward = rewards.gameOverReward(playerID, winner);
		learn(nextState, playerID, nextAction, finalReward, null, true);
		
		// resets the variables
		previousState = nextState = null;
		previousAction = nextAction = null;
		
		//double tdError = finalReward - qValue(previousState, playerID, previousAction);
		//tdLambdaUpdateRule(previousState, playerID, previousAction, tdError, weights, eligibility);
	}


	
	
	@Override
	/**
	 * Performs an update with an s,a,r,s' experience tuple. 
	 * If a' was not already chosen for s', it will be chosen here if s' is not terminal
	 */
	public void learn(GameState state, int player, String action, double reward, GameState nextState, boolean done) {
		
		if(nextState != null &&  !nextState.equals(this.nextState) && !done) {
			nextAction = epsilonGreedy(nextState, player);
			logger.debug("Next action changed to {}", nextAction);
		}
		
		sarsaUpdate(state, player, action, reward, nextState, nextAction, done);
	}
	
	/**
	 * Applies the Sarsa update rule to an experience tuple: s,a,r,s',a'
	 * @param state
	 * @param player
	 * @param action
	 * @param reward
	 * @param nextState
	 * @param nextAction
	 * @param done
	 */
	public void sarsaUpdate(GameState state, int player, String action, double reward, GameState nextState, String nextAction, boolean done) {
		this.nextAction = nextAction; //on the next step, I must perform this action (on policy)
		
		logger.trace(
			"Player {}: <s,a,r,s'(gameover?),a',q(s',a')> = <{}, {}, {}, {}({}), {}, {}>",
			player,
			state.getTime(), action, 
			reward, 
			nextState == null ? "null" : nextState.getTime(), 
			done, nextAction,
			done ? 0 : qValue(nextState, player, nextAction)
		);
		
		if(done) {
		logger.debug(
				"Player {}: <s,a,r,s'(gameover?),a',q(s',a')> = <{}, {}, {}, {}({}), {}, {}>",
				player,
				state.getTime(), action, 
				reward, 
				nextState == null ? "null" : nextState.getTime(), 
				done, nextAction,
				done ? 0 : qValue(nextState, player, nextAction)
			);
		}
		
		//delta = r + gamma * Q(s',a') - Q(s,a)
		double tdError = tdTarget(reward, nextState, player, nextAction, done) - qValue(state, player, action);

		if(done) {
			//logger.debug("weights before: {}", weights.get(action));
		}
		tdLambdaUpdateRule(state, player, action, tdError);
		if(done) {
			//logger.debug("weights after: {}", weights.get(action));
		}
	}
	
	/**
	 * The temporal-difference target is, by definition, r + gamma * q(s', a'),
	 * where s' is the reached state and a' is the action to be performed there.
	 * 
	 * (originally, the method was private and is public for unit testing;
	 * this is a code smell: perhaps a new class should be written with this functionality)
	 * 
	 * @param reward
	 * @param nextState
	 * @param player
	 * @param nextActionName
	 * @param done whether the nextState is terminal
	 * @return
	 */
	public double tdTarget(double reward, GameState nextState, int player, String nextActionName, boolean done) {
		double nextQ;
		
		// terminal states have value of zero
		if (done) {
			nextQ = 0;
			logger.trace("Reward for terminal state for player {}: {}. ", player, reward
			);
		} else {
			nextQ = qValue(nextState, player, nextActionName);
			logger.trace(
				"Reward for time {} for player {}: {}. q(s',a')={}. Done? {}", 
				nextState.getTime(), player, reward, nextQ, done
			);
		}
		
		return reward + gamma * nextQ;
	}
	
	
	/**
	 * Performs the TD(lambda) update rule on the weight vector.
	 * @param state
	 * @param player
	 * @param action
	 * @param tdError
	 */
	public void tdLambdaUpdateRule(GameState state, int player, String action, double tdError) {
		
		double[] f = featureExtractor.extractFeatures(state, player); // feature vector for the state
		
		// incrementes the eligibility of the selected action by adding the feature vector
		double[] eSelected = eligibility.get(action);
		for (int i = 0; i < eSelected.length; i++) {
			eSelected[i] += f[i];
		}
		
		// updates the weights of all actions and decays their eligibilities
		for (String actionName : weights.keySet()) {
			
			double[] w = weights.get(actionName); // weight vector
			double[] e = eligibility.get(actionName); // eligibility vector 
			
			// certifies that things are ok
			assert w.length == e.length;
			assert e.length == f.length;
			
			// vector updates 
			for (int i = 0; i < w.length; i++) {
				w[i] = w[i] + alpha * tdError * e[i]; // weight vector update (w = w+alpha*delta*e)
				e[i] = e[i] * gamma * lambda; //the eligibility of all actions decays by gamma * lambda
			}
		}
		
		/*
		 * Remark: in Silver et al (2013) TD search, the eligibility vector update is done as 
		 * e = e * lambda + f(s,a), where f(s,a) are the features for state s and action a.
		 * This is so because features are per state and action. 
		 * Moreover, they use gamma=1 always so that it does not appear in the equation.
		 * That is, the general form of the equation should be e = e * gamma * lambda + f(s,a)
		 *  
		 * Here, gamma can have different values and we can interpret that f(s,a) = zeros 
		 * for the non-selected action.
		 * Thus, we decay the eligibility vectors of all actions and then 
		 * increase the eligibility vector of the selected action by adding the current state features.
		 * In other words, we  implement equation e = e * gamma * lambda + f(s,a) in two steps.
		 */
		
	}

	/**
	 * Returns the weight vector
	 * @return
	 */
	public Map<String, double[]> getWeights(){
		return weights;
	}
	
	/**
	 * Replaces the current weights by the one specified here
	 * @param weights
	 */
	public void setWeights(Map<String, double[]> weights) {
		this.weights = weights;
	}

	@Override
	public double qValue(GameState state, int player, String actionName) {
		return qValue(featureExtractor.extractFeatures(state, player), actionName);
	}

	@Override
	public void save(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(weights);
		oos.close();
		fos.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
		ObjectInputStream ois = new ObjectInputStream(fis);
		try {
			weights = (Map<String, double[]>) ois.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Error while attempting to load weights.");
			e.printStackTrace();
		}
		ois.close();
		fis.close();
	}
	
	/**
	 * Returns a random action name with probability epsilon, and the greedy w.r.t the Q-value
	 * with probability (1-epsilon)
	 * 
	 * @param state
	 * @param player
	 * @return
	 */
    public String epsilonGreedy(GameState state, int player) {
		String chosenAction = null;

		// epsilon-greedy:
		if (random.nextDouble() < epsilon) { // random choice
			// trick to randomly select from HashMap adapted from:
			// https://stackoverflow.com/a/9919827/1251716
			List<String> keys = new ArrayList<String>(weights.keySet());
			chosenAction = keys.get(random.nextInt(keys.size()));
			if (chosenAction == null) {
				logger.error("Unable to select a random action!");
			}
		} else { // greedy choice
			chosenAction = greedyChoice(state, player, weights);
		}

		return chosenAction;
	}

    /**
	 * Returns action with the highest Q-value for the given state
	 * 
	 * @param state
	 * @param player
	 * @return
	 */
	public String greedyChoice(GameState state, int player, Map<String, double[]> weights) {

		// the name of the action that be selected for this state
		String chosenAction = null;

		// feature vector
		double[] features = featureExtractor.extractFeatures(state, player);

		// argmax Q:
		double maxQ = Double.NEGATIVE_INFINITY; // because MIN_VALUE is positive =/
		for (String candidate : weights.keySet()) {
			double q = qValue(features, candidate);
			if(Double.isInfinite(q) || Double.isNaN(q)) {
				logger.warn("(+ or -) infinite qValue for action {} in state {}", candidate, features); 
			}
			if (q > maxQ) {
				maxQ = q;
				chosenAction = candidate;
			}
		}
		if (chosenAction == null) {
			logger.error("Unable to select a greedy action in state {}!", state.getTime());
			logger.error("Dumping state to errorState{}.xml", state.getTime());
			state.toxml("errorState" + state.getTime() + ".xml");
		}

		return chosenAction;
	}

	/**
	 * Returns the Q-value of a given state-action pair
	 * 
	 * @param features
	 * @param actionName
	 * @return
	 */
	public double qValue(double[] features, String actionName) {
		return MathHelper.dotProduct(features, weights.get(actionName));
	}

	@Override
	public double stateValue(GameState state, int player) {
		double[] features = featureExtractor.extractFeatures(state, player);
		
		// max Q:
		double maxQ = Double.NEGATIVE_INFINITY; // because MIN_VALUE is positive =/
		for (String candidate : weights.keySet()) {
			double q = qValue(features, candidate);
			if(Double.isInfinite(q) || Double.isNaN(q)) {
				logger.warn("(+ or -) infinite qValue for action {} in state {}", candidate, features); 
			}
			if (q > maxQ) {
				maxQ = q;
			}
		}
		
		return maxQ;
	}

	/**
	 * Copies the values of the weights (differently from {@link #setWeights(Map)}) 
	 * that only references the given map.
	 * @param from
	 */
	public void copyWeights(Map<String, double[]> from) {
		
    	for(Entry<String, double[]> entry: from.entrySet()) {
    		// not the most memory-efficient...
    		weights.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
    	}
		
	}
	

}
