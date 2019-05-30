#!/bin/bash

# compiles project
ant

for alpha in {0.001,0.01,0.1,0.3,0.7,1}; do
	echo "Launching $alpha";
		
	for map in {"basesWorkers8x8","basesWorkers16x16A","basesWorkers32x32A","(4)BloodBath.scmB","(4)Andromeda.scxE"}; do
		echo "	Launching $map";
		./train.sh -c config/selfplay-"$map".properties -p basic4 -d results/alpha-winlossdraw/"$alpha"-"$map" \
		-r winlossdraw -i 0 -f 2 --train_matches 5000 --search_timebudget 0 ;
	
		#tests against WR LR NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/alpha-winlossdraw/"$alpha"-"$map" \
			-r winlossdraw -i 0 -f 2 -m 10 --search_timebudget 0 ;
		done
	done 
done

# view results
for alpha in {0.001,0.01,0.1,0.3,0.7,1}; do
	./scripts/average-score-all-opponents.sh results/alpha-winlossdraw/ 0 2
done
