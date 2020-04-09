package learner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import features.FeatureExtractor;
import rts.GameState;

public class MockupFeatureExtractor implements FeatureExtractor {

	Map<GameState, double[]> mapping;
	
	double[] featureVector;
	
	/**
	 * Initializes with the specified feature vector
	 * @param featureVector
	 * @param numFeatures
	 */
	public MockupFeatureExtractor(double[] featureVector) {
		mapping = new HashMap<>();
		
		setFeatureVector(featureVector);
	}
	
	public void setFeatureVector(double[] features) {
		featureVector = features;
	}
	
	/**
	 * Adds a pair (state, features). These features will be returned when
	 * {@link #extractFeatures(GameState, int)} is called with the 
	 * corresponding state.
	 * 
	 * @param state
	 * @param features
	 */
	public void putMapping(GameState state, double[] features) {
		mapping.put(state, features);
	}
	
	@Override
	/**
	 * Just returns the length of a previously set feature vector
	 */
	public int getNumFeatures() {
		return featureVector.length;
	}

	@Override
	/**
	 * A simple method that returns the previously set feature vector for the given state
	 * (or the default feature vector if no mapping was previously set)
	 */
	public double[] extractFeatures(GameState s, int player) {
		if (mapping.size() == 0) {
			return featureVector;
		}
		
		// doing a manual "get" in the HashMap because hash values are different
		// for 'equivalent' states represented by different (e.g. cloned) objects
		for(Entry<GameState, double[]> entry : mapping.entrySet()) {
			if(entry.getKey().equals(s)) {
				return entry.getValue();
			}
		}
		
		// should not get here...
		System.out.println("Unable to find an entry for " + s + ". Prepare for error!");
		return null;	
		
	}

	@Override
	public List<String> featureNames() {
		List<String> names = new ArrayList<>(featureVector.length);
		for(int i = 0; i < featureVector.length; i++) {
			names.add(""+i);	//name is the index
		}
		return names;
	}

}
