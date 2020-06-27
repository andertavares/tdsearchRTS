package reward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestWinLossDraw {

	@Test
	void testReward() throws Exception {
		
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		GameState state = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		);
		
		WinLossDraw model = new WinLossDraw(3000);
		// non-terminal rewards should be 0 for both players 
		assertEquals(0, model.reward(state, 0));
		assertEquals(0, model.reward(state, 1));
		
		//tests the last non-terminal state, reward should still be 0
		setGameTime(state, 2999);
		assertEquals(0, model.reward(state, 0));
		assertEquals(0, model.reward(state, 1));
		
		//tests the terminal state on timeout, reward should still be 0 (draw)
		setGameTime(state, 3000);
		assertEquals(0, model.reward(state, 0));
		assertEquals(0, model.reward(state, 1));
		
	}
	
	@Test
	void testGameOverReward() throws Exception {
		//TODO test the actual gameOverReward method@
		
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		// loads two terminal states with victories for each player
		GameState p0Wins = GameState.fromXML("test/reward/p0wins.xml", types);
		GameState p1Wins = GameState.fromXML("test/reward/p1wins.xml", types);
		
		WinLossDraw model = new WinLossDraw(3000);
		// players get 1/-1 for win/loss regardless of the time
		
		setGameTime(p0Wins, 1000); 
		assertEquals(1, model.reward(p0Wins, 0), 1E-10);
		assertEquals(1, model.reward(p0Wins, 0), 1E-10);
		assertEquals(-1, model.reward(p0Wins, 1), 1E-10);
		// signals flipped for p1wins
		setGameTime(p1Wins, 1000); 
		assertEquals(-1, model.reward(p1Wins, 0), 1E-10);
		assertEquals(1, model.reward(p1Wins, 1), 1E-10);
		
		
		// at half duration, p0 gets 1/2 and p1 gets -1/2 on p0wins
		setGameTime(p0Wins, 1500); 
		assertEquals(1, model.reward(p0Wins, 0), 1E-10);
		assertEquals(-1, model.reward(p0Wins, 1), 1E-10);
		// flipping signals for p1wins
		setGameTime(p1Wins, 1500); 
		assertEquals(-1, model.reward(p1Wins, 0), 1E-10);
		assertEquals(1, model.reward(p1Wins, 1), 1E-10);
		
		// p0 won at the but-last frame
		setGameTime(p0Wins, 2999); 
		assertEquals(1, model.reward(p0Wins, 0), 1E-10);
		assertEquals(-1, model.reward(p0Wins, 1), 1E-10);
		// p1 won at the but-last frame
		setGameTime(p1Wins, 2999); 
		assertEquals(-1, model.reward(p1Wins, 0), 1E-10);
		assertEquals(1, model.reward(p1Wins, 1), 1E-10);
		
		//at max duration
		setGameTime(p0Wins, 3000);
		assertEquals(1, model.reward(p0Wins, 0), 1E-10);
		assertEquals(-1, model.reward(p0Wins, 1), 1E-10);
		
		setGameTime(p1Wins, 3000);
		assertEquals(-1, model.reward(p1Wins, 0), 1E-10);
		assertEquals(1, model.reward(p1Wins, 1), 1E-10);
	}
	
	private void setGameTime(GameState state, int time) throws NoSuchFieldException, IllegalAccessException {
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = GameState.class.getDeclaredField("time");
		field.setAccessible(true);
		field.set(state, time);
		assert state.getTime() == time;
	}

}
