#!/bin/bash

# Generates data to plot learning curves
# -d, --working-dir,   Directory to load weights in and save results (full dir WITHOUT rep number)
# -t, --test_opponent, Name of the AI to test against
# -i initial rep to generate data
# -f final rep to generate data
# --test_matches number of tests matches
# --checkpoint which training checkpoint to test

classpath=.:bin:lib/*

echo "Launching learning curve matches..."

java -classpath $classpath -Djava.library.path=lib/ main.LearningCurve "$@" 

echo "Done."
