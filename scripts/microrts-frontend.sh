#!/bin/bash

# this is useful to visualize a map, for instance

classpath=.:bin:lib/*

echo "Launching microRTS front-end..."

java -classpath $classpath -Djava.library.path=lib/ gui.frontend.FrontEnd "$@" 

echo "Done."
