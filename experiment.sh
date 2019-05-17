#!/bin/bash

# compiles project
ant

# configures classpath and runs
classpath=.:bin:lib/*

echo "Launching experiment..."

java -classpath $classpath -Djava.library.path=lib/ main.Main "$@" 

echo "Done."
