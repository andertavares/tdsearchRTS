package reward;

import rts.GameState;

/**
 * Gives 1 for victories and 0 otherwise
 * @author artavares
 *
 */
public class WinOneZero implements RewardModel {

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

}
