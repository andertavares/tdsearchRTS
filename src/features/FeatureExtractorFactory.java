package features;

import rts.units.UnitTypeTable;

public class FeatureExtractorFactory {
	public static FeatureExtractor getFeatureExtractor(String extractorName, UnitTypeTable types, int maxGameCycles) {
        if(extractorName.equalsIgnoreCase("mapaware")) {
        	return new MapAware(types, maxGameCycles);
        }
        else if (extractorName.equals("material") || extractorName.equalsIgnoreCase("MaterialAdvantage")) {
        	 return new MaterialAdvantage(types, maxGameCycles);
        }
        else if (extractorName.equals("distance") || extractorName.equalsIgnoreCase("UnitDistance")) {
        	return new UnitDistance(types, maxGameCycles);
        }
        else if (extractorName.equals("materialdisthp") || extractorName.equalsIgnoreCase("UnitDistance")) {
        	return new MaterialAdvantageDistancesHP(types, maxGameCycles);
        }
        else {
        	throw new IllegalArgumentException("Feature extractor '" + extractorName + "' not found.");
        }
	}
}
