package utils;

import java.io.IOException;

import org.jdom.JDOMException;

import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

/**
 * Calculates cycles according to the map size
 * @author anderson
 *
 */
public class CyclesCalculator {
	
	/**
	 * Calculates the match duration (#frames) according to the map width
	 * (possible to-do shouldn't it be according to the area?) 
	 * @param mapLocation
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 * @throws Exception
	 */
	public static int calculate(String mapLocation) throws JDOMException, IOException, Exception {
		PhysicalGameState map = PhysicalGameState.load(mapLocation, new UnitTypeTable());
		
		// determines the matchDuration according to the map width
        int matchDuration = 12000;
        if (map.getWidth() <= 64) {
            matchDuration = 8000;
        }
        if (map.getWidth() <= 32) {
            matchDuration = 6000;
        }
        if (map.getWidth() <= 24) {
            matchDuration = 5000;
        }
        if (map.getWidth() <= 16) {
            matchDuration = 4000;
        }
        if (map.getWidth() <= 8) {
            matchDuration = 3000;
        } 
		
        return matchDuration;

	}

}
