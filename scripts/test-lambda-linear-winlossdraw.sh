#!/bin/bash

# compiles project
ant

for map in $@; do
	for lambda in {0.0,0.1,0.3,0.7,1}; do
		echo "Launching $lambda";
			
		echo "	Launching $map";
		./train.sh -c config/selfplay-"$map".properties -p basic4 -d results/lambda-winlossdraw/"$lambda"-"$map" \
		-r winlossdraw -i 0 -f 2 --train_matches 5000 --search_timebudget 0 --td_lambda $lambda ;
	
		#tests against WR LR NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/lambda-winlossdraw/"$lambda"-"$map" \
			-r winlossdraw -i 0 -f 2 -m 10 --search_timebudget 0 ;
		done
	done
done

# view results
for lambda in {0.0,0.1,0.3,0.7,1}; do
	for map in $@; do
		./scripts/average-score-all-opponents.sh results/lambda-winlossdraw/"$lambda"-"$map" 0 2
	done
done
