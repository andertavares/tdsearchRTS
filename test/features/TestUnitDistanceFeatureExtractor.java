package features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import features.UnitDistance;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestUnitDistanceFeatureExtractor {

	@Test
	void testInitial8x8State() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		UnitDistance extractor = new UnitDistance(types, 3000, 64, 50);
		
		double[] features = extractor.extractFeatures(state, 1);
		
		assertEquals(17, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(8 / 16.0, features[1]);	//shortestDistanceBetweenEnemies
		assertEquals(5 / 50.0, features[2]);	//resources p0
		assertEquals(5 / 50.0, features[3]);	//resources p1
		assertEquals(0., features[4]);			//time
		assertEquals(1.0 / 64, features[5]);	//worker
		assertEquals(1.0 / 64, features[6]);	//
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
		//assertEquals(8 / 16.0, features[17]);	//shortest distance
		
	}
	/*
	@Test
	void testErrorState() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// loads errorState.xml 
		GameState state = GameState.fromXML("src/test/errorState.xml", types);
		
		MapAware extractor = new MapAware(types, 3000, 64, 64, 50);
		
		double[] features = extractor.extractFeatures(state, 0);
		
		assertEquals(18, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(8 / 64.0, features[1]);	//width
		assertEquals(8 / 64.0, features[2]);	//height
		assertEquals(5 / 50.0, features[3]);	//resources p0
		assertEquals(5 / 50.0, features[4]);	//resources p1
		assertEquals(101.0 / 3000, features[5]);			//time
		assertEquals(1.0 / 64, features[6]);	//worker
		assertEquals(1.0 / 64, features[7]);	//
		assertEquals(0, features[8]);	//light
		assertEquals(0, features[9]);	//
		assertEquals(0, features[10]);	//heavy
		assertEquals(0, features[11]);	//
		assertEquals(0, features[12]);	//ranged
		assertEquals(0, features[13]);	//
		assertEquals(1.0 / 64, features[14]);	//base
		assertEquals(1.0 / 64, features[15]);	//
		assertEquals(0, features[16]);	//barracks
		assertEquals(0, features[17]);	//
		
		
	}
	*/
}
