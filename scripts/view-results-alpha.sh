#!/bin/bash

echo "each line will contain the result on a map, in order: basesWorkers8x8,basesWorkers16x16A,basesWorkers32x32A,(4)BloodBath.scmB"

for alpha in {0.001,0.01,0.1,0.3,0.7,1}; do
	
	echo "---- alpha: $alpha ----";
	
	# traverses opponents
	for opp in {{Worker,Light}"Rush",NaiveMCTS,A3N,GAB}; do
	
		echo "Opponent: $opp";
			
		for map in {basesWorkers8x8,basesWorkers16x16A,basesWorkers32x32A,"(4)BloodBath.scmB"}; do
			python3 analysis/average_score.py results/alpha-winlossdraw/"$alpha"-"$map" -i 0 -f 2 -o $opp
		done
	done
done

echo "DONE"
