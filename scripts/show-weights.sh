#!/bin/bash

classpath=.:bin:lib/*

echo "Showing learned weights"

java -classpath $classpath -Djava.library.path=lib/ utils.ShowWeights "$@" 

echo "Done."

