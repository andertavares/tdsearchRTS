package utils;

import java.lang.reflect.Constructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import rts.units.UnitTypeTable;

public class AILoader {

	/**
	 * Loads an {@link AI} according to its name, using the provided UnitTypeTable.
	 * @param aiName
	 * @param types
	 * @return
	 * @throws Exception if unable to instantiate the AI instance
	 */
	public static AI loadAI(String aiName, UnitTypeTable types) throws Exception {
		AI ai;
		
		Logger logger = LogManager.getRootLogger();
		logger.info("Loading {}", aiName);
		
		Constructor<?> cons1 = Class.forName(aiName).getConstructor(UnitTypeTable.class);
		ai = (AI)cons1.newInstance(types);
		return ai;
	}

}
