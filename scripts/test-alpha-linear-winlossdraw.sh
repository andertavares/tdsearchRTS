#!/bin/bash

# compiles project
ant

for map in $@; do
	echo "Launching $map";

	for alpha in {0.001,0.01,0.1,0.3,0.7,1}; do
		echo "Launching $alpha";
			
		./train.sh -c config/selfplay-"$map".properties -p basic4 -d results/alpha-winlossdraw/"$alpha"-"$map" \
		-r winlossdraw -i 0 -f 2 --train_matches 5000 --search_timebudget 0 --td_alpha_initial $alpha;
	
		#tests against WR LR NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/alpha-winlossdraw/"$alpha"-"$map" \
			-r winlossdraw -i 0 -f 2 -m 10 --search_timebudget 0 -o $opp;
		done
	done
done

./view-results-alpha.sh
