package features;

import rts.GameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;


/**
 * Accounts for Material advantage + the closest distance between enemy units
 * @author artavares
 *
 */
public class UnitDistance extends MaterialAdvantage {
	
	
	/**
	 * Initializes a FeatureExtractor with specified maximum values for some entities for normalizing.
	 * The minimum is always zero.
	 * 
	 * @param unitTypeTable the specified types for the game
	 * @param maxTime the maximum duration of a game, in cycles
	 * @param maxUnits the maximum number of possible units for the specific game
	 * @param maxResources the maximum number of possible resources a player will possess in the specific game
	 */
	public UnitDistance(UnitTypeTable unitTypeTable, int maxTime, int maxUnits, int maxResources) {
		
		super(unitTypeTable, maxTime, maxUnits, maxResources);
		
		// we add one feature (distance between closest enemy unit) to the material advantage
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the max values.
	 * Time is 15000, units and resources are 50
	 * @param unitTypeTable
	 */
	public UnitDistance(UnitTypeTable unitTypeTable) {
		this(unitTypeTable, 15000, 50, 50);
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the non-specified max values.
	 * units and resources are 50
	 * @param unitTypeTable
	 * @param maxTime
	 * @param mapSize
	 */
	public UnitDistance(UnitTypeTable types, int maxTime) {
		this(types, maxTime, 50, 50);
	}
	
	/**
	 * UnitDistance adds one feature to MaterialAdvantage
	 */
	public int getNumFeatures() {
		return super.getNumFeatures() + 1;
	}
	
	public double[] extractFeatures(GameState s, int player) {
		//copies the features extracted by the superclass
		//TODO change the returned type to Vector or ArrayList?
		double[] features = new double[getNumFeatures()];
		double[] materialAdvFeatures = super.extractFeatures(s, player);
		
		int finalParentIndex = super.getNumFeatures();
		
		for (int i = 0; i < finalParentIndex; i++) {
			features[i] = materialAdvFeatures[i];
		}
		
		int mapWidth = s.getPhysicalGameState().getWidth();
		int mapHeight = s.getPhysicalGameState().getHeight();
		
		// retrieves the shortest Manhattan distance between enemy units and normalizes 
		features[finalParentIndex] = shortestManhattanDistanceBetweenEnemies(s) / (double)(mapWidth + mapHeight);
		
		return features;
	}
	
	/**
	 * Retrieves the shortest Manhattan distance between units belonging to different players
	 * @param state
	 * @return
	 */
	public int shortestManhattanDistanceBetweenEnemies(GameState state) {
		int shortestDistance = Integer.MAX_VALUE;
		
		// inefficient way to determine the distance...
		for(Unit u : state.getUnits()) {
			for(Unit v : state.getUnits()) {
				// skips resources and units from the same player
				if(u.getPlayer() == v.getPlayer() || u.getType().isResource || v.getType().isResource) continue;
				
				int distance = Math.abs(u.getX() - v.getX()) + Math.abs(u.getY() - v.getY());
				if (distance < shortestDistance) {
					shortestDistance = distance;
				}
				
			}
		}
		
		return shortestDistance;
	}
}
