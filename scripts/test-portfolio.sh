#!/bin/bash

# compiles project
ant

for map in $@; do
	echo "Launching $map";

	for p in {4,6,8,10}; do
		echo "Launching basic$p";
			
		./train.sh -c config/selfplay-"$map".properties -p "basic"$p -d results/portfolio-winlossdraw/"basic$p"-"$map" \
		-r winlossdraw -i 0 -f 2 --train_matches 5000 --search_timebudget 0 ;
	
		#tests against WR LR NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p "basic"$p -d results/portfolio-winlossdraw/"basic$p"-"$map" \
			-r winlossdraw -i 0 -f 2 -m 10 --search_timebudget 0 -o $opp;
		done
	done
done

# view results
for p in {4,6,8,10}; do
	
	# traverses opponents
	for opp in {{Worker,Light}"Rush",NaiveMCTS,A3N,GAB}; do
			
		echo "Opponent: $opp";
		
		for map in $@; do
			python3 analysis/average_score.py results/portfolio-winlossdraw/"basic$p"-"$map" -i 0 -f 2 -o $opp
		done
	done
done
