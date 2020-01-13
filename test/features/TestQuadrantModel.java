package features;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestQuadrantModel {
	@Test
	void testFeatureNames() {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		QuadrantModel extractor = new QuadrantModel(types, 3000);
		
		List<String> expectedNames = new ArrayList<>() {{
			add("bias");
			add("resources_p0");
			add("resources_p1");
			add("time");
			add("unit_count-0-0-0-base");
			add("unit_count-0-0-1-base");
			add("unit_count-0-1-0-base");
			add("unit_count-0-1-1-base"); //... make it faster
			
	    }};
		
	}

	@Test
	void testInitial8x8State() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		QuadrantModel extractor = new QuadrantModel(types, 3000);
		
		double[] features = extractor.extractFeatures(state, 1);
		
		assertEquals(17, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(5 / 50.0, features[1]);	//resources p0
		assertEquals(5 / 50.0, features[2]);	//resources p1
		assertEquals(0., features[3]);			//time
		
		// unit count: unit_count-x-y-player-type
		// avg hp: avg_health-x-y-player
		assertEquals(1.0 / 4, features[4]);	//unit_count-0-0-0-base
		assertEquals(1.0 / 4, features[5]);	//unit_count-0-0-1-base
		assertEquals(1.0 / 4, features[5]);	//unit_count-0-0-0-barracks
		assertEquals(1.0 / 4, features[5]);	//unit_count-0-0-1-barracks
		assertEquals(0, features[7]);	//light
		assertEquals(0, features[8]);	//
		assertEquals(0, features[9]);	//heavy
		assertEquals(0, features[10]);	//
		assertEquals(0, features[11]);	//ranged
		assertEquals(0, features[12]);	//
		assertEquals(1.0 / 64, features[13]);	//base
		assertEquals(1.0 / 64, features[14]);	//
		assertEquals(0, features[15]);	//barracks
		assertEquals(0, features[16]);	//
		
	}

}
