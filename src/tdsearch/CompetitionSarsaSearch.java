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

import ai.core.AI;
import features.FeatureExtractor;
import features.MaterialAdvantage;
import java.io.File;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.WinLossTiesBroken;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import utils.ForwardModel;

public class CompetitionSarsaSearch extends TDSearch {

	/**
	 * The weights are per action abstraction (indexed by their names)
	 */
	private Map<String, double[]> weights;

	/**
	 * The vectors of eligibility traces (one per abstraction)
	 */
	private Map<String, double[]> eligibility;
	
	private String previousChoiceName;
	
	private GameState previousState;

	private double planningEpsilon;
        
        public CompetitionSarsaSearch(UnitTypeTable types){
            this(
                types, 
                PortfolioManager.basicPortfolio(types), 
                new WinLossTiesBroken(12000), 
                new MaterialAdvantage(types, 12000), 
                12000, //duration
                100,   //planning budget
                0.01,   // alpha
                0.1,   //epsilon
                0.99,  //gamma
                0.1,   //lambda
                1      //random seed
            );
        }
	

	/**
	 * Creates an instance of SarsaSearch with the specified parameters
	 * 
	 * @param types
	 * @param portfolio the portfolio of algorithms/action abstractions to select (a map(name -> AI))
	 * @param rewards
	 * @param featureExtractor
	 * @param timeBudget
	 * @param matchDuration the maximum match duration in cycles
	 * @param alpha
	 * @param epsilon
	 * @param gamma
	 * @param lambda
	 * @param randomSeed
	 */
	public CompetitionSarsaSearch(UnitTypeTable types, Map<String,AI> portfolio, RewardModel rewards, FeatureExtractor featureExtractor, int matchDuration, int timeBudget, double alpha, 
			double epsilon, double gamma, double lambda, int randomSeed) 
	{
		super(types, portfolio, rewards, featureExtractor, matchDuration, timeBudget, alpha, epsilon, gamma, lambda, randomSeed);
		
		planningEpsilon = 0.1; // TODO: make this a configurable parameter!
		
		//initialize previous choice and state as null (they don't exist yet)
		previousChoiceName = null;
		previousState = null;

		// initialize weights and eligibility
		weights = new HashMap<>();
		eligibility = new HashMap<String, double[]>();

		for (String aiName : abstractions.keySet()) {

			eligibility.put(aiName, new double[featureExtractor.getNumFeatures()]);

			// initializes weights randomly within [-1, 1]
			double[] abstractionWeights = new double[featureExtractor.getNumFeatures()];
			for (int i = 0; i < abstractionWeights.length; i++) {
				abstractionWeights[i] = (random.nextDouble() * 2) - 1; // randomly initialized in [-1,1]
			}
			weights.put(aiName, abstractionWeights);

		}
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		//sanity check for player ID:
		if(gs.getTime() == 0) {
                    playerID = player; //assigns the ID on the initial state

                    //attempts to load the weights (io/WxH/weights_P.bin)
                    String weightsPath = "io/" + gs.getPhysicalGameState().getWidth() 
                            + "x" + gs.getPhysicalGameState().getHeight() + 
                            "/weights_"+player + ".bin";
                    logger.info("Attempting to load weights at " + weightsPath);
                    try{
                        loadWeights(weightsPath);
                    }
                    catch (Exception e){
                        logger.error("Error while attempting to load weights... acting randomly.", e);
                    }

                    // determines the matchDuration
                    matchDuration = 12000;
                    if (gs.getPhysicalGameState().getWidth() <= 64) {
                        matchDuration = 8000;
                    }
                    if (gs.getPhysicalGameState().getWidth() <= 32) {
                        matchDuration = 6000;
                    }
                    if (gs.getPhysicalGameState().getWidth() <= 24) {
                        matchDuration = 5000;
                    }
                    if (gs.getPhysicalGameState().getWidth() <= 16) {
                        matchDuration = 4000;
                    }
                    if (gs.getPhysicalGameState().getWidth() <= 8) {
                        matchDuration = 3000;
                    } 
                    logger.info("Duration set to " + matchDuration);
                    //end (determine match duration)
			
		} else if (player != playerID) { // consistency check for other states
			logger.error("Called to play with different ID! (mine={}, given={}", playerID, player);
			logger.error("Will proceed, but behavior might be unpredictable");
		}
		
		logger.debug("v({}) for player{} before planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		sarsaPlanning(gs, player);
		logger.debug("v({}) for player{} after planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		
		String currentChoiceName = epsilonGreedy(gs, player, weights, epsilon);
		
		if(previousState != null && previousChoiceName != null) {
			// updates the 'long-term' memory from actual experience
			sarsaUpdate(previousState, player, previousChoiceName, gs, currentChoiceName, weights, eligibility);
		}
		
		// updates previous choices for the next sarsa learning update
		previousChoiceName = currentChoiceName;
		previousState = gs.clone(); //cloning fixes a subtle error where gs changes in the game engine and becomes the next state, which is undesired 
		
		//Date end = new Date(System.currentTimeMillis());
		logger.debug("Player {} selected {}.",
			player, currentChoiceName
		);
		
		return abstractionToAction(currentChoiceName, gs, player);
		
	}
	
	@Override
	public void gameOver(int winner) {
		/*
		 *  if learning from actual experience, the agent never is called to act
		 *  in a terminal state and therefore, never sees the final reward, 
		 *  whith is the most important
		 */
		logger.debug("gameOver. winner={}, playerID={}", winner, playerID);
		double tdError = rewards.gameOverReward(playerID, winner) - qValue(previousState, playerID, previousChoiceName);
		
		tdLambdaUpdateRule(previousState, playerID, previousChoiceName, tdError, weights, eligibility);
		
	}


	private void sarsaPlanning(GameState gs, int player) {
		Date begin = new Date(System.currentTimeMillis());
		Date end = begin;
		int planningBudget = (int) (.8 * timeBudget); // 80% of budget to planning
		long duration = 0;
		
		// copies 'long-term' memory to 'short-term' memory
		Map<String, double[]> planningWeights = new HashMap<>(weights);
		
		GameState state = gs.clone(); //this state will advance during the linear look-ahead search below
		
		while (duration < planningBudget) { // while time available
			// starts with a new eligibility trace vector for planning
			Map<String, double[]> planningEligibility = new HashMap<String, double[]>(); 
			resetMap(planningEligibility);

			state = gs.clone();
			String aName = epsilonGreedy(state, player, planningWeights, planningEpsilon); // aName is a short for abstraction name

			while (!state.gameover() && duration < planningBudget) { // go until game over or time is out TODO add maxcycles condition

				// issue the action to obtain the next state, issues a self-play move for the
				// opponent
				GameState nextState = state.clone();
				logger.trace("Planning step, selected {}", aName);
				String opponentAName = epsilonGreedy(state, 1 - player, planningWeights, planningEpsilon);
				
				// must retrieve both actions and only then issue them
				PlayerAction playerAction = abstractionToAction(aName, state, player);
				PlayerAction opponentAction = abstractionToAction(opponentAName, state, 1 - player);
				
				nextState.issueSafe(playerAction);
				nextState.issueSafe(opponentAction);
				ForwardModel.forward(nextState); //advances the state up to the next decision point or gameover

				// nextAName is a short for next abstraction name
				String nextAName = epsilonGreedy(nextState, player, planningWeights, planningEpsilon);

				// updates the 'short-term' memory from simulated experience
				sarsaUpdate(state, player, aName, nextState, nextAName, planningWeights, planningEligibility);

				state = nextState;
				aName = nextAName;

				// updates duration
				end = new Date(System.currentTimeMillis());
				duration = end.getTime() - begin.getTime();
			}
			
		} // end while (timeAvailable)
		
		logger.debug("Planning for player {} at frame #{} looked up to frame {} and took {}ms",
			player, gs.getTime(), state.getTime(), end.getTime() - begin.getTime()
		);
	}
	
	/**
	 * Performs a Sarsa update on the given weights using the given eligibility traces.
	 * For an experience tuple <s, a, r, s', a'>, where s is the state, a is the actionName, 
	 * r is the reward (calculated internally),
	 * s' is the next state, a' is the nextActionName
	 * 
	 * 1) Calculates the TD error: delta = r + gammna * Q(s',a') - Q(s,a) 
	 * 2) Updates the weight vector: w = w + alpha * delta * e (e is the eligibility vector) 
	 * 3) Updates the eligibility vector: e = lambda * gamma * e + features
	 * @param state
	 * @param player
	 * @param actionName
	 * @param nextState
	 * @param nextActionName
	 * @param weights
	 * @param eligibility
	 */
	private void sarsaUpdate(GameState state, int player, String actionName, GameState nextState, String nextActionName, 
			Map<String, double[]> weights, Map<String, double[]> eligibility) {
		
		logger.debug(
			"<s,a,r,s'(gameover?),a',q(s',a')> = <{}, {}, {}, {}({}), {}, {}>",
			state.getTime(), actionName, 
			rewards.reward(nextState, player), 
			nextState.getTime(), nextState.gameover(), nextActionName,
			qValue(nextState, player, nextActionName)
		);
		
		//delta = r + gammna * Q(s',a') - Q(s,a)
		double tdError = tdTarget(nextState, player, nextActionName) - qValue(state, player, actionName);

		tdLambdaUpdateRule(state, player, actionName, tdError, weights, eligibility);
		
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

	private void tdLambdaUpdateRule(GameState state, int player, String actionName, double tdError, Map<String, double[]> weights,
			Map<String, double[]> eligibility) {
		
		double[] f = featureExtractor.extractFeatures(state, player); // feature vector for the state
		
		for (String abstractionName : weights.keySet()) {
			double[] w = weights.get(abstractionName); // weight vector
			double[] e = eligibility.get(abstractionName); // eligibility vector 
			
			// certifies that things are ok
			assert w.length == e.length;
			assert e.length == f.length;
			
			// vector updates
			for (int i = 0; i < w.length; i++) {
				w[i] = w[i] + alpha * tdError * e[i]; // weight vector update
				e[i] = e[i] * gamma * lambda; //the eligibility of all actions decays by gamma * lambda
			}
		}
		
		// incrementes the eligibility of the selected action by adding the feature vector
		double[] eSelected = eligibility.get(actionName);
		for (int i = 0; i < eSelected.length; i++) {
			eSelected[i] += f[i];
		}
	}
	
	/* * (OLD VERSION)
	 * Performs a Sarsa update for the given experience tuple <s, a, r, s', a'>. s
	 * is the state, a is the actionName, r is the reward (calculated internally),
	 * s' is the next state, a' is the nextActionName
	 * 
	 * 1) Calculates the TD error: delta = r + Q(s',a') - Q(s,a) 
	 * 2) Updates the weight vector: w = w + alpha * delta * e (e is the eligibility vector) 
	 * 3) Updates the eligibility vector: e = lambda * gamma * e + features
	 * 
	 * @param state
	 * @param player
	 * @param actionName
	 * @param nextState
	 * @param nextActionName
	 *
	private void sarsaLearning(GameState state, int player, String actionName, GameState nextState,
			String nextActionName) {

		double tdError = tdTarget(nextState, player, nextActionName) - qValue(state, player, actionName);

		double[] w = weights.get(actionName); // weight vector
		double[] e = eligibility.get(actionName); // eligibility vector for the selected action
		double[] f = featureExtractor.extractFeatures(state, player); // feature vector for the state

		// weight vector update
		for (int i = 0; i < w.length; i++) {
			w[i] += alpha * tdError * e[i];
		}

		// the eligibility of all action(abstraction)s decays by gamma * lambda
		for (String abstractionName : eligibility.keySet()) {
			double[] anyEligibility = eligibility.get(abstractionName);

			for (int i = 0; i < anyEligibility.length; i++) {
				anyEligibility[i] = anyEligibility[i] * gamma * lambda;
			}
		}

		// adds the feature vector to the eligibility of the selected
		// action(abstraction)
		for (int i = 0; i < e.length; i++) {
			e[i] = e[i] * lambda + f[i];
		}
	}*/

	/**
	 * The temporal-difference target is, by definition, r + gamma * q(s', a'),
	 * where s' is the reached state and a' is the action to be performed there.
	 * 
	 * Here, we adopt no intermediate rewards. If the game is over and the player
	 * won, r is 1 and q(s', a') is 0. If the game is over and the player lost or
	 * draw, r is 0 and q(s', a') is 0. If the game is not over, r is 0 and q(s',
	 * a') is the predicted value given by the function approximator.
	 * 
	 * TODO at gameover, it might be interesting to break ties with in-game score
	 * rather than give zero reward
	 * 
	 * @param nextState
	 * @param player
	 * @param nextActionName
	 * @return
	 */
	private double tdTarget(GameState nextState, int player, String nextActionName) {
		double reward, nextQ;
		reward = rewards.reward(nextState, player);
		
		// terminal states have value of zero
		if (nextState.gameover() || nextState.getTime() >= matchDuration) {
			nextQ = 0;
		} else {
			nextQ = qValue(nextState, player, nextActionName);
		}
		logger.trace("Reward for time {} for player {}: {}. q(s',a')={}. GameOver? {}", nextState.getTime(), player, reward, nextQ, nextState.gameover());
		return reward + this.gamma * nextQ;
	}

	/**
	 * Returns the name of the AI that would act in this state using epsilon greedy
	 * (a random AI name with probability epsilon, and the greedy w.r.t the Q-value
	 * with probability (1-epsilon)
	 * 
	 * @param state
	 * @param player
	 * @param weights
	 * @param epsilon 
	 * @return
	 */
	private String epsilonGreedy(GameState state, int player, Map<String, double[]> weights, double epsilon) {

		// the name of the AI that will choose the action for this state
		String chosenName = null;

		// epsilon-greedy:
		if (random.nextDouble() < epsilon) { // random choice
			// trick to randomly select from HashMap adapted from:
			// https://stackoverflow.com/a/9919827/1251716
			List<String> keys = new ArrayList<String>(weights.keySet());
			chosenName = keys.get(random.nextInt(keys.size()));
			if (chosenName == null) {
				logger.error("Unable to select a random abstraction!");
			}
		} else { // greedy choice
			chosenName = greedyChoice(state, player, weights);
		}

		return chosenName;
	}

	/**
	 * Returns the name of the AI/action abstraction with the highest Q-value for
	 * the given state
	 * 
	 * @param state
	 * @param player
	 * @return
	 */
	private String greedyChoice(GameState state, int player, Map<String, double[]> weights) {

		// the name of the AI that will choose the action for this state
		String chosenName = null;

		// feature vector
		double[] features = featureExtractor.extractFeatures(state, player);

		// argmax Q:
		double maxQ = Double.NEGATIVE_INFINITY; // because MIN_VALUE is positive =/
		for (String candidateName : weights.keySet()) {
			double q = qValue(features, candidateName);
			if(Double.isInfinite(q) || Double.isNaN(q)) {
				logger.warn("(+ or -) infinite qValue for action {} in state {}", candidateName, features); 
			}
			if (q > maxQ) {
				maxQ = q;
				chosenName = candidateName;
			}
		}
		if (chosenName == null) {
			logger.error("Unable to select an action abstraction for the greedy action in state {}! Selecting WorkerRush to avoid a crash.", state.getTime());
			chosenName = "WorkerRush";
		}

		return chosenName;
	}

	/**
	 * Returns an action that the AI with the given name would perform for the given
	 * state
	 * 
	 * @param state
	 * @param player
	 * @return
	 */
	private PlayerAction abstractionToAction(String name, GameState state, int player) {
		logger.trace(
			String.format("Translating action of %s for player %d at time %d", 
				name, player, state.getTime()
			));

		AI abstraction = abstractions.get(name);

		PlayerAction action = null;
		try {
			action = abstraction.clone().getAction(player, state);
		} catch (Exception e) {
			logger.error("Abstraction '" + abstraction + "' failed to return an action. Filling w/ nones.", e);
			action.fillWithNones(state, player, 1);
		}

		logger.trace("Issuing action " + action);

		if (!action.integrityCheck()) {
			logger.error(String.format("Illegal action attempted by %s at time %d for player %d", name, state.getTime(),
					player));
		}

		return action;
	}

	/**
	 * Returns the value of the state described by the given feature vector.
	 * Performs a max over the Q-values for that state.
	 * 
	 * @param features
	 * @param player
	 * @return
	 */
	public double stateValue(double[] features) {
		double maxQ = Double.NEGATIVE_INFINITY;
		for (String candidateName : weights.keySet()) {
			double q = qValue(features, candidateName);
			
			logger.trace("q(s,{})={}", candidateName, q);
			if (q > maxQ) {
				maxQ = q;
			}
		}
		return maxQ;
	}

	/**
	 * Returns the Q-value for the given state-action pair
	 * FIXME: must receive the weights to differentiate simulated from real experience
	 * 
	 * @param state
	 * @param player
	 * @param actionName the name of the action abstraction (the AI name)
	 * @return
	 */
	private double qValue(GameState state, int player, String actionName) {
		return qValue(featureExtractor.extractFeatures(state, player), actionName);
	}

	/**
	 * Returns the Q-value of an action abstraction for the state described by the
	 * given feature vector
	 * FIXME: must receive the weights to differentiate simulated from real experience
	 * 
	 * @param features
	 * @param abstractionName
	 * @return
	 */
	private double qValue(double[] features, String abstractionName) {
		return linearCombination(features, weights.get(abstractionName));
	}

	/**
	 * Resets the eligibility vectors (all zeros)
	 */
	private void resetMap(Map<String, double[]> map) {
		for (String abstractionName : abstractions.keySet()) {
			// resets all values in vector to zero
			map.put(abstractionName, new double[featureExtractor.getNumFeatures()]);
		}
	}

	/**
	 * Saves weights to a binary file
	 * 
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
	 * 
	 * @param path
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void loadWeights(String path) throws IOException {
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
	
	@Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
		preGameAnalysis(gs, milliseconds, "io");
    }

	@Override
    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
        
        int maxCycles = 12000;
        if (gs.getPhysicalGameState().getWidth() <= 64) {
            maxCycles = 8000;
        }
        if (gs.getPhysicalGameState().getWidth() <= 32) {
            maxCycles = 6000;
        }
        if (gs.getPhysicalGameState().getWidth() <= 24) {
            maxCycles = 5000;
        }
        if (gs.getPhysicalGameState().getWidth() <= 16) {
            maxCycles = 4000;
        }
        if (gs.getPhysicalGameState().getWidth() <= 8) {
            maxCycles = 3000;
        }
                
        // creates the player instance
        TDSearch player = new CompetitionSarsaSearch(
            gs.getUnitTypeTable(), 
            abstractions,
            rewards,
            featureExtractor,
            maxCycles,
            0, alpha, epsilon, gamma, lambda, 1
        );

		// creates the training opponent
        TDSearch trainingOpponent = new CompetitionSarsaSearch(
            gs.getUnitTypeTable(),
            abstractions,
            rewards,
            featureExtractor,
            maxCycles,
            0, alpha, epsilon, gamma, lambda, 2
        );
		
	Logger logger = LogManager.getRootLogger();
			
        // creates output directory if needed
        String outputPrefix = readWriteFolder + "/" + gs.getPhysicalGameState().getWidth() + "x" + gs.getPhysicalGameState().getHeight();
        File f = new File(outputPrefix);
        if (!f.exists()) {
                logger.info("Creating directory " + outputPrefix);
                System.out.println();
                f.mkdirs();
        }
		
        // training matches
        logger.info("Starting training...");
        
        Date begin = new Date(System.currentTimeMillis());
        Date end = begin;
        int planningBudget = (int) (.99 * milliseconds); // saves 1% to prevent hiccups
        long duration = 0;

        int currentMatch = 1;
        while (duration < planningBudget) { // while time available
 
            GameState state = gs.clone();
        
            boolean gameover = false;

            while (!gameover && state.getTime() < maxCycles) {
        	
                if(duration > planningBudget) return;   //no time left, quit the current match
                
        	// initializes state equally for the players 
        	GameState player1State = state; 
        	GameState player2State = state; 
        	       	
        	// retrieves the players' actions
        	PlayerAction player1Action = player.getAction(0, player1State);
        	PlayerAction player2Action = trainingOpponent.getAction(1, player2State);

        	// issues the players' actions
        	state.issueSafe(player1Action);
		state.issueSafe(player2Action);

		// runs one cycle of the game
		gameover = state.cycle();
                
                // updates duration
                end = new Date(System.currentTimeMillis());
                duration = end.getTime() - begin.getTime();
            } //end of the match
        
            player.gameOver(state.winner());
            trainingOpponent.gameOver(state.winner());
            
            player.saveWeights(outputPrefix + "/weights_0.bin");
            trainingOpponent.saveWeights(outputPrefix + "/weights_1.bin");
            
            logger.info("weights saved");
            
            System.out.print(String.format("\rMatch %8d finished with result %3d.", currentMatch, state.winner()));
            currentMatch++;

        }
        
        logger.info("Taining finished...");
        
    }
	
	/**
     * Resets the portfolio with the new unit type table
     */
    public void reset(UnitTypeTable utt) {
    	for(AI ai : abstractions.values()){
    		ai.reset(utt);
    	}
    	
    	reset();
    	
    }
    
    /**
     * Is called at the beginning of every game. Resets all AIs in the portfolio
     * and my internal variables. It does not reset the weight vector
     */
    public void reset() {
    	for(AI ai : abstractions.values()){
    		ai.reset();
    	}
    }

}
