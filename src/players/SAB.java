package players;

import ai.asymmetric.SAB.SABScriptChoose;
import rts.units.UnitTypeTable;
import utils.AILoader;

/**
 * Encapsulates the creation of a standard instance of SAB
 * @author artavares
 *
 */
public class SAB extends SABScriptChoose {
	public SAB(UnitTypeTable types) {
		super(types, 200, 1, 2, AILoader.standardPortfolio(types), "SAB");
	}
}
