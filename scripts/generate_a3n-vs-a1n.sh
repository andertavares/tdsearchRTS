#!/bin/bash

# Generates the commands that execute A3N vs A1N.
# Parameter: root results dir

if [ "$#" -lt 1 ]; then
	echo "Please specify the root results dir"
	exit
fi 

# maps that were left out: BroodWar/"(4)BloodBath.scmB.xml" 

for m in {16x16/TwoBasesBarracks16x16.xml,16x16/basesWorkers16x16A.xml,24x24/basesWorkers24x24A.xml,32x32/basesWorkers32x32A.xml,8x8/basesWorkers8x8A.xml,BWDistantResources32x32.xml,DoubleGame24x24.xml,NoWhereToRun9x8.xml}; do 
	for s in {CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M}; do 
		for u in {0..3}; do 
			echo "./scripts/a3n-vs-a1n.sh -m maps/$m -d $1 -s $s -u $u"
		done
	done
done