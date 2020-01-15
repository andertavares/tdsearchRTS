package features;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

class TestQuadrantModel {
	@Test
	void testFeatureNames() {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		QuadrantModel extractor = new QuadrantModel(types, 3000);
		
		List<String> expectedNames = new ArrayList<>(); 
		expectedNames.add("bias");
		expectedNames.add("resources_p0");
		expectedNames.add("resources_p1");
		expectedNames.add("game_time");
			
		String[] unitTypes = {"Base", "Barracks", "Worker", "Light", "Heavy", "Ranged"};
		
		// adds the unit_count feature names per quadrant & player (0 and 1) & unit type
		// adds the avg_health feature names per quadrant & player (0 and 1)
		for(int xQuad = 0; xQuad < 3; xQuad++) {
			for(int yQuad = 0; yQuad < 3; yQuad++) {
				for(String type : unitTypes){
					expectedNames.add(String.format("unit_count-%d-%d-%d-%s", xQuad, yQuad, 0, type));
					expectedNames.add(String.format("unit_count-%d-%d-%d-%s", xQuad, yQuad, 1, type));
				}
				
				expectedNames.add(String.format("avg_health-%d-%d-%d", xQuad, yQuad, 0));
				expectedNames.add(String.format("avg_health-%d-%d-%d", xQuad, yQuad, 1));
			}
		}
		assertEquals(expectedNames, extractor.featureNames());
	}

	@Test
	void testInitial8x8State() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		QuadrantModel extractor = new QuadrantModel(types, 3000);
		
		// 4 quadrant-independent features: bias, resources0,1 & game time
		// 9*2 = 18 features of avg health
		// 9*2*6 = 108 features of unit count per type
		// total: 4 + 18 + 108 = 130 features
		assertEquals(130, extractor.getNumFeatures());
		
		// creates a linked hash map as the feature extractor does, then unboxes it and compares the resulting arrays
		Map<String, Double> features = new LinkedHashMap<>();
		
		// adds the 'global' features
		features.put("bias", 1.0);
		features.put("resources_p0", 5.0 / 50);
		features.put("resources_p1", 5.0 / 50);
		features.put("game_time", 0.0);
		
		// initializes quadrant-dependent features as zero
		String[] unitTypes = {"Base", "Barracks", "Worker", "Light", "Heavy", "Ranged"};
		for (int xQuad = 0; xQuad < 3; xQuad++){
			for (int yQuad = 0; yQuad < 3; yQuad++){
					// initializes the count of each unit type as zero, ignoring resources
					for(String type : unitTypes){
						features.put(String.format("unit_count-%d-%d-%d-%s",xQuad, yQuad, 0, type), 0.0); //for player 0
						features.put(String.format("unit_count-%d-%d-%d-%s",xQuad, yQuad, 1, type), 0.0); //for player 1
					}
					
					features.put(String.format("avg_health-%d-%d-%d", xQuad, yQuad, 0), 0.0); //for player 0
					features.put(String.format("avg_health-%d-%d-%d", xQuad, yQuad, 1), 0.0); //for player 1
					
			}
		}
		
		// manually sets the values of known features. 
		int numTilesPerQuadrant = 9; //since each quadrant is a 3x3 region
		
		//p0 has a base and a worker in quadrant (0,0), both at full health
		features.put("unit_count-0-0-0-Base", 1.0 / numTilesPerQuadrant ); 
		features.put("unit_count-0-0-0-Worker", 1.0 / numTilesPerQuadrant ); 
		features.put("avg_health-0-0-0", 1.0); 
		
		//p1 has a base in quadrant (1, 2) and a worker in quadrant (2, 2), both at full health
		features.put("unit_count-1-2-1-Base", 1.0 / numTilesPerQuadrant );
		features.put("avg_health-1-2-1", 1.0);
		features.put("unit_count-2-2-1-Worker", 1.0 / numTilesPerQuadrant );
		features.put("avg_health-2-2-1", 1.0);
		
		
		// unboxes the linked hash map into the expected feature array
		double[] expectedFeatures = new double[130];	//130 is the number of features verified above
		int i = 0;
		for(Double d : features.values()) {
			expectedFeatures[i] = d.doubleValue();
			i++;
		}
		double[] extractedFeatures = extractor.extractFeatures(state, 1);
		//System.out.println(Arrays.toString(extractedFeatures));
		//System.out.println(extractor.featureNames());
		assertArrayEquals(expectedFeatures, extractedFeatures, 1E-5);
		
	}

}
