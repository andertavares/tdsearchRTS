package tdsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import ai.core.ParameterSpecification;
import learning.LearningAgent;
import learning.LearningAgentFactory;
import portfolio.PortfolioManager;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class SarsaSearch extends AI {

	/**
	 * Interval between decision points
	 */
	private int decisionInterval;

	/**
	 * All choices (one per frame)
	 */
	public List<String> choices;

	/**
	 * The actual ID of this player
	 */
	protected int playerID;

	/**
	 * Time budget to return an action
	 */
	protected int timeBudget;

	/**
	 * The match duration
	 */
	protected int maxCycles;

	/**
	 * The random number generator
	 */
	protected Random random;
	
	/**
	 * This maps the AI name to its instance.
     * Each AI filters out the possible actions to consider at each state.
     * Thus they're called the action abstractions.
     */
    protected Map<String,AI> abstractions;

	String currentChoiceName;

	protected Logger logger;

	LearningAgent learner;
	
	public SarsaSearch(UnitTypeTable types, LearningAgent learner, Map<String, AI> portfolio, int maxCycles, int timeBudget, int decisionInterval) {
		this.learner = learner;
		this.maxCycles = maxCycles;
		this.abstractions = portfolio;
		this.timeBudget = timeBudget;
		this.decisionInterval = decisionInterval;

		choices = new ArrayList<>();
		logger = LogManager.getRootLogger();
	}
			
	/**
	    * Instantiates a policy selector with parameters from a config object
	    * @param types
	    * @param randomSeed
	    * @param config
	    */
	   public SarsaSearch(UnitTypeTable types, int randomSeed, Properties config) {
		   this(
			   types, 
			   LearningAgentFactory.getLearningAgent(config.getProperty("learner"), types, config, randomSeed),
			   PortfolioManager.getPortfolio(types, Arrays.asList(config.getProperty("portfolio").split(","))),
			   Integer.parseInt(config.getProperty("max_cycles")),
			   Integer.parseInt(config.getProperty("search.timebudget")),
			   Integer.parseInt(config.getProperty("decision_interval"))
		   );
	   }

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		//sanity check for player ID:
		if(gs.getTime() == 0) {
			playerID = player; //assigns the ID on the initial state
			
		} else if (player != playerID) { // consistency check for other states
			logger.error("Called to play with different ID! (mine={}, given={}", playerID, player);
			logger.error("Will proceed, but behavior might be unpredictable");
		}
		
		// performs a new choice if the interval has passed
		if (decisionInterval <= 1 || gs.getTime() % decisionInterval == 0) { 
			// determines the current choice
			currentChoiceName = learner.act(gs, player);
		}

		// logs and stores the current choice (even if unchanged)
		logger.debug("Frame {}. Player {} chose: {}.", gs.getTime(), player, currentChoiceName);
		choices.add(currentChoiceName);
		
		return abstractionToAction(currentChoiceName, gs, player);
		
		/*logger.debug("v({}) for player{} before planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		sarsaPlanning(gs, player);
		logger.debug("v({}) for player{} after planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		*/
		
	}
	
	/**
	 * Returns the choices performed by this agent
	 * 
	 * @return
	 */
	public List<String> getChoices() {
		return choices;
	}

	@Override
	public void gameOver(int winner) {
		/*
		 *  if learning from actual experience, the agent never is called to act
		 *  in a terminal state and therefore, never sees the final reward, 
		 *  which is the most important
		 */
		logger.debug("gameOver. winner={}, playerID={}", winner, playerID);
		learner.finish(winner);
		
	}


	/*private void sarsaPlanning(GameState gs, int player) {
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
	}*/
	

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

	/*
	 * Returns the value of the state described by the given feature vector.
	 * Performs a max over the Q-values for that state.
	 * 
	 * @param features
	 * @param player
	 * @return
	 *
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
	}*/


	/**
	 * Saves learner's weights to a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void saveWeights(String path) throws IOException {
		learner.save(path);
	}

	/**
	 * Load weights from a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadWeights(String path) throws IOException {
		learner.load(path);
	}
	
	@Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
		//preGameAnalysis(gs, milliseconds, "io");
    }

	@Override
    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
		/*
		String portfolioNames = "WorkerRush, LightRush, RangedRush, HeavyRush, "
				+ "WorkerDefense, LightDefense, RangedDefense, HeavyDefense, " + "BuildBase, BuildBarracks";

		int maxCycles = 12000;
		if (gs.getPhysicalGameState().getWidth() <= 64) {
			maxCycles = 8000;
		} else if (gs.getPhysicalGameState().getWidth() <= 32) {
			maxCycles = 6000;
		} else if (gs.getPhysicalGameState().getWidth() <= 24) {
			maxCycles = 5000;
		} else if (gs.getPhysicalGameState().getWidth() <= 16) {
			maxCycles = 4000;
		} else if (gs.getPhysicalGameState().getWidth() <= 8) {
			maxCycles = 3000;
		}

		// loads the reward model (default=victory-only)
		RewardModel rwd = new WinLossTiesBroken(maxCycles);

		FeatureExtractor fe = new MaterialAdvantage(gs.getUnitTypeTable(), maxCycles);

		// creates the player instance
		TDSearch player = new SarsaSearch(gs.getUnitTypeTable(),
				PortfolioManager.getPortfolio(gs.getUnitTypeTable(), Arrays.asList(portfolioNames.split(","))), rwd, fe,
				maxCycles, 0, alpha, epsilon, gamma, lambda, 1);

		// creates the training opponent
		TDSearch trainingOpponent = new SarsaSearch(gs.getUnitTypeTable(),
				PortfolioManager.getPortfolio(gs.getUnitTypeTable(), Arrays.asList(portfolioNames.split(","))), rwd, fe,
				maxCycles, 0, alpha, epsilon, gamma, lambda, 2);

		Logger logger = LogManager.getRootLogger();

		// creates output directory if needed
		String outputPrefix = readWriteFolder + "/" + gs.getPhysicalGameState().getWidth() + "x"
				+ gs.getPhysicalGameState().getHeight();
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

				if (duration > planningBudget)
					return; // no time left, quit the current match

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
			} // end of the match

			player.gameOver(state.winner());
			trainingOpponent.gameOver(state.winner());

			player.saveWeights(outputPrefix + "/weights_0.bin");

			// save opponent weights if selfplay
			if (trainingOpponent instanceof TDSearch) {
				((TDSearch) trainingOpponent).saveWeights(outputPrefix + "/weights_1.bin");
			}

			System.out.print(String.format("\rMatch %8d finished with result %3d.", currentMatch, state.winner()));
			currentMatch++;

		}

		logger.info("Starting training finished...");
		*/

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

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
