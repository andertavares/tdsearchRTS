package reward;

import rts.GameState;

/**
 * An interface for rewards from microRTS  
 * @author artavares
 *
 */
public interface RewardModel {
	/**
	 * Returns the reward for the given game state, 
	 * from the point of view of the given player
	 * @param state
	 * @param player
	 * @return
	 */
	public double reward(GameState state, int player);
}
