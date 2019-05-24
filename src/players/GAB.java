package players;

import ai.asymmetric.GAB.SandBox.GABScriptChoose;
import rts.units.UnitTypeTable;
import utils.AILoader;

/**
 * Encapsulates the creation of a standard instance of GAB
 * @author artavares
 *
 */
public class GAB extends GABScriptChoose {
	public GAB(UnitTypeTable types) {
		super(types, 200, 1, 2, AILoader.standardPortfolio(types), "GAB");
	}
}
