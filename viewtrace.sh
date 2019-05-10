#!/bin/bash

classpath=.:bin:lib/*

echo "Launching TraceVisualizationTest..."

java -classpath $classpath -Djava.library.path=lib/ tests.TraceVisualizationTest "$@" 

echo "Done."
