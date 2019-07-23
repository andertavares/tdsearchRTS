package reward;

public class RewardModelFactory {
	public static RewardModel getRewardModel(String modelName, int maxGameCycles) {
		if(modelName.equals("victory-only") || modelName.equalsIgnoreCase("VictoryOnly")) {
        	return new VictoryOnly();
        }
        else if (modelName.equals("winloss-tiebreak") || modelName.equalsIgnoreCase("WinLossTiesBroken")) {
        	 return new WinLossTiesBroken(maxGameCycles);
        }
        else if (modelName.equals("winlossdraw")  || modelName.equalsIgnoreCase("WinLossDraw")) {
        	return new WinLossDraw(maxGameCycles);
        }
        else {
        	throw new IllegalArgumentException("Reward model '" + modelName + "' not found.");
        }
	}
}
