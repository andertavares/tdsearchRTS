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
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.encog.neural.networks.BasicNetwork;

import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import reward.RewardModel;
import reward.RewardModelFactory;
import rts.GameState;
import rts.units.UnitTypeTable;
import utils.MathHelper;
import utils.StrategyNames;

/**
 * A Q-learning agent with the action-value function approximated by a multi-layer perceptron
 * @author artavares
 * TODO remove duplicate code from LinearQLearning
 */
public class MLPQLearning implements LearningAgent {
	
		/**
		 * Taken action
		 */
		private String action;
		
		/**
		 * A list of possible actions
		 */
		List<String> actions;
	   
	   
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
	    
	
	public MLPQLearning(UnitTypeTable types, Properties config) {
		int maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
 		
        rewards = RewardModelFactory.getRewardModel(
 		   config.getProperty("rewards"), maxCycles
        );
        
        featureExtractor = FeatureExtractorFactory.getFeatureExtractor(
 		   config.getProperty("features"), types, maxCycles
 	    );
        
        actions = StrategyNames.acronymsToNames(Arrays.asList(
 		   config.getProperty("strategies").split(",") 
 	    ));
        
        alpha = Double.parseDouble(config.getProperty("td.alpha.initial")); 
        epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial"));
        gamma = Double.parseDouble(config.getProperty("td.gamma")); 
 		
 		random = new Random(Integer.parseInt(config.getProperty("random_seed", "0")));
 		
 		// create  a neural  network ,  without  using a  factory
 		BasicNetwork network = new BasicNetwork(); //encog should be 'importable' now...
 		
 		initialize();
	}
	
	/**
     * Initializes internal variables (logger, weights, eligibility traces as well as
     * current and previous state and action)
     */
    private void initialize() {
    	logger = LogManager.getRootLogger();
    	action = null;
        previousState = nextState = null;
    	
        //weights = new HashMap<>();
        
        playerID = -1;	//initializes with a 'flag' value so that it is updated in the first call to 'act' 

        /*for (String action : actions) {

            // initializes weights randomly within [-1, 1]
            double[] actionWeights = new double[featureExtractor.getNumFeatures()];
            for (int i = 0; i < actionWeights.length; i++) {
                    actionWeights[i] = (random.nextDouble() * 2) - 1; // randomly initialized in [-1,1]
            }
            weights.put(action, actionWeights);
        }*/
    }

	@Override
	public String act(GameState state, int player) {
		// sets my player ID on the first call
		if(playerID == -1) {
			playerID = player;
		}
		
		// updates the previous and current states, as well as previous and current actions
        previousState = nextState;
        nextState = state.clone();
        
        // gets the reward for this state
        double reward = rewards.reward(state, player);

    	// performs q-learning update on the current experience tuple, except on the 1st state (nextState is null)
        if(previousState != null && action != null) {
            learn(previousState, player, action, reward, nextState, state.gameover());
        }

        // selects and returns the current action
        action = epsilonGreedy(state, player);
		return action;
	}

	@Override
	/**
	 * Performs an update with an s,a,r,s' experience tuple. 
	 * If a' was not already chosen for s', it will be chosen here if s' is not terminal
	 */
	public void learn(GameState state, int player, String action, double reward, GameState nextState, boolean done) {
		
		double maxQ = done ? 0 : maxQ(state, player);
		
		logger.debug(
			"Player {}: <s,a,r,s'(gameover?),a',q(s',a')> = <{}, {}, {}, {}({}), {}, {}>",
			player,
			state.getTime(), action, 
			reward, 
			nextState == null ? "null" : nextState.getTime(), 
			done, 
			done ? "null" : action,
			maxQ
		);
		
		//calculates the td error = r + gammna * max_a' Q(s',a') - Q(s,a)
		double tdError = reward + gamma * maxQ - qValue(state, player, action);

		// updates the weight vector: w = w + alpha * tdError * f
		double[] f = featureExtractor.extractFeatures(state, player); // feature vector for the state
		double[] w = weights.get(action); // weight vector of the taken action
		for (int i = 0; i < w.length; i++) {
			w[i] = w[i] + alpha * tdError * f[i]; 
		}
	}
	
	/**
	 * Returns the highest Q-value for the given state
	 * @param state
	 * @param player
	 * @return
	 */
	private double maxQ(GameState state, int player) {
		// maxQ is the q-value of the greedy action
		return qValue(state, player, greedyChoice(state, player, weights));
	}

	@Override
	public void finish(int winner) {
		
		// updates the value of the last taken state-action pair
		double finalReward = rewards.gameOverReward(playerID, winner);
		learn(nextState, playerID, action, finalReward, null, true);
		
		// resets the variables
		previousState = nextState = null;
		actions = null;
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

}
