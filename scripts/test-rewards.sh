#!/bin/bash

# compiles project
ant

for map in $@; do
	echo "Launching $map";
		
	for reward in {winloss-tiebreak,victory-only,winlossdraw}; do
		echo "	Launching $reward";
		./train.sh -c config/selfplay-"$map".properties -p basic4 -d results/rewards-linear/"$reward"-"$map" \
		-r $reward -i 0 -f 2 --train_matches 5000 --search_timebudget 0  ;
		
		#tests against WR LR NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/rewards-linear/"$reward"-"$map" \
			-r $reward -i 0 -f 2 --test_matches 10 --search_timebudget 0 -o $opp;
		done
	done 
done

# view results
for reward in {winloss-tiebreak,victory-only,winlossdraw}; do
	./scripts/average-score-all-opponents.sh results/alpha-winlossdraw/ 0 2
done

