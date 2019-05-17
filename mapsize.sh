#!/bin/bash

# this script calls the utility that indicates the width x heigth of the given map

classpath=.:bin:lib/*

java -classpath $classpath -Djava.library.path=lib/ utils.MapSize "$@" 

