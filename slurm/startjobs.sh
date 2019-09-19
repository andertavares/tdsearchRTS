#!/bin/bash


# Starts instances of filejobclient.py in parallel.
# Parameters: initial job number, final job number and job queue directory (optional)


if [ "$#" -lt 2 ]; then
	echo "Please inform the initial and final job number."
	exit
fi

queuedir="jobqueue"
if [ "$#" -eq 3 ]; then
	queuedir=$3
fi

# creates the log dir (does not throw error if already exists)
mkdir -p "$queuedir/logs"

# starts the file job clients
for i in $(seq ${1} ${2}); do
	echo "Starting $i"
	#sleep 3 && echo "hi" >> "logs/job$i.txt" & # use this line to test (toggle comments with this and the one below)
	python3 filejobclient.py $queuedir >> "$queuedir/logs/job$i.txt" &
	sleep 1 # 1 second interval to help avoiding race conditions
done
wait

echo "finished"
