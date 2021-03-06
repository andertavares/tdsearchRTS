package features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rts.GameState;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class QuadrantModel implements FeatureExtractor {
	
	public static final int NUM_QUADRANTS = 3; //TODO make it a parameter
	
	private UnitTypeTable types;
	private int maxCycles;
	private int numFeatures;
	
	public QuadrantModel(UnitTypeTable types, int maxGameCycles) {
		this.types = types;
		this.maxCycles = maxGameCycles;
		this.numFeatures = featureNames().size(); //caches it just in case...
	}

	@Override
	public int getNumFeatures() {
		return numFeatures;
	}

	@Override
	public double[] extractFeatures(GameState s, int player) {
		// in a linkedhashmap, elements are recovered in the order they were stored
		// this will be useful to unbox everything in the end
		Map<String, Double> features = new LinkedHashMap<>();
		
		// adds the 'global' features
		features.put("bias", 1.0);
		features.put("resources_p0", s.getPlayer(0).getResources() / 50.0);
		features.put("resources_p1", s.getPlayer(1).getResources() / 50.0);
		features.put("game_time", s.getTime() / (double)maxCycles);
		
		
		// --- now for the quadrant-dependent features
		
		int width = s.getPhysicalGameState().getWidth();
		int height = s.getPhysicalGameState().getHeight();
		// determines the length of the quadrants. 
		// If dimensions are not multiple of the number of quadrants, rounds the division up to ensure 
		// the whole map is swept
		int xQuadLength = width % NUM_QUADRANTS == 0 ? width / NUM_QUADRANTS : 1 + width / NUM_QUADRANTS;
		int yQuadLength = height % NUM_QUADRANTS == 0 ? height / NUM_QUADRANTS : 1 + height / NUM_QUADRANTS;
		int numTiles = xQuadLength * yQuadLength; //number of tiles per quadrant (max #units that can be there)
        
        // for each quadrant, counts the number of units of each type per player
		for (int xQuad = 0; xQuad < NUM_QUADRANTS; xQuad++){
			for (int yQuad = 0; yQuad < NUM_QUADRANTS; yQuad++){
				
				// arrays counting the sum of hit points and number of units owned by each player
				// (we're actually using proportional hit points of units: current / max)
				double hpSum[] = new double[2];
				int unitCount[] = new int[2];
				
				// initializes the unit count and HP sum of each type and player as zero
				hpSum[0] = hpSum[1] = 0;
				unitCount[0] = unitCount[1] = 0;
				
				// initializes the count of each unit type as zero, ignoring resources
				for(UnitType type : types.getUnitTypes()){
					if(type.name.equalsIgnoreCase("resource")) continue; 
					
					features.put(unitCountFeatureName(xQuad, yQuad, 0, type), 0.0); //for player 0
					features.put(unitCountFeatureName(xQuad, yQuad, 1, type), 0.0); //for player 1
				}

				// a collection of units in this quadrant 
				Collection<Unit> unitsInQuad = s.getPhysicalGameState().getUnitsInRectangle(
					xQuad*xQuadLength, yQuad*yQuadLength, xQuadLength, yQuadLength
				);
				
				// traverses the list of units in quadrant, incrementing their feature count
				for(Unit u : unitsInQuad){
					if(u.getType().isResource) continue;	//ignores resources
					
					unitCount[u.getPlayer()]++;
					hpSum[u.getPlayer()] += u.getHitPoints() / (double) u.getType().hp;

					String name = unitCountFeatureName(xQuad, yQuad, u.getPlayer(), u.getType());
					
					// counts and increment the number of the given unit in the current quadrant
					// increment is 1/quadsize due to normalization (assuming a max of 50 units)
					// defaults to zero if this is the first time the feature is being queried
					features.put(name, features.get(name) + 1.0 / numTiles); 
				}
				
				// computes the average HP of units owned by each player
				for(int p = 0; p < 2; p++){ // p for each player
					double avgHP = unitCount[p] != 0 ? hpSum[p] / unitCount[p] : 0;
					features.put(avgHealthFeatureName(xQuad, yQuad, p), avgHP);
				}
				
			}
		}
		// now I must unbox everything =/
		double[] featureArray = new double[getNumFeatures()];
		int i = 0;
		for(Double d : features.values()) {
			featureArray[i] = d.doubleValue();
			i++;
		}
		// this one-liner did not work for unboxing: Stream.of(features.values()).mapToDouble(Double::doubleValue).toArray();
		return featureArray;
	}

	@Override
	public List<String> featureNames() {
		List<String> featureNames = new ArrayList<>();
		
		featureNames.add("bias");
		featureNames.add("resources_p0");
		featureNames.add("resources_p1");
		featureNames.add("game_time");
		
		// adds the 'per-quadrant' features 

		// the first two for traverse the quadrants
		for (int xQuad = 0; xQuad < NUM_QUADRANTS; xQuad++){
			for (int yQuad = 0; yQuad < NUM_QUADRANTS; yQuad++){
				
				// the third for traverses the unit types
				for(UnitType type : types.getUnitTypes()){
					if(type.name.equalsIgnoreCase("resource")) continue; // does not count resource in map, only owned by players
					
					// one call for each player (0 and 1)
					featureNames.add(unitCountFeatureName(xQuad, yQuad, 0, type));
					featureNames.add(unitCountFeatureName(xQuad, yQuad, 1, type));
				}
				
				// one call for each player (0 and 1)
				featureNames.add(avgHealthFeatureName(xQuad, yQuad, 0));
				featureNames.add(avgHealthFeatureName(xQuad, yQuad, 1));
			}
		}	
		
		return featureNames;
	}

	private String avgHealthFeatureName(int xQuad, int yQuad, int player) {
		return "avg_health-" + xQuad + "-" + yQuad + "-" + player;
	}

	private String unitCountFeatureName(int xQuad, int yQuad, int player, UnitType type) {
		return "unit_count-" + xQuad + "-" + yQuad + "-" + player + "-" + type.name;
	}

}
