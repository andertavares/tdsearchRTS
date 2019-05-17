#!/bin/bash

# this is useful to visualize a map, for instance

classpath=.:bin:lib/*

java -classpath $classpath -Djava.library.path=lib/ utils.MapSize "$@" 

