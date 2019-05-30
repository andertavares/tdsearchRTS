#!/bin/bash

#must have all results for the five maps ready

for reward in {winloss-tiebreak,victory-only,winlossdraw}; do
	echo "Measuring $reward";
	
	for opp in {{Worker,Light}"Rush",NaiveMCTS,PuppetMCTS,A3N,GAB}; do
		echo "--against $opp (each line is a map)";
		
		for map in {"basesWorkers8x8","basesWorkers16x16A","basesWorkers32x32A","(4)BloodBath.scmB","(4)Andromeda.scxE"}; do
			python3 analysis/average_score.py $1/"$reward-$map" -i $2 -f $3 -o $opp;
		done
	done
done