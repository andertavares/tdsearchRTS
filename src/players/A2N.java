package players;

import java.util.ArrayList;

import ai.RandomBiasedAI;
import ai.CMAB.CMABBuilder;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;

/**
 * Encapsulates the creation of a standard instance of A2N
 * @author artavares
 *
 */
public class A2N extends CMABBuilder {
	public A2N(UnitTypeTable types) {
		super(
			100, -1, 100, 2, 0, new RandomBiasedAI(types), 
			new SimpleSqrtEvaluationFunction3(), 0, types, new ArrayList<AI>(), 
			"CmabAsyReduzedGenerator"
		);
	}
}
