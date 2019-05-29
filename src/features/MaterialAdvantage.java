package features;

import java.util.HashMap;
import java.util.Map;

import rts.GameState;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

/**
 * Feature extractor that accounts for material advantage (quantity of units), 
 * plus time 
 * @author anderson
 *
 */
public class MaterialAdvantage implements FeatureExtractor {

private int numFeatures;
	
	private int maxUnits;
	private int maxResources;
	
	private int maxTime;
	
	Map<UnitType, Integer> baseIndexes;
	
	private int initialUnitIndex;
	private int numberTypes;
	
	/**
	 * Initializes a FeatureExtractor with specified maximum values for some entities for normalizing.
	 * The minimum is always zero.
	 * 
	 * @param unitTypeTable the specified types for the game
	 * @param maxTime the maximum duration of a game, in cycles
	 * @param maxUnits the maximum number of possible units for the specific game
	 * @param maxResources the maximum number of possible resources a player will possess in the specific game
	 */
	public MaterialAdvantage(UnitTypeTable unitTypeTable, int maxTime, int maxUnits, int maxResources) {
		
		this.maxTime = maxTime;
		this.maxUnits = maxUnits;
		this.maxResources = maxResources;
		
		/**
		 * 1 bias, 2 resources, 1 time, 12 unit counts 
		 */
		numFeatures = 16;
		
		baseIndexes = new HashMap<UnitType, Integer>();
		
		initialUnitIndex = 4; //features regarding units start at this index in the feature vector
		numberTypes = 6; // there are 6 unit types (resources are not regarded)
		
		//TODO do as the line commented below and initialize base index by traversing the list of types
		//numberTypes = unitTypeTable.getUnitTypes().size() - 1; 
		
		// baseIndexes will help to locate unit features in the array
		// there are two features per unit type: the count for player 0 and 1
		// hence, each unit type occupies two positions in the array and the offset increases by two 
		// at each line below
		baseIndexes.put(unitTypeTable.getUnitType("Worker"), initialUnitIndex);	
		baseIndexes.put(unitTypeTable.getUnitType("Light"), initialUnitIndex + 2);
		baseIndexes.put(unitTypeTable.getUnitType("Heavy"), initialUnitIndex + 4);
		baseIndexes.put(unitTypeTable.getUnitType("Ranged"), initialUnitIndex + 6);
		baseIndexes.put(unitTypeTable.getUnitType("Base"), initialUnitIndex + 8);
		baseIndexes.put(unitTypeTable.getUnitType("Barracks"), initialUnitIndex + 10);
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the max values.
	 * Time is 15000, units and resources are 50
	 * @param unitTypeTable
	 */
	public MaterialAdvantage(UnitTypeTable unitTypeTable) {
		this(unitTypeTable, 15000, 50, 50);
		
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the non-specified max values.
	 * units and resources are 50
	 * @param unitTypeTable
	 */
	public MaterialAdvantage(UnitTypeTable types, int maxTime) {
		this(types, maxTime, 50, 50);
	}
	
	public int getNumFeatures() {
		return numFeatures;
	}
	
	public double[] extractFeatures(GameState s, int player) {
		//count: resources, bases, barracks, workers, heavy, light, ranged for both players
		double[] features = new double[numFeatures];
		
		features[0] = 1; //bias, always 1
		
		// resources
		features[1] = Math.min(s.getPlayer(player).getResources(), maxResources);	//player's resources
		features[2] = Math.min(s.getPlayer(1-player).getResources(), maxResources);	//opponent's resources
		
		// time
		features[3] = Math.min(s.getTime(), maxTime);
		
		for (Unit u : s.getPhysicalGameState().getUnits()) {
			if (u.getType().isResource) continue; //map resources are not interesting
			
			//gets the base index from unit type, adds 0 if the unit is ally and 1 if it is enemy
			int index = baseIndexes.get(u.getType()) + (u.getPlayer() == player ? 0 : 1);
			
			// increments the unit count, capping the maximum number of units to aid normalization
			features[index] = Math.min(features[index] + 1, maxUnits);
		}
		
		// BEGIN: normalize features
		// 1 and 2 are normalized by MAX_RESOURCES
		features[1] /= maxResources;
		features[2] /= maxResources;
		
		// 5 is normalized at maxTime
		features[3] /= maxTime;
		
		// from initial unit index to 2*the number of types (inclusive) are normalized at MAX_UNITS
		// 2*number of types is used because the count is done for each player
		for(int i = initialUnitIndex; i < numFeatures; i++) {
			features[i] /= maxUnits;
		}
		// END: normalize features
		
		return features;
	}
	
}
