package learner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jdom.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import learning.LinearSarsaLambda;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import utils.StrategyNames;

class TestLinearSarsaLambda {

	LinearSarsaLambda learner;
	MockupFeatureExtractor testFeatureExtractor;
	MockupRewardModel testRewardModel;
	UnitTypeTable types;
	double alpha, gamma, lambda;
	
	@BeforeEach
	void setUp() throws Exception {
		types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		testFeatureExtractor = new MockupFeatureExtractor(new double[] {1.0, 0.5});
		testRewardModel = new MockupRewardModel(0.1, 0);
		alpha = 0.01;
		gamma = 0.9;
		lambda = 0.1;
		
		learner = new LinearSarsaLambda(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList("WR,LR,HR,RR".split(",")), 
			alpha, 0.1, gamma, lambda, 0
		);
	}
	
	@Test
	/**
	 * LinearSarsaLambda raised an exception when instantiated from config without random seed
	 */
	void testCreationFromConfigWithoutRandomSeed() {
		Properties config = new Properties();
		config.put("max_cycles", "200");
		config.put("rewards", "winlossdraw");
		config.put("features", "materialdistancehp");
		config.put("portfolio", "WR,LR,HR,RR"); 
		config.put("td.alpha.initial", "0.15");
		config.put("td.epsilon.initial", "0.1");
		config.put("td.gamma", "0.99");
		config.put("td.lambda", "0.15");
		learner = new LinearSarsaLambda(types, config);
		// if code reaches here without throwing an exception, we're good to go
	}
	
	@Test
	void testQValue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		
		setLearnerWeights(testWeights);
		
		//feature vector is [1.0, 0.5], weights for 'action1' are: [0.3, 0.1], expected Q-value is: 0.3 + 0.05 = 0.35
		assertEquals(0.35, learner.qValue(new double[] {1.0, 0.5}, "action1"));
		
	}
	
	@Test
	void testTDTarget() throws Exception {
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		setLearnerWeights(testWeights);
		
		//testRewardModel.setValues(0.1, 1.0);
		
		GameState nextState = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		); 
		
		// td target is r + gamma * q(s', a') -- q(s',a') is 0.35 (as per the previous test)
		assertEquals(0.1 + 0.9*0.35, learner.tdTarget(0.1, nextState, 0, "action1", false));
		
		// now let's suppose the reached state is a gameover state
		nextState = new GameState(new PhysicalGameState(8, 8), types); //empty 8x8 physical game state is at gameover
		assertEquals(1.0, learner.tdTarget(1.0, nextState, 0, "action1", true));
	}

	@Test 
	void testTDLambdaUpdateRule() throws JDOMException, IOException, Exception{
		// puts a custom set of weights into the learner
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
			put("action2", new double[] {0.7, 0.2});
		}};
		setLearnerWeights(testWeights);
		
		// creates fake" eligibility traces (all zeros) 
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {0, 0});
			put("action2", new double[] {0, 0});
		}};
		setLearnerEligibility(eligibility);
		
		GameState previousState = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		); 
		
		GameState nextState = previousState.clone();
		
		// previousQ should be  1.0*0.3 + 0.5*0.1 = 0.35
		double previousQ = learner.qValue(new double[] {1.0, 0.5}, "action1");
		assertEquals(0.35, previousQ);
		
		// nextQ should be  1.0*0.4 + 0.5*0.2 = 0.8
		double nextQ = learner.qValue(new double[] {1.0, 0.5}, "action2");
		assertEquals(0.8, nextQ, 1E-5);
		
		// tdTarget should be r+gamma * nextQ = 0.1 + 0.9*0.8 = 0.82
		double tdTarget = learner.tdTarget(0.1, nextState, 0, "action2", false);
		assertEquals(0.82, tdTarget);
		
		double tdError =  tdTarget - previousQ; // (0.82-0.35) = 0.47
		
		// tests the update without eligibility
		learner.tdLambdaUpdateRule(previousState, 0, "action1", tdError);
		
		// tests if eligibility has changed (increased by the feature vector & decayed by gamma*lambda)
		assertArrayEquals(
			new double[] {1*gamma*lambda, 0.5*gamma*lambda}, 
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {0, 0}, 
			eligibility.get("action2")
		);
		
		
		// checks the weight vector for action1 
		double[] newWeightA1 = new double[] {0.3 + alpha*tdError, 0.1 + alpha*tdError*0.5};
		assertArrayEquals(
			newWeightA1, // update rule: w_i = w_i + alpha*error*e_i
			testWeights.get("action1")
		);
		
		// checks the weight vector for action2 (expected to be unchanged)
		assertArrayEquals(
			new double[] {0.7, 0.2}, 
			testWeights.get("action2")
		);
		
		// checks the q value
		assertEquals(
			newWeightA1[0] * 1.0 + newWeightA1[1]*0.5,
			learner.qValue(new double[] {1.0, 0.5}, "action1")
		);
		
	}
	
	@Test 
	void testTDLambdaUpdateRuleWithEligibility() throws JDOMException, IOException, Exception{
		/**
		 * Let's simulate a tabular problem with two states: s0 and s1. 
		 * We start with the following condition: agent was in s0, did action1, reached s1. 
		 * Then it did action2 in s1, reaching s0, receiving reward +10. Then it chose action2. 
		 * This reward should affect action2 in s1 and action1 in s0 (not action2 in s0 because it 
		 * was not performed yet)
		 */
		
		// The feature vector is one-hot encoded
		double[][] features = new double[][] {
			{1, 0}, //s0
			{0, 1}  //s1
		};
		
		// let's put a custom set of weights into the learner
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		// let's start with these eligibility traces (simulating that agent has done action1 in s0) 
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {lambda*gamma, 0});
			put("action2", new double[] {0, 0});
		}};
		setLearnerEligibility(eligibility);
		
		// maintains a copy of the e-traces because it will change in the td update 
		// did a manual re-instantiation with the same values as above 'coz java won't allow a deep copy easily
		@SuppressWarnings("serial")
		Map<String, double[]> oldEligibility = new HashMap<>() {{
			put("action1", new double[] {lambda*gamma, 0});
			put("action2", new double[] {0, 0});
		}};
		
		// let's say the new state is as follows
		testFeatureExtractor.setFeatureVector(features[1]); 
		
		GameState previousState = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		); 
		
		GameState nextState = previousState.clone();
		
		//let's say action2 was taken, 
		
		// previousQ(s1,a2) should 0*4 + 1*-1 = -1
		double previousQ = learner.qValue(features[1], "action2");
		assertEquals(-1, previousQ);
		
		// nextQ(s0,a2) should be  1.0*4 + 0*-1 = 4
		double nextQ = learner.qValue(features[0], "action2");
		assertEquals(4, nextQ);
		
		// tdTarget should be r+gamma * nextQ 
		testRewardModel.setValues(10, 0);	// sets the reward
		testFeatureExtractor.setFeatureVector(features[0]); //sets the next state (s0)
		double tdTarget = learner.tdTarget(10, nextState, 0, "action2", false);
		assertEquals(10 + gamma*nextQ, tdTarget);
		
		double tdError =  tdTarget - previousQ; 
		
		// tests the update of action2 in s1
		testFeatureExtractor.setFeatureVector(features[1]); //sets the state to s1
		learner.tdLambdaUpdateRule(previousState, 0, "action2", tdError);
		
		// tests if eligibility has changed (decayed for action1)
		double[] oldEligAction1 = oldEligibility.get("action1");
		assertArrayEquals(
			new double[] {oldEligAction1[0] * gamma*lambda, oldEligAction1[1]*gamma*lambda}, //the initial was lambda*gamma and it decays by lambda*gamma 
			eligibility.get("action1")
		);
		assertArrayEquals( // (increased by the feature vector & decayed by gamma*lambda) for action2
			new double[] {0, 1 * gamma * lambda}, 
			eligibility.get("action2")
		);
		
		
		// --- tests the weight vector ---
		
		
		// checks the weight vector for action1 it was w=[1, 2], only the first component will be affected (action1 in s0)
		// each w_i is updated as: w_i = w_i + alpha*error*e_i, where e_i is the eligibility trace before decay
		double[] newWeightA1 = new double[] {1 + alpha*tdError*oldEligAction1[0], 2}; //the second component should not change
		assertArrayEquals(
			newWeightA1, 
			testWeights.get("action1")
		);
		
		// checks the weight vector for action2 it was w=[4, -1]; only the second component will be affected (action2 in s1)
		double[] newWeightA2 = new double[] {4, -1 + alpha*tdError};
		assertArrayEquals(
			newWeightA2, 
			testWeights.get("action2")
		);
		
		// ---- checks the q-values
		// q(s0, a1)
		assertEquals(
			newWeightA1[0] * features[0][0] + newWeightA1[1]*features[0][1],
			learner.qValue(features[0], "action1")
		);
		
		// q(s1, a1)
		assertEquals(
			newWeightA1[0] * features[1][0] + newWeightA1[1]*features[1][1],
			learner.qValue(features[1], "action1")
		);
		
		// q(s0, a2)
		assertEquals(
			newWeightA2[0] * features[0][0] + newWeightA2[1]*features[0][1],
			learner.qValue(features[0], "action2")
		);
		
		// q(s1, a2)
		assertEquals(
			newWeightA2[0] * features[1][0] + newWeightA2[1]*features[1][1],
			learner.qValue(features[1], "action2")
		);
		
	}
	
	@Test
	void testGreedyChoice() throws NoSuchFieldException, IllegalAccessException {
		// creates two 'foo' game states that will be mapped to different feature vectors
		GameState s0 = new GameState(new PhysicalGameState(0, 0), types);
		GameState s1 = new GameState(new PhysicalGameState(3, 3), types);
		
		// adds the game states to one-hot feature encoding
		testFeatureExtractor.putMapping(s0, new double[] {1, 0});
		testFeatureExtractor.putMapping(s1, new double[] {0, 1});
		
		// custom set of weights: prefer action2 in s0 and action1 in s1
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		assertEquals("action2", learner.greedyChoice(s0, 0, testWeights));
		assertEquals("action1", learner.greedyChoice(s1, 0, testWeights));
	}
	
	@Test
	void testEpsilonGreedy() throws NoSuchFieldException, IllegalAccessException {
		// creates a 'foo' game state that will be mapped to different feature vectors
		GameState s0 = new GameState(new PhysicalGameState(0, 0), types);
		
		// adds the game states to one-hot feature encoding
		testFeatureExtractor.putMapping(s0, new double[] {1, 0});
		
		// custom set of weights: prefer action2 in s0
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		// counts the number of times that action2 was chosen in a 1M selections:
		int greedyChoice = 0;
		for(int i = 0; i < 1000000; i++) {
			if(learner.epsilonGreedy(s0, 0).equals("action2")) {
				greedyChoice++;
			}
		}
		
		// on the random choice, it is expected that the greedy action is also taken 50% of the times
		// in total, the greedy action is expected to be taken 90% + (50% * 10%) = 95%
		
		//with a tolerance of 200 choices, checks that the greedy action was taken 95% of the time
		assertEquals(950000.0, greedyChoice, 200); 
	}
	
	
	
	@Test 
	void testSarsaUpdate() throws JDOMException, IOException, Exception {
		
		// loads a physical game state that is not a game over
		PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// creates three 'foo' game states that will be mapped to different feature vectors
		// they are different in the number of units (microRTS does not differentiates states based solely on time)
		GameState s0 = new GameState(pgs, types);
		GameState s1 = s0.clone(); s1.getPhysicalGameState().addUnit(new Unit(0, types.getUnitTypes().get(0), 3, 3)); 
		GameState s2 = s1.clone(); s2.getPhysicalGameState().addUnit(new Unit(0, types.getUnitTypes().get(0), 5, 5));  
		
		// encodes the game states with one-hot encoding
		double[][] features = new double[][] {
			{1, 0, 0}, 
			{0, 1, 0},
			{0, 0, 1}
		};
		testFeatureExtractor.putMapping(s0, features[0] );
		testFeatureExtractor.putMapping(s1, features[1] );
		testFeatureExtractor.putMapping(s2, features[2] );
		
		// creates test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2, 3});
			put("action2", new double[] {4, -1, -2});
		}};
		setLearnerWeights(testWeights);
		
		// creates eligibility traces (as if the agent has just started)
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {0, 0, 0});
			put("action2", new double[] {0, 0, 0});
		}};
		setLearnerEligibility(eligibility);
		
		// sarsa tuple: s0, action2, +10, s1, action1
		testRewardModel.setValues(10, 0);
		learner.sarsaUpdate(s0, 0, "action2", 10, s1, "action1", false);
		
		// tests eligibility of the two actions (unchanged for a1, changed for a2)
		assertArrayEquals(
			new double[] {0, 0, 0},  
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {1 * gamma*lambda, 0, 0},  
			eligibility.get("action2")
		);
		
		// tests q-values of the two actions
		assertArrayEquals(
			new double[] {1, 2, 3},  //action1 is unchanged
			new double[] {
				learner.qValue(features[0], "action1"), 
				learner.qValue(features[1], "action1"),
				learner.qValue(features[2], "action1"),
			}
		);
		
		double q_s0_a2 = 4 + alpha*(10 + gamma*2 - 4); //oldQ + alpha*(r + gamma*q(s1,a1) - oldQ)
		assertArrayEquals(
			new double[] {q_s0_a2, -1, -2},  //action2 is changed on s0
			new double[] {
				learner.qValue(features[0], "action2"), 
				learner.qValue(features[1], "action2"),
				learner.qValue(features[2], "action2"),
			}
		);
		
		// next sarsa tuple: s1, action1, -100, s2, action2 
		testRewardModel.setValues(-100, 0);
		learner.sarsaUpdate(s1, 0, "action1", -100, s2, "action2", false);
		
		// tests eligibility of the two actions:
		assertArrayEquals(
			new double[] {0, gamma*lambda, 0},  
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {gamma*lambda * gamma*lambda, 0, 0}, //the initial was lambda*gamma and it decays by lambda*gamma 
			eligibility.get("action2")
		);
		
		// tests q-values of the two actions
		double tdError = -100 + gamma*-2 - 2;
		assertArrayEquals( //perhaps break this into 3 assertEquals?
			new double[] {1, 2 + alpha*(tdError), 3},  //action1 changes on s1 as oldQ + alpha*(r + gamma*q(s1,a1) - oldQ)
			new double[] {
				learner.qValue(features[0], "action1"), 
				learner.qValue(features[1], "action1"),
				learner.qValue(features[2], "action1"),
			}
		);
		
		q_s0_a2 = q_s0_a2 + alpha*tdError*gamma*lambda;
		assertArrayEquals(
			new double[] {q_s0_a2, -1, -2},  //action2 is changed on s0
			new double[] {
				learner.qValue(features[0], "action2"), 
				learner.qValue(features[1], "action2"),
				learner.qValue(features[2], "action2"),
			}
		);
		
	}
	
	
	
	@Test
	void testGameOver() throws Exception {
		// for this test, we'll use a learner with epsilon = 0, to control which action it would return
		learner = new LinearSarsaLambda(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList("WR,LR,HR,RR".split(",")),  
			alpha, 0.0, gamma, lambda, 0
		);
		
		// loads a physical game state that is not a game over
		PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// creates three 'foo' game states that will be mapped to different feature vectors
		GameState s0 = new GameState(pgs, types);
		GameState s1 = new GameState(new PhysicalGameState(0, 0), types); //this is a terminal state
		
		// encodes the game states with one-hot encoding
		double[][] features = new double[][] {
			{1, 0}, 
			{0, 1},
		};
		testFeatureExtractor.putMapping(s0, features[0]); //FIXME find a way to get the states identity...
		testFeatureExtractor.putMapping(s1, features[1]);
		
		// creates test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {5, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		// let's simulate that the agent has the following eligibility traces:
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {0.03, 0.1});
			put("action2", new double[] {0.2, 0});
		}};
		setLearnerEligibility(eligibility);
		
		//agent should take action1 in s0
		String actionInS0 = learner.act(s0, 0);
		assertEquals("action1", actionInS0);
		
		// now let's do a gameOverUpdate as if the player has won, setting the game over reward to 1
		testRewardModel.setValues(0, 1);
		learner.finish(0); //winner was player 0 (our player under test) 
		
		// tests eligibility of the two actions (increased+decayed for a1, decayed for a2)
		assertArrayEquals(
			new double[] {(0.03+1) * gamma*lambda, 0.1 * gamma*lambda},  
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {0.2 * gamma*lambda, 0},  
			eligibility.get("action2")
		);
		
		double tdError = 1 - 5; //reward - q(s0,a1)
		// tests q-values: q = q + alpha * tderror * e )
		assertEquals(5 + alpha * tdError * (0.03+1), learner.qValue(features[0], "action1"));
		assertEquals(2 + alpha * tdError * 0.1, learner.qValue(features[1], "action1"));
		
		assertEquals(4 + alpha* tdError * 0.2, learner.qValue(features[0], "action2"));
		assertEquals(-1, learner.qValue(features[1], "action2"));
	}

	@Test
	void testLoadAndSaveWeights() throws NoSuchFieldException, IllegalAccessException, IOException {
		Map<String, double[]> testWeights = new HashMap<>(); 
		testWeights.put("action1", new double[] {0.1, 2});
		testWeights.put("action2", new double[] {4, -1});
		setLearnerWeights(testWeights);
		
		learner.save("testweights.bin");
		
		// changes some of the weights and verifies the change
		testWeights.get("action1")[0] = -1000;
		assertEquals(-1000, learner.qValue(new double[] {1, 0} , "action1"));
		
		// loads the previously saved weights
		learner.load("testweights.bin");
		
		// checks that the weights have their original values via qValue
		assertEquals(0.1, learner.qValue(new double[] {1, 0} , "action1"));
		assertEquals(2, learner.qValue(new double[] {0, 1} , "action1"));
		assertEquals(4, learner.qValue(new double[] {1, 0} , "action2"));
		assertEquals(-1, learner.qValue(new double[] {0, 1} , "action2"));
		
	}
	
	/**
	 * Sets the weights of our learner object
	 * @param weights
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private void setLearnerWeights(Map<String, double[]> weights) throws NoSuchFieldException, IllegalAccessException {
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = LinearSarsaLambda.class.getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, weights);
		assertEquals(learner.getWeights(), weights);
	}
	
	private void setLearnerEligibility(Map<String, double[]> elig) throws NoSuchFieldException, IllegalAccessException {
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = LinearSarsaLambda.class.getDeclaredField("eligibility");
		field.setAccessible(true);
		field.set(learner, elig);
		//assertEquals(learner.getEligibility(), elig);
	}

}
