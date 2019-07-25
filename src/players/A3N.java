package players;

import java.util.Arrays;

import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.competition.capivara.CmabAssymetricMCTS;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;

/**
 * Encapsulates the creation of a standard instance of A3N
 * @author artavares
 *
 */
public class A3N extends CmabAssymetricMCTS {
	
	
	/**
	 * Instantiates A3N by calling the superclass constructor
	 * with appropriate parameters
	 * @param types
	 */
	public A3N (UnitTypeTable types) {
		super(
			100, -1, 100, 8, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(types),
            new SimpleSqrtEvaluationFunction3(), true, types, "ManagerClosestEnemy", 1, 
            Arrays.asList(new LightRush(types), new RangedRush(types), new HeavyRush(types)),
            "A3N"
        );
	}
}
