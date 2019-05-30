package reward;

import rts.GameState;

public class WinLossDraw implements RewardModel {

int timeLimit;
	
	public WinLossDraw(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	@Override
	/**
	 * Returns 0 for non-terminal states or draws
	 * Returns 1 for victories or -1 for defeats.
	 */
	public double reward(GameState state, int player) {
		double reward;
		if(state.gameover() || state.getTime() >= timeLimit) {
			if(state.winner() == -1) { 
				reward = 0; // draw
			}
			else reward = state.winner() == player ? 1 : -1; //win or loss
		}
		else {
			reward = 0; //game not finished
		}
		return reward;
	}

	@Override
	/**
	 * Returns 1 for victory, 0 for draw, -1 for loss
	 */
	public double gameOverReward(int player, int winner) {
		if(winner == -1) return 0;
		return winner == player ? 1 : -1; 
	}

}
