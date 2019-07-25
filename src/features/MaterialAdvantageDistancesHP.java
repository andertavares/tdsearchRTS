package features;

import java.util.List;

import rts.GameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;


/**
 * Accounts for Material advantage + various distances and measures of units HP
 * @author artavares
 *
 */
public class MaterialAdvantageDistancesHP extends MaterialAdvantage {
	
	/**
	 * Initializes a FeatureExtractor with specified maximum values for some entities for normalizing.
	 * The minimum is always zero.
	 * 
	 * @param unitTypeTable the specified types for the game
	 * @param maxTime the maximum duration of a game, in cycles
	 * @param maxUnits the maximum number of possible units for the specific game
	 * @param maxResources the maximum number of possible resources a player will possess in the specific game
	 */
	public MaterialAdvantageDistancesHP(UnitTypeTable unitTypeTable, int maxTime, int maxUnits, int maxResources) {
		
		super(unitTypeTable, maxTime, maxUnits, maxResources);
		
		// we add one feature (distance between closest enemy unit) to the material advantage
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the max values.
	 * Time is 15000, units and resources are 50
	 * @param unitTypeTable
	 */
	public MaterialAdvantageDistancesHP(UnitTypeTable unitTypeTable) {
		this(unitTypeTable, 15000, 50, 50);
	}
	
	/**
	 * Initializes the FeatureExtractor with default values for the non-specified max values.
	 * units and resources are 50
	 * @param unitTypeTable
	 * @param maxTime
	 * @param mapSize
	 */
	public MaterialAdvantageDistancesHP(UnitTypeTable types, int maxTime) {
		this(types, maxTime, 50, 50);
	}
	
	/**
	 * This class adds 8 features to material advantage:
	 * Shortest distance between enemies
	 * Largest distance between enemies
	 * Shortest distance of my units to enemy base
	 * Shortest distance of enemy units to my base
	 * Lowest HP ratio of my units
	 * Lowest HP ratio of enemy units
	 * Highest HP ratio of my units
	 * Highest HP ratio of enemy units
	 * 
	 */
	public int getNumFeatures() {
		return super.getNumFeatures() + 8;
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

		// all distances are normalized over mapWidth + mapHeight
		int mapWidth = s.getPhysicalGameState().getWidth();
		int mapHeight = s.getPhysicalGameState().getHeight();
		
		// shortest distance between enemy units
		features[finalParentIndex] = shortestManhattanDistanceToEnemyUnit(s) / (double)(mapWidth + mapHeight);
		
		// largest distance between enemy units
		features[finalParentIndex+1] = largestManhattanDistanceToEnemyUnit(s) / (double)(mapWidth + mapHeight);
		
		// shortest distance from my units to enemy base
		features[finalParentIndex+2] = shortestManhattanDistanceFromUnitToBase(s, player, 1-player) / (double)(mapWidth + mapHeight);
		
		// shortest distance from enemy units to my base
		features[finalParentIndex+3] = shortestManhattanDistanceFromUnitToBase(s, 1-player, player) / (double)(mapWidth + mapHeight);
		
		//lowest HP ratio remaining of mine and enemy units
		features[finalParentIndex+4] = lowestRemainingHPRatio(s, player);
		features[finalParentIndex+5] = lowestRemainingHPRatio(s, 1-player);
		
		//largest HP ratio remaining of mine and enemy units
		features[finalParentIndex+6] = highestRemainingHPRatio(s, player);
		features[finalParentIndex+7] = highestRemainingHPRatio(s, 1-player);
		
		return features;
	}
	
	/**
	 * Retrieves the shortest Manhattan distance between units (not buildings!) belonging to different players
	 * @param state
	 * @return
	 */
	public int shortestManhattanDistanceToEnemyUnit(GameState state) {
		// initializes the shortest distance to a sensible value (width+height)
		int width = state.getPhysicalGameState().getWidth();
		int height = state.getPhysicalGameState().getHeight();
		int shortestDistance = width + height;
		
		// inefficient way to determine the distance...
		for(Unit u : state.getUnits()) {
			for(Unit v : state.getUnits()) {
				// considers only mobile units from different players
				if(u.getPlayer() == v.getPlayer() 
						|| !u.getType().canMove || !v.getType().canMove) continue;
				
				int distance = Math.abs(u.getX() - v.getX()) + Math.abs(u.getY() - v.getY());
				if (distance < shortestDistance) {
					shortestDistance = distance;
				}
				
			}
		}
		
		return shortestDistance;
	}
	
	/**
	 * Retrieves the largest Manhattan distance between units (not buildings!) belonging to different players
	 * @param state
	 * @return
	 */
	public int largestManhattanDistanceToEnemyUnit(GameState state) {
		int largestDistance = 0;
		
		// inefficient way to determine the distance...
		for(Unit u : state.getUnits()) {
			for(Unit v : state.getUnits()) {
				// considers only mobile units from different players
				if(u.getPlayer() == v.getPlayer() 
						|| !u.getType().canMove || !v.getType().canMove) continue;
				
				int distance = Math.abs(u.getX() - v.getX()) + Math.abs(u.getY() - v.getY());
				if (distance > largestDistance) {
					largestDistance = distance;
				}
				
			}
		}
		
		return largestDistance;
	}
	
	/**
	 * Returns the shortest manhattan distance from any mobile unit of mine to 
	 * any enemy base
	 * @param state
	 * @param player
	 * @return
	 */
	public int shortestManhattanDistanceFromUnitToBase(GameState state, int player, int enemy) {
		// initializes the shortest distance to a sensible value (width+height)
		int width = state.getPhysicalGameState().getWidth();
		int height = state.getPhysicalGameState().getHeight();
		int shortestDistance = width + height;
		
		// inefficient way to determine the distance...
		for(Unit playerUnit : state.getUnits()) {
			for(Unit enemyBase : state.getUnits()) {
				// considers only my mobile units and enemy bases (stockpiles)
				if(playerUnit.getPlayer() != player || enemyBase.getPlayer() != 1-player 
						|| !playerUnit.getType().canMove || !enemyBase.getType().isStockpile) continue;
				
				int distance = Math.abs(playerUnit.getX() - enemyBase.getX()) + Math.abs(playerUnit.getY() - enemyBase.getY());
				if (distance < shortestDistance) {
					shortestDistance = distance;
				}
			}
		}
		return shortestDistance;
	}
	
	/**
	 * Returns the remaining HP ratio (currHP / maxHP) of the player's 
	 * healthiest unit 
	 * @param state
	 * @param player
	 * @return
	 */
	public double highestRemainingHPRatio(GameState state, int player) {
		double largestHPRatio = 0.0;
		for(Unit u : state.getUnits()) {
			if(u.getPlayer() != player) continue;
			
			double hpRatio = u.getHitPoints() / (double)u.getType().hp;
			if(hpRatio > largestHPRatio) {
				largestHPRatio = hpRatio;
			}
		}
		return largestHPRatio;
	}
	
	/**
	 * Returns the remaining HP ratio (currHP / maxHP) of the player's 
	 * least healthy unit 
	 * @param state
	 * @param player
	 * @return
	 */
	public double lowestRemainingHPRatio(GameState state, int player) {
		double lowestHPRatio = 1.0;
		for(Unit u : state.getUnits()) {
			if(u.getPlayer() != player) continue;
			
			double hpRatio = u.getHitPoints() / (double)u.getType().hp;
			if(hpRatio < lowestHPRatio) {
				lowestHPRatio = hpRatio;
			}
		}
		return lowestHPRatio;
	}
	
	public List<String> featureNames(){
		List<String> names = super.featureNames();
		names.add("shortestDistanceMobileUnits");
		names.add("longestDistanceMobileUnits");
		names.add("shortestDistanceFromMyUnitToEnemyBase");
		names.add("shortestDistanceFromEnemyUnitToMyBase");
		names.add("lowestRemainingHPRatioMy");
		names.add("lowestRemainingHPRatioEnemy");
		names.add("highestRemainingHPRatioMy");
		names.add("highestRemainingHPRatioEnemy");

		return names;
		
	}
}
