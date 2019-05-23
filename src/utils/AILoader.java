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
		
		// treats special cases (constructors with many different parameters)
		if(aiName.equalsIgnoreCase("GAB")) {
			//return new GABScriptChoose(types, 200, 1, 2, standardPortfolio(types), "GAB");	
		}
		else if (aiName.equalsIgnoreCase("SAB")) {
			//return new SABScriptChoose(types, 200, 1, 2, standardPortfolio(types), "SAB");
		}
		else if (aiName.equalsIgnoreCase("A1N")) {
			/*return new CmabNaiveMCTS(100, -1, 50, 10, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(types),
	        	new SimpleSqrtEvaluationFunction3(), true, "CmabCombinatorialGenerator", 
	        	types, standardPortfolio(types), "A1N");
	        	*/
		}
		else if (aiName.equalsIgnoreCase("A2N")) {
			/*return new CMABBuilder(100, -1, 100, 2, 0, new RandomBiasedAI(utt), 
			 * new SimpleSqrtEvaluationFunction3(), 0, utt, new ArrayList<AI>(), "CmabAsyReduzedGenerator");
	        	*/
		}
		else if(aiName.equalsIgnoreCase("A3N")) {
			// this is not the standard portfolio: does not contain WorkerRush
			List<AI> portfolio = new ArrayList<AI>();
			portfolio.add(new LightRush(types));
			portfolio.add(new RangedRush(types));
			portfolio.add(new HeavyRush(types)); 
			/*
			return new CmabAssymetricMCTS(100, -1, 100, 8, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(utt),
                new SimpleSqrtEvaluationFunction3(), true, utt, "ManagerClosestEnemy", 1,
                port2, "A3N");
					                        */
		}
		else if(aiName.equalsIgnoreCase("Tiamat")) {
			
		}
		
		else if(aiName.equalsIgnoreCase("Capivara") || aiName.equalsIgnoreCase("CapivaraBot")) {
			
		}
		
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
	private static List<AI> standardPortfolio(UnitTypeTable types) {
		List<AI> portfolio = new ArrayList<AI>();
		portfolio.add(new WorkerRush(types));
		portfolio.add(new LightRush(types));
		portfolio.add(new RangedRush(types));
		portfolio.add(new HeavyRush(types));
		return portfolio;
	}

}
