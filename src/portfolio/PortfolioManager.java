package portfolio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.abstraction.EconomyRush;
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
		Map<String,AI> fullPortfolio = new HashMap<String, AI>();
		// rush scripts
		fullPortfolio.put("WorkerRush", new WorkerRush(types));
		fullPortfolio.put("LightRush", new LightRush(types));
		fullPortfolio.put("RangedRush", new RangedRush(types));
		fullPortfolio.put("HeavyRush", new HeavyRush(types));
		
		// defense scripts
		fullPortfolio.put("WorkerDefense", new WorkerDefense(types));
		fullPortfolio.put("LightDefense", new LightDefense(types));
		fullPortfolio.put("RangedDefense", new RangedDefense(types));
		fullPortfolio.put("HeavyDefense", new HeavyDefense(types));
		
		// support scripts
		fullPortfolio.put("BuildBase", new BuildBase(types));
		fullPortfolio.put("BuildBarracks", new BuildBarracks(types));
		//fullPortfolio.put("EconomyRush", new EconomyRush(types));
		return fullPortfolio;
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

		// adds the members found in memberNames from the fullPortfolio to the returned portfolio
		for(String name : memberNames) {
			portfolio.put(name, basicPortfolio.get(name));
		}
		
		return portfolio;
	}
}
