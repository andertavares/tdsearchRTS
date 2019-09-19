package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class StrategyNames {
	
	/**
     * A map from acronyms to actual class names inside A3N original project
     */
    @SuppressWarnings("serial")
	public final static Map<String,String> selectionStrategyNames = new HashMap<>() {{
    	put("CC", "ManagerClosest");
    	put("CE", "ManagerClosestEnemy");
    	put("FC", "ManagerFather");
    	put("FE", "ManagerFartherEnemy");
    	put("AV-", "ManagerLessDPS");
    	put("AV+", "ManagerMoreDPS");
    	put("HP-", "ManagerLessLife");
    	put("HP+", "ManagerMoreLife");
    	put("R", "ManagerRandom");
    	put("M", "ManagerUnitsMelee");
    }};
    
    /**
     * Convert one acronym to a name
     * @param acronym
     * @return
     */
    public static String acronymToName(String acronym) {
    	if (!selectionStrategyNames.containsKey(acronym)) {
    		throw new IllegalArgumentException(acronym + " not found in selection strategies.");
    	}
    	return selectionStrategyNames.get(acronym);
    }
    
    /**
     * Returns a list with names corresponding to all acronyms in the received list
     * @param acronyms
     * @return
     */
    public static List<String> acronymsToNames(List<String> acronyms) {
    	List<String> names = new Vector<>(acronyms.size());
    	
    	for (String acronym : acronyms) {
    		names.add(acronymToName(acronym));
    	}
    	
    	return names;
    }

}
