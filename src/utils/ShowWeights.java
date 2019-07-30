package utils;

import java.util.Map;
import java.util.Properties;

import config.ConfigManager;
import config.Parameters;
import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import learner.UnrestrictedPolicySelectionLearner;
import rts.units.UnitTypeTable;

public class ShowWeights {

	/**
	 * TODO provide a reverse array to facilitate reading the feature names
	 * @param args 0 is the config file 1 is the weights file
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		Properties config = ConfigManager.loadConfig(args[0]);
		Parameters.ensureDefaults(config);
		
		
		// creates this object just to pass to the feature extractor constructor
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_RANDOM);
		
		FeatureExtractor extractor = FeatureExtractorFactory.getFeatureExtractor(config.getProperty("features"), types, 0);
		
		UnrestrictedPolicySelectionLearner learner = UnrestrictedPolicySelectionLearner.fromConfig(types, 0, config);
		
		//loads the weights
		learner.loadWeights(args[1]);
		Map<String, double[]> weights = learner.getWeights();
		
		//outputs the feature and its value for each strategy
		int i = 0; //accounts for the index in the feature vector
		for(String featureName : extractor.featureNames()) {
			System.out.println(featureName + ":");
			
			for (String strategyName : weights.keySet()) {
				System.out.println(String.format("\t%s: %f", strategyName, weights.get(strategyName)[i]));
			}
			
			i++; //moves to the next feature
		}
	}

}
