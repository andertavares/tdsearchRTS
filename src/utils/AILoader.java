package utils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import rts.units.UnitTypeTable;

public class AILoader {

	/**
	 * Loads an {@link AI} according to its name, using the provided UnitTypeTable.
	 * @param aiName
	 * @param types
	 * @return
	 * @throws Exception if unable to instantiate the AI instance
	 * TODO create encapsulating classes for these custom AIs
	 */
	public static AI loadAI(String aiName, UnitTypeTable types) throws Exception {
		AI ai;
		
		Logger logger = LogManager.getRootLogger();
		logger.info("Loading {}", aiName);
		
		// treat a generic case
		Constructor<?> cons1 = Class.forName(aiName).getConstructor(UnitTypeTable.class);
		ai = (AI)cons1.newInstance(types);
		return ai;
	}

	/**
	 * Returns a list with Worker, Light, Ranged and Heavy rushes
	 * @param types
	 * @return
	 */
	public static List<AI> standardPortfolio(UnitTypeTable types) {
		List<AI> portfolio = new ArrayList<AI>();
		portfolio.add(new WorkerRush(types));
		portfolio.add(new LightRush(types));
		portfolio.add(new RangedRush(types));
		portfolio.add(new HeavyRush(types));
		return portfolio;
	}

}
