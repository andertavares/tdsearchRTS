package utils;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import config.ConfigManager;
import config.Parameters;
import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import learning.LinearSarsaLambda;
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
		
		LinearSarsaLambda learner = new LinearSarsaLambda(types, config, 0);
		
		//loads the weights
		learner.load(args[1]);
		Map<String, double[]> weights = learner.getWeights();
		
		// writes a header with the feature names
		System.out.println("strategy," + String.join(",", extractor.featureNames()));
		
		//writes the feature values for one strategy per line
		for (String strategyName : weights.keySet()) {
			System.out.println(
				strategyName + "," +  
				Arrays.stream(weights.get(strategyName)) //array to csv black magic: https://stackoverflow.com/a/38425624/1251716
		        .mapToObj(String::valueOf)
		        .collect(Collectors.joining(","))
		    );
		}
		
		
	}

}
