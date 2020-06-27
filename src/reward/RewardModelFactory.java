package reward;

public class RewardModelFactory {
	public static RewardModel getRewardModel(String modelName, int maxGameCycles) {
		switch (modelName.toLowerCase()) { //no 'break' because all cases return a value
		
		case "victory-only":
		case "victoryonly":
			return new VictoryOnly();
			
		case "winloss-tiebreak":
		case "winlosstiesbroken":
			return new WinLossTiesBroken(maxGameCycles);
		
		case "winlossdraw":
			return new WinLossDraw(maxGameCycles);
		
		case "winlossduration":
			return new WinLossDuration(maxGameCycles);
        default:
        	throw new IllegalArgumentException("Reward model '" + modelName + "' not found.");
		}
	}
}
