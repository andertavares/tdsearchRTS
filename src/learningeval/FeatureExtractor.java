package learningeval;

import java.util.HashMap;
import java.util.Map;

import rts.GameState;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

/**
 * Extracts features from game states
 * @author artavares
 *
 */
public class FeatureExtractor {
	
	private int numFeatures;
	
	private static final int MAX_MAPSIZE = 256;
	private static final int MAX_UNITS = 50;
	private static final int MAX_RESOURCES = 50;
	
	Map<UnitType, Integer> baseIndexes;
	
	public FeatureExtractor(UnitTypeTable unitTypeTable) {
		numFeatures = 16;
		
		baseIndexes = new HashMap<UnitType, Integer>();
		
		int startingUnitIndex = 5; //features regarding units start at index 5 in the feature vector
		
		// baseIndexes will help to locate unit features in the array
		// there are two features per unit type: the count for player 0 and 1
		// hence, each unit type occupies two positions in the array and the offset increases by two 
		// at each line below
		baseIndexes.put(unitTypeTable.getUnitType("Worker"), startingUnitIndex);	
		baseIndexes.put(unitTypeTable.getUnitType("Light"), startingUnitIndex + 2);
		baseIndexes.put(unitTypeTable.getUnitType("Heavy"), startingUnitIndex + 4);
		baseIndexes.put(unitTypeTable.getUnitType("Ranged"), startingUnitIndex + 6);
		baseIndexes.put(unitTypeTable.getUnitType("Base"), startingUnitIndex + 8);
		baseIndexes.put(unitTypeTable.getUnitType("Barracks"), startingUnitIndex + 10);
		
	}
	
	public int getNumFeatures() {
		return numFeatures;
	}
	
	public double[] extractFeatures(GameState s, int player) {
		//count: resources, bases, barracks, workers, heavy, light, ranged for both players
		double[] features = new double[numFeatures];
		
		features[0] = 1; //bias, always 1
		
		// map dimensions, capped at a maximum value
		features[1] = Math.min(s.getPhysicalGameState().getWidth(), MAX_MAPSIZE);
		features[2] = Math.min(s.getPhysicalGameState().getHeight(), MAX_MAPSIZE);
		
		// resources
		features[3] = Math.min(s.getPlayer(player).getResources(), MAX_RESOURCES);	//player's resources
		features[4] = Math.min(s.getPlayer(1-player).getResources(), MAX_RESOURCES);	//opponent's resources
		
		for (Unit u : s.getPhysicalGameState().getUnits()) {
			if (u.getType().isResource) continue; //map resources are not interesting
			
			//gets the base index from unit type, adds 0 if the unit is ally and 1 if it is enemy
			int index = baseIndexes.get(u.getType()) + u.getPlayer() == player ? 0 : 1;
			
			// caps the maximum number of units to aid normalization
			features[index] = Math.min(index, MAX_UNITS);
		}
		
		// BEGIN: normalize features
		// 1 and 2 are normalized by MAX_MAPSIZE
		features[1] /= MAX_MAPSIZE;
		features[2] /= MAX_MAPSIZE;
		
		// 3 and 4 are normalized at MAX_RESOURCES
		features[3] /= MAX_RESOURCES;
		features[4] /= MAX_RESOURCES;
		
		// 5 to 16 (inclusive) are normalized at MAX_UNITS
		for(int i = 5; i <= 16; i++) {
			features[i] /= MAX_UNITS;
		}
		// END: normalize features
		
		return features;
	}
}
