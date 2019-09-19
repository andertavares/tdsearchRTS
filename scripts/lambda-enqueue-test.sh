#!/bin/bash

if [ "$#" -lt 2 ]; then
	echo "Please inform basedir and queuedir (in this order)"
	exit
fi

BASEDIR=$1
QUEUEDIR=$2

# creates the queue directory (ignores if already exists)
mkdir -p $QUEUEDIR

# uncomment below to erase the previous queue
# rm -f $QUEUEDIR/todo.txt # -f fails silently if the file does not exist

# creates the jobs
for s in {"CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M","HP-,CE,FC,R"}; do 
	for t in {100,500,1000}; do 
		for r in {0..4}; do
			python3 scripts/generate-lambda-test.py $BASEDIR -t $t -i $r -f $r -s $s >> $QUEUEDIR/todo.txt
		done
	done
done

# should be 48k jobs in total

echo "Finished generating the job list (`wc -l $QUEUEDIR/todo.txt` jobs)"

