package features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import features.MaterialAdvantageDistancesHP;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestMaterialDistanceHPFeatureExtractor {

	@Test
	void testInitial8x8BasesWorkers() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		MaterialAdvantageDistancesHP extractor = new MaterialAdvantageDistancesHP(types, 3000, 64, 50);
		
		double[] features = extractor.extractFeatures(state, 1);
		
		assertEquals(24, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(5 / 50.0, features[1]);	//resources p0
		assertEquals(5 / 50.0, features[2]);	//resources p1
		assertEquals(0., features[3]);			//time
		assertEquals(1.0 / 64, features[4]);	//worker
		assertEquals(1.0 / 64, features[5]);	//
		assertEquals(0, features[6]);	//light
		assertEquals(0, features[7]);	//
		assertEquals(0, features[8]);	//heavy
		assertEquals(0, features[9]);	//
		assertEquals(0, features[10]);	//ranged
		assertEquals(0, features[11]);	//
		assertEquals(1.0 / 64, features[12]);	//base
		assertEquals(1.0 / 64, features[13]);	//
		assertEquals(0, features[14]);	//barracks
		assertEquals(0, features[15]);	//
		
		assertEquals(10 / 16.0, features[16]);	// shortest distance between enemy units
		assertEquals(10 / 16.0, features[17]);	// largest distance between enemy units
		
		assertEquals(9 / 16.0, features[18]); // shortest distance from my units to enemy base
		assertEquals(9 / 16.0, features[19]); // shortest distance from enemy units to my base
		
		assertEquals(1, features[20]); //lowest HP ratio remaining of a unit of mine
		assertEquals(1, features[21]); //lowest HP ratio remaining of an enemy unit  

		assertEquals(1, features[22]); //largest HP ratio remaining of a unit of mine
		assertEquals(1, features[23]); //largest HP ratio remaining of an enemy unit
		
	}
	
	/**
	 * This test enforces that distance features return sensible values when there are no 
	 * mobile units in the map
	 * @throws JDOMException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	void testInitial9x8BasesOnly() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/NoWhereToRun9x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		MaterialAdvantageDistancesHP extractor = new MaterialAdvantageDistancesHP(types, 3000, 72, 50);
		
		double[] features = extractor.extractFeatures(state, 1);
		
		assertEquals(24, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(5 / 50.0, features[1]);	//resources p0
		assertEquals(5 / 50.0, features[2]);	//resources p1
		assertEquals(0., features[3]);			//time
		assertEquals(0, features[4]);	//worker
		assertEquals(0, features[5]);	//
		assertEquals(0, features[6]);	//light
		assertEquals(0, features[7]);	//
		assertEquals(0, features[8]);	//heavy
		assertEquals(0, features[9]);	//
		assertEquals(0, features[10]);	//ranged
		assertEquals(0, features[11]);	//
		assertEquals(1.0 / 72, features[12]);	//base
		assertEquals(1.0 / 72, features[13]);	//
		assertEquals(0, features[14]);	//barracks
		assertEquals(0, features[15]);	//
		
		assertEquals(1, features[16]);	// shortest distance between enemy units
		assertEquals(0, features[17]);	// largest distance between enemy units
		
		assertEquals(1, features[18]); // shortest distance from my units to enemy base
		assertEquals(1, features[19]); // shortest distance from enemy units to my base
		
		assertEquals(1, features[20]); //lowest HP ratio remaining of a unit of mine
		assertEquals(1, features[21]); //lowest HP ratio remaining of an enemy unit  

		assertEquals(1, features[22]); //largest HP ratio remaining of a unit of mine
		assertEquals(1, features[23]); //largest HP ratio remaining of an enemy unit
		
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
