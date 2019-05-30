#!/bin/bash

# compiles project
ant

for map in $@; do
	echo "Launching $map";
		
	for reward in {winloss-tiebreak,victory-only,winlossdraw}; do
		echo "	Launching $reward";
		./train.sh -c config/selfplay-"$map".properties -p basic4 -d results/rewards-linear/"$reward"-"$map" \
		-r $reward -i 0 -f 2 --train_matches 5000 --search_timebudget 0 ;
		
		#tests against WR LR 
		for opp in {WorkerRush,LightRush}; do
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/rewards-linear/"$reward"-"$map" \
			-r $reward -i 0 -f 2 --train_matches 5000 --search_timebudget 0 -o ai.abstraction.$opp;
		done
	
		#tests against NAV A1N A3N PS GAB SAB
		for opp in {"ai.abstraction."{Worker,Light}"Rush","ai.mcts.naivemcts.NaiveMCTS","players."{A3N,GAB}}; do
			
			echo "		Testing against $opp";
			./test.sh -c config/selfplay-"$map".properties -p basic4 -d results/rewards-linear/"$reward"-"$map" \
			-r $reward -i 0 -f 2 --test_matches 10 --search_timebudget 0 -o $opp;
		done
	
	
	done 
done

# configures classpath and runs
classpath=.:bin:lib/*

echo "Launching experiment..."

#java -classpath $classpath -Djava.library.path=lib/ main.Train "$@" 

echo "Done."
