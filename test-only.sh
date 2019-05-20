#!/bin/bash

# Runs test matches. Parameters:
# -c, --config-input,  Input config path
# -t, --test_opponent, Name of the AI to test against
# -d, --working-dir,   Directory to load weights in and save results
# -f, --final_rep,     Number of the final repetition (useful to parallelize executions). Assumes 0 if omitted
# -i, --initial_rep,   Number of the initial repetition (useful to parallelize executions). Assumes 0 if omitted


classpath=.:bin:lib/*

echo "Launching test matches..."

java -classpath $classpath -Djava.library.path=lib/ main.TestOnly "$@" 

echo "Done."
