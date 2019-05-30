package reward;

import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;

/**
 * Gives 1 on victory, 0 on defeat or non-terminals.
 * Breaks ties by material advantage
 * @author artavares
 *
 */
public class WinLossTiesBroken implements RewardModel {
	
	int timeLimit;
	
	public WinLossTiesBroken(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	@Override
	/**
	 * Returns 0 for non-terminal states or defeats
	 * Returns 1 for victories.
	 * Ties broken by evaluation on the terminal state (whoever has the higher scores wins)
	 */
	public double reward(GameState state, int player) {
		double reward;
		if(state.gameover() || state.getTime() >= timeLimit) {
			if(state.winner() == -1) { // draw
				//break ties by the evaluation of the terminal state
				reward = (new SimpleSqrtEvaluationFunction3().evaluate(player, 1-player, state) > 0) ? 1 : 0;  
			}
			else reward = state.winner() == player ? 1 : 0; //win or loss
		}
		else {
			reward = 0; //game not finished
		}
		return reward;
	}

	@Override
	/**
	 * Returns 1 for victory, 0 otherwise (we're unable to break ties here)
	 */
	public double gameOverReward(int player, int winner) {
		return winner == player ? 1 : 0;
	}

}
