#!/bin/bash

classpath=.:bin:lib/*

echo "Launching experiment..."

java -classpath $classpath -Djava.library.path=lib/ main.TestOnly "$@" 

echo "Done."
