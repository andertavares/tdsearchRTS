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
		int xQuadLength = s.getPhysicalGameState().getWidth() / NUM_QUADRANTS;
		int yQuadLength = s.getPhysicalGameState().getHeight() / NUM_QUADRANTS;
        
        // for each quadrant, counts the number of units of each type per player
		for (int xQuad = 0; xQuad < NUM_QUADRANTS; xQuad++){
			for (int yQuad = 0; yQuad < NUM_QUADRANTS; yQuad++){
				
				// arrays counting the sum of hit points and number of units owned by each player
				float hpSum[] = new float[2];
				int unitCount[] = new int[2];
				
				// a collection of units in this quadrant:
				Collection<Unit> unitsInQuad = s.getPhysicalGameState().getUnitsAround(
					xQuad*xQuadLength, yQuad*yQuadLength, xQuadLength
				);
				
				// initializes the unit count of each type and player as zero
				// also initializes the sum of HP of units owned per player as zero
				for(int p = 0; p < 2; p++){ // p for each player
					//unitCountPerQuad.put(p, new HashMap<>());
					hpSum[p] = 0;
					unitCount[p] = 0;
				}
				
				// traverses the list of units in quadrant, incrementing their feature count
				for(Unit u : unitsInQuad){
					if(u.getType().isResource) continue;	//ignores resources
					
					unitCount[u.getPlayer()]++;
					hpSum[u.getPlayer()] += u.getHitPoints();

					String name = unitCountFeatureName(xQuad, yQuad, u.getPlayer(), u.getType());
					
					// counts and increment the number of the given unit in the current quadrant
					// defaults to zero if this is the first time the feature is being queried
					features.put(name, features.getOrDefault(name, 0.0) + 1);
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
				
				// the third for traverses the players
				for(int player = 0; player < 2; player++){
					
					featureNames.add(avgHealthFeatureName(xQuad, yQuad, player));
					
					// the fourth for traverses the unit types
					for(UnitType type : types.getUnitTypes()){
						featureNames.add(unitCountFeatureName(xQuad, yQuad, player, type));
					}
				}
			}
		}	
		
		return featureNames;
	}

	private String avgHealthFeatureName(int xQuad, int yQuad, int player) {
		return String.format(
			"avg_health-%d-%d-%d", 
			xQuad, yQuad, player
		);
	}

	private String unitCountFeatureName(int xQuad, int yQuad, int player, UnitType type) {
		return String.format(
			"unit_count-%d-%d-%d-%s", 
			xQuad, yQuad, player, type.name
		);
	}

}
