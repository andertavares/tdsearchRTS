#!/bin/bash

# Parameters: repNum and train matches

# compiles project
ant

for map in {basesWorkers8x8,NoWhereToRun9x8}; do
	echo "Launching $map";

	for interval in {1,10,25,50}; do
		echo "    Launching interval = $interval";
			
		./train.sh -c config/selfplay-"$map".properties -d results/"$map"/sall/i"$interval"/m"$2" \
		-i $1 -f $1 --train_matches $2 --decision_interval $interval -s all;
	
		
		echo "		Testing against A3N";
		./alternating_test.sh  -c config/selfplay-"$map".properties -d results/"$map"/sall/i"$interval"/m"$2" \
		-i $1 -f $1 --test_matches 40 --decision_interval $interval -s all -o players.A3N;
	done
done

