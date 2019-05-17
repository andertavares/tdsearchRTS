package utils;

import java.util.logging.Level;

import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class MapSize {

	public static void main(String[] args) {
		PhysicalGameState pgs = null;
        
		try {
			pgs = PhysicalGameState.load(args[0], new UnitTypeTable());
		} catch (Exception e) {
			System.err.println("Error while loading map from file: " + args[0]);
			e.printStackTrace();
		}
		
		System.out.println(String.format("Width x height of %s is %dx%d",args[0], pgs.getWidth(), pgs.getHeight()));
	}

}
