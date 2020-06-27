package reward;

import rts.GameState;

public class WinLossDuration implements RewardModel {

	/**
	 * Maximum duration of a match, in frames
	 */
	private int maxDuration;
	
	/**
	 * Elapsed frames in the last seen state.
	 * This is needed in gameOverReward because the method
	 * does not receive the final game state =/
	 */
	private int lastDuration;

	public WinLossDuration(int maxDuration) {
		this.maxDuration = maxDuration;
	}
	
	@Override
	public double reward(GameState state, int player) {
		lastDuration = state.getTime();
		
		double reward;
		if(state.gameover() || state.getTime() >= maxDuration) {
			reward = gameOverReward(player, state.winner());
		}
		else {
			reward = 0; //game not finished
		}
		return reward;
	}

	@Override
	public double gameOverReward(int player, int winner) {
		int rFactor;
		if(winner == -1) rFactor = 0;
		else rFactor = (winner == player) ? 1 : -1; 
		
		// r*(dmax - d)/dmax -- scales between -1 and 1
		return rFactor * (double) (maxDuration - lastDuration ) / maxDuration;
	}

}
