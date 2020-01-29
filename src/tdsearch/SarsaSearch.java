package tdsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import learning.LinearSarsaLambda;
import portfolio.PortfolioManager;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import utils.ForwardModel;

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
	
	/**
	 * Planning counterpart, to generate the actions of the opponent
	 */
	LearningAgent planningOpponent; 
	
	/**
	 * Creates a SarsaSearch object by specifying all parameters
	 * @param types
	 * @param learner
	 * @param portfolio
	 * @param maxCycles
	 * @param timeBudget
	 * @param decisionInterval
	 */
	public SarsaSearch(UnitTypeTable types, LearningAgent learner, Map<String, AI> portfolio, int maxCycles, int timeBudget, int decisionInterval) {
		this.learner = learner;
		this.planningOpponent = ((LinearSarsaLambda)learner).almostClone();
		this.maxCycles = maxCycles;
		this.abstractions = portfolio;
		this.timeBudget = timeBudget;
		this.decisionInterval = decisionInterval;

		choices = new ArrayList<>();
		logger = LogManager.getRootLogger();
	}
			
	/**
	 * Instantiates SarsaSearch with parameters from a config object
	 * 
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
		
		// launches planning
		//logger.debug("v({}) for player{} before planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		
		logger.debug("Frame {}. Player {} would choose: {} before planning.", gs.getTime(), player, learner.act(gs, player));
		sarsaPlanning(gs, player);
		logger.debug("Frame {}. Player {} would choose: {} after planning.", gs.getTime(), player, learner.act(gs, player));
		
		//logger.debug("v({}) for player{} after planning: {}", gs.getTime(), player, stateValue(featureExtractor.extractFeatures(gs, player)));
		
		// performs a new choice if the interval has passed
		if (decisionInterval <= 1 || gs.getTime() % decisionInterval == 0) { 
			// determines the current choice
			currentChoiceName = learner.act(gs, player);
		}

		// logs and stores the current choice (even if unchanged)
		logger.debug("Frame {}. Player {} chose: {}.", gs.getTime(), player, currentChoiceName);
		choices.add(currentChoiceName);
		
		return abstractionToAction(currentChoiceName, gs, player);
		
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


	/**
	 * Launches the planning procedure from the current state & player. 
	 * Assumes that someone loaded the planning opponent with the proper weights.
	 * @param gs
	 * @param player
	 */
	private void sarsaPlanning(GameState gs, int player) {
		Date begin = new Date(System.currentTimeMillis());
		Date end = begin;
		int planningBudget = (int) (.8 * timeBudget); // 80% of budget to planning
		long elapsed = 0;
		
		GameState state = gs.clone(); //this state will advance during the linear look-ahead search below
		
		while (elapsed < planningBudget) { // while time available
			// starts with a new eligibility trace vector for planning
			LinearSarsaLambda planner = ((LinearSarsaLambda)learner).clone();

			state = gs.clone();
			
			// go until the match ends, the time is over or the planning budget is over
			while (!state.gameover() && state.getTime() < maxCycles && elapsed < planningBudget) { 

				// requests the action from the planners (learning happens inside the act method) 
				String aName = planner.act(state, player); // aName is a short for abstraction name
				String opponentAName = planningOpponent.act(state, 1 - player);
				logger.trace("Planning step, selected {} vs {}", aName, opponentAName);
				
				// retrieves the actions given by the abstractions
				PlayerAction playerAction = abstractionToAction(aName, state, player);
				PlayerAction opponentAction = abstractionToAction(opponentAName, state, 1 - player);
				
				// issues the actions
				GameState nextState = state.clone();
				nextState.issueSafe(playerAction);
				nextState.issueSafe(opponentAction);
				ForwardModel.forward(nextState); //advances the state up to the next decision point or gameover
				
				// (don't need to call planner.learn() here because it happens inside 'act'
				
				state = nextState;

				// updates duration
				end = new Date(System.currentTimeMillis());
				elapsed = end.getTime() - begin.getTime();
			}

			// if reached a gameover or timeout, let learners finish
			if(state.gameover() || state.getTime() >= maxCycles) {
				logger.debug("Planning reached gameover({}), winner: {}", state.getTime(), state.winner());
				planner.finish(state.winner());
				planningOpponent.finish(state.winner());
			}
			
		} // end while (timeAvailable)
		
		logger.debug("Planning for player {} at frame #{} looked up to frame {} (over={}) and took {}ms",
			player, gs.getTime(), state.getTime(), state.gameover(), end.getTime() - begin.getTime()
		);
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
	
	/**
	 * Loads the weights for the planning adversary from a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadPlanningOpponentWeights(String path) throws IOException {
		planningOpponent.load(path);
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
    	choices = new ArrayList<>(); //resets the list of choices 
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
