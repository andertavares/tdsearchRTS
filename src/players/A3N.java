package players;

import ai.RandomBiasedAI;
import ai.CMAB.CmabAssymetricMCTS;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;

/**
 * Encapsulates the creation of a standard instance of A3N
 * @author artavares
 *
 */
public class A3N extends CmabAssymetricMCTS {

	public A3N (UnitTypeTable types) {
		super(
			100, -1, 100, 8, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(types),
            new SimpleSqrtEvaluationFunction3(), true, types, "ManagerClosestEnemy", 1
        );
	}
}
