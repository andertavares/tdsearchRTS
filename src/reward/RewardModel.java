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
	
	/**
	 * Gives the reward on gameover, from the point of view
	 * of a player.
	 * This method is necessary when learning from actual experience,
	 * as getAction is never called for a terminal state and the agent never sees it,
	 * but the gameOver method is called.
	 * 
	 * @winner the winner code (0, 1, or -1 if the game is a draw)
	 * @param player
	 * @return
	 */
	public double gameOverReward(int player, int winner);
}
