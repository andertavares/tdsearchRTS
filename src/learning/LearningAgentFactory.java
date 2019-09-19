package learning;

import java.util.Properties;

import rts.units.UnitTypeTable;

public class LearningAgentFactory {
	public static LearningAgent getLearningAgent(String name, UnitTypeTable types, Properties config, int randomSeed) {
		if(name.equalsIgnoreCase("sarsa")) {
        	return new LinearSarsaLambda(types, config, randomSeed);
        }
        else if (name.equals("qlearning")) {
        	 return new LinearQLearning(types, config, randomSeed);
        }
        else {
        	throw new IllegalArgumentException("LearningAgent '" + name + "' not found.");
        }
	}
}
