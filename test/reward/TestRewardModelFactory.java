package reward;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TestRewardModelFactory {

	@Test
	void testGetRewardModel() {
	
		assertTrue(
			RewardModelFactory.getRewardModel("victory-only", 3000) instanceof VictoryOnly
		);
		assertTrue(
			RewardModelFactory.getRewardModel("VictoryOnly", 3000) instanceof VictoryOnly
		);
		
		assertTrue(
			RewardModelFactory.getRewardModel("winloss-tiebreak", 3000) instanceof WinLossTiesBroken
		);
		assertTrue(
			RewardModelFactory.getRewardModel("WinLossTiesBroken", 3000) instanceof WinLossTiesBroken
		);
		
		assertTrue(
			RewardModelFactory.getRewardModel("winlossdraw", 3000) instanceof WinLossDraw
		);
		
		assertTrue(
			RewardModelFactory.getRewardModel("winlossduration", 3000) instanceof WinLossDuration
		);
	}

}
