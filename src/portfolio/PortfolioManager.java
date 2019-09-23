package portfolio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;

import ai.abstraction.HeavyDefense;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightDefense;
import ai.abstraction.LightRush;
import ai.abstraction.RangedDefense;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerDefense;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import rts.units.UnitTypeTable;

/**
 * Makes it easier to retrieve a portfolio of AIs
 * @author anderson
 *
 */
public class PortfolioManager {
	
	/**
	 * Returns the portfolio with 10 basic AIs
	 * @return
	 */
	public static Map<String,AI> basicPortfolio(UnitTypeTable types){
		Map<String,AI> basicPortfolio = new HashMap<String, AI>();
		// rush scripts
		basicPortfolio.put("WR", new WorkerRush(types));
		basicPortfolio.put("LR", new LightRush(types));
		basicPortfolio.put("RR", new RangedRush(types));
		basicPortfolio.put("HR", new HeavyRush(types));
		
		// defense scripts
		basicPortfolio.put("WD", new WorkerDefense(types));
		basicPortfolio.put("LD", new LightDefense(types));
		basicPortfolio.put("RD", new RangedDefense(types));
		basicPortfolio.put("HD", new HeavyDefense(types));
		
		// support scripts
		basicPortfolio.put("BB", new BuildBase(types));
		basicPortfolio.put("BK", new BuildBarracks(types));
		//fullPortfolio.put("EconomyRush", new EconomyRush(types));
		
		return basicPortfolio;
	}
	
	/**
	 * Receives a list with the names of AIs and return the corresponding portfolio,
	 * which maps the names themselves to the AI objects. The names must be a subset of the basic portfolio.
	 * @param types
	 * @param memberNames
	 * @return
	 */
	public static Map<String,AI> getPortfolio(UnitTypeTable types, List<String> memberNames){
		Map<String,AI> portfolio = new HashMap<String, AI>();
		Map<String,AI> basicPortfolio = basicPortfolio(types);

		// adds the members found in memberNames from the basicPortfolio to the returned portfolio
		for(String name : memberNames) {
			name = name.trim(); // removes leading and trailing whitespaces 
			try {
				portfolio.put(name, basicPortfolio.get(name));
			}
			catch (NullPointerException e) { //tried to get non-existing portfolio member
				LogManager.getRootLogger().error("Unrecognized portfolio member {}", name, e);
			}
		}
		
		return portfolio;
	}
}
