package reward;

import rts.GameState;

/**
 * Gives 1 for victories and 0 otherwise
 * @author artavares
 *
 */
public class VictoryOnly implements RewardModel {

	@Override
	/**
	 * Returns 1 for victories and 0 otherwise (draw, loss or non-terminal states)
	 */
	public double reward(GameState state, int player) {
		
		if (state.gameover() && state.winner() == player ) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	/**
	 * Returns 1 for victories and 0 for draws and losses
	 */
	public double gameOverReward(int player, int winner) {
		return winner == player ? 1 : 0;
	}

}
