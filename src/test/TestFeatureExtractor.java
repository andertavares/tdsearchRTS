package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import learningeval.FeatureExtractor;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

class TestFeatureExtractor {

	@Test
	void testInitial8x8State() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		GameState state = new GameState(map, types);
		
		FeatureExtractor extractor = new FeatureExtractor(types, 3000, 64, 64, 50);
		
		double[] features = extractor.extractFeatures(state, 1);
		
		assertEquals(18, extractor.getNumFeatures());
		
		assertEquals(1, features[0]);	//bias
		assertEquals(8 / 64.0, features[1]);	//width
		assertEquals(8 / 64.0, features[2]);	//height
		assertEquals(5 / 50.0, features[3]);	//resources p0
		assertEquals(5 / 50.0, features[4]);	//resources p1
		assertEquals(0., features[5]);			//time
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
	
	@Test
	void testErrorState() throws JDOMException, IOException, Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH); 
		PhysicalGameState map = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// loads errorState.xml 
		GameState state = GameState.fromXML("src/test/errorState.xml", types);
		
		FeatureExtractor extractor = new FeatureExtractor(types, 3000, 64, 64, 50);
		
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

}
