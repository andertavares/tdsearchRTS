#!/bin/bash

QUEUEDIR="queue_lcurve"

# creates the queue directory (ignores if already exists)
mkdir -p $QUEUEDIR

# uncomment below to erase the previous queue
# rm -f $QUEUEDIR/todo.txt # -f fails silently if the file does not exist

# creates the jobs
for s in {"CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M","HP-,CE,FC,R"}; do 
	for o in {selfplay,players.A3N}; do 
		for t in {100,500,1000}; do 
			for r in {0..4}; do
				# checkpoint is 10 for t in {100,500} [DEACTIVATED and 20 for t==1000]
				c=10
				#if [ $t -eq 1000 ]; then
				#	c=20
				#fi  
				python3 scripts/generate-lambda-lcurve.py results/lambda_train-vs-$o -t $t -i $r -f $r -s $s --checkpoint $c >> $QUEUEDIR/todo.txt
			done
		done
	done
done

# should be 48k jobs in total

echo "Finished generating the job list (`wc -l $QUEUEDIR/todo.txt` jobs)"

