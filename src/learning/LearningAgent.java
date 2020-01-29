package learning;

import java.io.IOException;

import rts.GameState;

public interface LearningAgent {

	/**
	 * Returns an action (represented by a string) for the given state.
	 * The player number is "part" of the state information
	 * @param state
	 * @param player
	 * @return
	 */
	public String act(GameState state, int player);
	
	/**
	 * Updates the agent's knowledge
	 * @param state
	 * @param player
	 * @param action
	 * @param reward
	 * @param nextState
	 * @param done whether the nextState is terminal
	 */
	public void learn(GameState state, int player, String action, double reward, GameState nextState, boolean done);
	
	/**
	 * Called at the end of an episode so that the agent 
	 * can update the value of an action taken prior to the terminal state
	 * @param winner
	 */
	public void finish(int winner);
	
	/**
	 * Returns the value for a state-action pair
	 * @param state
	 * @param player
	 * @param action
	 * @return 
	 */
	public double qValue(GameState state, int player, String action);
	
	/**
	 * Returns the value of a state
	 * @param state
	 * @param player
	 * @return
	 */
	public double stateValue(GameState state, int player);
	
	/**
	 * Saves the agent knowledge to the specified path
	 * @param path
	 * @throws IOException
	 */
	public void save(String path) throws IOException;
	
	/**
	 * Loads the agent knowledge from the specified path
	 * @param path
	 * @throws IOException
	 */
	public void load(String path) throws IOException;
	
	
	
}
