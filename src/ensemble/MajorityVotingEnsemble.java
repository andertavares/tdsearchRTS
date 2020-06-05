package ensemble;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;

import ai.core.AI;
import ai.core.ParameterSpecification;
import learning.LearningAgent;
import learning.LinearSarsaLambda;
import portfolio.PortfolioManager;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MajorityVotingEnsemble extends AI{

	/**
	 * Stores each policy and its name
	 */
	private Map<String, LearningAgent> policies;
	
	/**
	 * Portfolio of AIs
	 */
	Map<String, AI> portfolio;
	
	/**
	 * Stored to aid the creation of SarsaSearch AIs
	 */
	private UnitTypeTable types;
	private Properties config;
	
	public MajorityVotingEnsemble(UnitTypeTable types, Properties config) {
		this.types = types;
		this.config = config;
		policies = new HashMap<>();
		portfolio = PortfolioManager.fullPortfolio(types);
		
		/*
		String[] paths = config.getProperty("policy.paths").split(",");
		String[] names = config.getProperty("policy.names").split(",");
		
		if (paths.length != names.length) {
			throw new RuntimeException("Names and policy paths have differing lengths");
		}
		
		for (int i = 0; i < paths.length; i++) {
			addSarsaPolicy(names[i], paths[i]);
		}
		*/
	}
	
	public void addSarsaPolicy(Properties sarsaConfig, String name, String path)  {
		
		//creates a dummy config with zero alpha & epsilon
		Properties dummyConfig = (Properties) sarsaConfig.clone();
		dummyConfig.put("td.alpha.initial", "0");
		dummyConfig.put("td.epsilon.initial", "0");
		
		// creates the learning agent w/ dummyConfig, loads its policy and adds it
		LinearSarsaLambda policy = new LinearSarsaLambda(types, dummyConfig, policies.size());
		try {
			policy.load(path);
		} catch (IOException e) {
			LogManager.getRootLogger().error("Unable to load policy " + path + " using random weights.", e);
		}
		addPolicy(name, policy);
	}
	
	public void addPolicy(String name, LearningAgent agent) {
		policies.put(name, agent);
	}
	
	@Override
	public AI clone() {
		MajorityVotingEnsemble newEnsemble = new MajorityVotingEnsemble(types, config);
		
		for(Entry<String, LearningAgent> entry : policies.entrySet()) {
			// won't bother with cloning the LearningAgent
			newEnsemble.addPolicy(entry.getKey(), entry.getValue());
		}
		return newEnsemble;
	}

	@Override
	public PlayerAction getAction(int player, GameState state) throws Exception {
		Map<String, Integer> votes = new HashMap<String, Integer>();
		String mostVoted = null;
		int mostVotes = -1;
		
		// traverses the list of actors to collect the votes
		for(LearningAgent actor : policies.values()) {
			String vote = actor.act(state, player);
			votes.put(vote, votes.getOrDefault(vote, 0) + 1);
			
			// change the winner if necessary
			if (votes.get(vote) > mostVotes) {
				mostVoted = vote;
				mostVotes = votes.get(vote);
			}
		}
		LogManager.getRootLogger().debug("Most voted: {} with {} votes", mostVoted, mostVotes);
		return portfolio.get(mostVoted).getAction(player, state);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset(UnitTypeTable utt) {
    	for(AI ai : portfolio.values()){
    		ai.reset(utt);
    	}
    }
	
	@Override
    public void reset() {
    	for(AI ai : portfolio.values()){
    		ai.reset();
    	}
    }

}
