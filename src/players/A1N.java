package players;

import ai.RandomBiasedAI;
import ai.CMAB.CmabNaiveMCTS;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;
import utils.AILoader;

/**
 * Encapsulates the creation of a standard instance of A1N
 * @author artavares
 *
 */
public class A1N extends CmabNaiveMCTS {

	public A1N (UnitTypeTable types) {
		super (
			100, -1, 50, 10, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(types),
			new SimpleSqrtEvaluationFunction3(), true, "CmabCombinatorialGenerator", 
			types, AILoader.standardPortfolio(types), "A1N"
        );
	}
}
