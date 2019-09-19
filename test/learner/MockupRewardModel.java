package learner;

import reward.RewardModel;
import rts.GameState;

/**
 * Simple, configurable, reward model. Useful for testing
 * @author artavares
 *
 */
public class MockupRewardModel implements RewardModel {

	double reward;
	double gameOverReward;
	
	/**
	 * Initializes the mockup reward model with pre-defined values
	 * @param currentReward
	 * @param gameOverReward
	 */
	public MockupRewardModel(double currentReward, double gameOverReward) {
		setValues(currentReward, gameOverReward);
	}
	
	public void setValues(double currentReward, double gameOverReward) {
		this.reward = currentReward;
		this.gameOverReward = gameOverReward;
	}
	
	/**
	 * On the first call returns the currentReward, on the second, returns the nextReward (repeats every two calls)
	 */
	@Override
	public double reward(GameState state, int player) {
		return reward;
	}

	/**
	 * Returns the previously set game over reward
	 */
	@Override
	public double gameOverReward(int player, int winner) {
		return gameOverReward;
	}

}
