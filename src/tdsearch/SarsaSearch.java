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
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class SarsaSearch extends TDSearch {

	/**
	 * The weights are per action abstraction (indexed by their names)
	 */
	private Map<String, double[]> weights;
	
	/**
	 * The vectors of eligibility traces (one per abstraction)
	 */
	private Map<String, double[]> eligibility;
	
	/**
	 * Creates an instance of SarsaSearch with the specified parameters
	 * @param types
	 * @param timeBudget
	 * @param alpha
	 * @param epsilon
	 * @param gamma
	 * @param lambda
	 * @param randomSeed
	 */
	public SarsaSearch(UnitTypeTable types, int timeBudget, double alpha, double epsilon, double gamma, double lambda,
			int randomSeed) {
		super(types, timeBudget, alpha, epsilon, gamma, lambda, randomSeed);
		
		// initialize weights and eligibility
		weights = new HashMap<>();
		eligibility = new HashMap<String, double[]>();
    	
    	for(String aiName : abstractions.keySet()){
    		
    		eligibility.put(aiName, new double[featureExtractor.getNumFeatures()]);
    		
	    	double[] abstractionWeights = new double[featureExtractor.getNumFeatures()];
	    	
	    	for (int i = 0; i < abstractionWeights.length; i++){
	    		// weights are initialized randomly within [-1, 1]
	    		abstractionWeights[i] = (random.nextDouble() * 2) - 1 ; //randomly initialized in [-1,1]
	    	}
	    	weights.put(aiName, abstractionWeights);
	    	
    	}
	}
	
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		
		Date begin = new Date(System.currentTimeMillis());
		Date end;
    	int planningBudget =  (int) (.8 * timeBudget); //80% of budget to planning
    	long duration = 0;
    	
		while (duration < planningBudget) { // while time available
			// resets the eligibility traces
			resetEligibility();
			
			GameState state = gs.clone();
			String aName = epsilonGreedyAbstraction(state, player); //aName is a short for abstraction name
			
			while (!state.gameover() && duration < planningBudget) { // go until game over or time is out
				
				// the features of this state
				//double[] features = featureExtractor.extractFeatures(state, player);
				
				// issue the action to obtain the next state, issues a self-play move for the opponent
				GameState nextState = state.clone();
				logger.debug("Issuing action " + aName);
				String opponentAName = epsilonGreedyAbstraction(state, 1 - player);
				nextState.issueSafe(abstractionToAction(aName, state, player));
				nextState.issueSafe(abstractionToAction(opponentAName, state, 1 - player));
				nextState.cycle();
				
				// nextAName is a short for next abstraction name
				String nextAName = epsilonGreedyAbstraction(nextState, player);
				
				sarsaLearning(state, player, aName, nextState, nextAName);
				
				state = nextState; 
				aName = nextAName;
				
				// updates duration 
				end = new Date(System.currentTimeMillis());
				duration = end.getTime() - begin.getTime();
			}
		} //end while (timeAvailable)
		
		String selectedAbstractionName = greedyAbstraction(gs, player);
		return abstractionToAction(selectedAbstractionName, gs, player);
	}

	/**
	 * Performs a Sarsa update for the given experience tuple <s, a, r, s', a'>.
	 * s is the state, a is the actionName, r is the reward (calculated internally), 
	 * s' is the next state, a' is the nextActionName
	 * 
	 * 1) Calculates the TD error: delta = r + Q(s',a') - Q(s,a)
	 * 2) Updates the weight vector: w = w + alpha * delta * e (e is the eligibility)
	 * 3) Updates the eligibility: e = lambda * gamma * e + features
	 * 
	 * @param state
	 * @param player
	 * @param actionName
	 * @param nextState
	 * @param nextActionName
	 */
	private void sarsaLearning(GameState state, int player, String actionName, GameState nextState,
			String nextActionName) {
		
		double tdError = tdTarget(nextState, player, nextActionName) - qValue(state, player, actionName);
		
		double[] w = weights.get(actionName);		// weight vector
		double[] e = eligibility.get(actionName);	// eligibility vector for the selected action
		double[] f = featureExtractor.extractFeatures(state, player);	//feature vector for the state
		
		// weight vector update
		for(int i = 0; i < w.length; i++) {
			w[i] += alpha * tdError * e[i];	
		}
		
		// the eligibility of all action(abstraction)s decays by gamma * lambda
		for(String abstractionName : eligibility.keySet()) {
			double[] anyEligibility = eligibility.get(abstractionName);
			
			for(int i = 0; i < anyEligibility.length; i++) {
				anyEligibility[i] = anyEligibility[i] * gamma * lambda;
			}
		}
		
		// adds the feature vector to the eligibility of the selected action(abstraction) 		
		for(int i = 0; i < e.length; i++) {
			e[i] = e[i] * lambda + f[i];
		}
	}
	
	
	/**
	 * The temporal-difference target is, by definition, r + gamma * q(s', a'), 
	 * where s' is the reached state and a' is the action to be performed there
	 * Here, we adopt no intermediate rewards. 
	 * If the game is over and the player won, r is 1 and q(s', a') is 0.
	 * If the game is over and the player lost or draw, r is 0 and q(s', a') is 0.
	 * If the game is not over, r is 0 and q(s', a') is the predicted value given by the function approximator
	 * 
	 * TODO at gameover, it might be interesting to break ties with in-game score rather than give zero reward
	 * @param nextState
	 * @param player
	 * @param nextActionName
	 * @return
	 */
	private double tdTarget(GameState nextState, int player, String nextActionName) {
		double reward, nextQ;
		
		if (nextState.gameover()) { 
			nextQ = 0;
			reward = nextState.winner() != player ? 1 : 0;
		}
		else {
			reward = 0;
			nextQ = qValue(nextState, player, nextActionName);
		}
		return reward + gamma * nextQ;
	}

	/**
	 * Returns the name of the AI that would act in this state using 
	 * epsilon greedy (a random AI name with probability epsilon, 
	 * and the greedy w.r.t the Q-value with probability (1-epsilon)  
	 * @param state
	 * @param player
	 * @return
	 */
	private String epsilonGreedyAbstraction(GameState state, int player){
    	
    	// the name of the AI that will choose the action for this state
        String chosenName = null;
        
        // epsilon-greedy:
        if(random.nextDouble() < epsilon){ //random choice
        	//trick to randomly select from HashMap adapted from: https://stackoverflow.com/a/9919827/1251716
        	List<String> keys = new ArrayList<String>(abstractions.keySet());
        	chosenName = keys.get(random.nextInt(keys.size()));
        	if(chosenName == null){
        		logger.error("Unable to select a random abstraction!");
        	}
        }
        else { //greedy choice
        	chosenName = greedyAbstraction(state, player);
        }
        
        return chosenName;
	}
	
	/**
	 * Returns the name of the AI/action abstraction with the highest Q-value for the given state
	 * @param state
	 * @param player
	 * @return
	 */
	private String greedyAbstraction(GameState state, int player) {
		
		// the name of the AI that will choose the action for this state
        String chosenName = null;
		
		// feature vector
        double[] features = featureExtractor.extractFeatures(state, player);
    	
        // argmax Q:
        double maxQ = Double.NEGATIVE_INFINITY; //because MIN_VALUE is positive =/
    	for(String candidateName: weights.keySet()){
    		double q = qValue(features, candidateName);
    		if (q > maxQ){
    			maxQ = q;
    			chosenName = candidateName;
    		}
    	}
    	if(chosenName==null){
    		logger.error("Unable to select an action abstraction for the greedy action!");
    	}
    	
    	return chosenName;
	}

	/**
     * Returns an action that the AI with the given name would perform for the given state
     * @param state
     * @param player
     * @return
     */
    private PlayerAction abstractionToAction(String name, GameState state, int player){
    	
        AI abstraction =  abstractions.get(name);
        
        PlayerAction action = null;
        try {
        	action = abstraction.getAction(player, state);
		} catch (Exception e) {
			logger.error("Abstraction '" + abstraction +"' failed to return an action. Filling w/ nones.", e);
			action.fillWithNones(state, player, 1);
		}
        
        return action;
    }
    
    /**
     * Returns the value of the state described by the given feature vector.
     * Performs a max over the Q-values for that state.
     * @param features
     * @param player
     * @return
     */
    public double stateValue(double[] features) {
    	double maxQ = Double.NEGATIVE_INFINITY; 
    	for(String candidateName: weights.keySet()){
    		double q = qValue(features, candidateName);
    		if (q > maxQ){
    			maxQ = q;
    		}
    	}
    	return maxQ;
    }
	
    /**
     * Returns the Q-value for the given state-action pair
     * @param state
     * @param player
     * @param actionName the name of the action abstraction (the AI name)
     * @return
     */
    private double qValue(GameState state, int player, String actionName) {
		return qValue(featureExtractor.extractFeatures(state, player), actionName);
	}
    
    /**
     * Returns the Q-value of an action abstraction for the state described by the given feature vector 
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
	private void resetEligibility() {
		for(String abstractionName : abstractions.keySet()) {
			// each vector of eligibility traces is initialized to zero (thanks, java)
			eligibility.put(abstractionName, new double[featureExtractor.getNumFeatures()]);
		}
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
	
	

}