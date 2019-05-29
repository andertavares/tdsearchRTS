package features;

import rts.GameState;

public interface FeatureExtractor {
	
	/**
	 * Returns the total number of features
	 * @return
	 */
	public int getNumFeatures();
	
	/**
	 * Returns the feature vector that describes a state
	 * @param s
	 * @param player
	 * @return
	 */
	public double[] extractFeatures(GameState s, int player);

}
