package utils;

import rts.GameState;

public class ForwardModel {
	/**
	 * Advances the game state up to the next decision point 
	 * (a point where a player can issue an action)
	 *
	 * @param state
	 * @return the number of frames moved forward
	 */
	public static int forward(GameState state) {
		int skippedFrames = 0;
		while(!state.gameover() && !state.canExecuteAnyAction(0) && !state.canExecuteAnyAction(1)) {
			state.cycle();
			skippedFrames++;
		}
		return skippedFrames;
	}
}
