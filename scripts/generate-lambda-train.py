#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import commandlog


def parse_args():
    parser = argparse.ArgumentParser(
        description='Generates the commands to run lambda tests, printing to stdout'
    )

    parser.add_argument(
        'basedir', help='Base directory where results will be stored',
    )

    parser.add_argument(
        '-t', '--train-matches', required=True, type=int, 
        help='Number of train matches'
    )
    
    parser.add_argument(
        '-r', '--repetitions', required=True, type=int, 
        help='Number of repetitions'
    )
    
    parser.add_argument(
        '-m', '--maps', help='List of maps', nargs='+',
        default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
    )
    
    parser.add_argument(
        '-l', '--lambdas', help='List of lambda values', nargs='+',
        default=[0.1, 0.3, 0.5, 0.7, 0.9]
    )
    
    parser.add_argument(
        '-s', '--strategies', help='List of strategies (comma-separated list without spaces, or the keyword "all")', 
        default='all'
    )
    
    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )
    
    parser.add_argument(
        '--train-opponent', help='Training opponent', default='selfplay'
    )
    
    return parser.parse_args()


if __name__ == '__main__':

    args = parse_args()
    
    if not args.silent:
        # registers the parameters of this call
        commandlog.log_command(' '.join(sys.argv), 'lambda train')
    
    for mapname in args.maps:
        for lambd in args.lambdas:
            command = './train.sh -c config/%s.properties -d %s --train_matches %s --decision_interval 10 --train_opponent %s -s %s -e materialdistancehp -r winlossdraw --td_lambda %s --checkpoint 10' % \
            (mapname, args.basedir, args.train_matches, args.train_opponent, args.strategies, lambd)

        
            for rep in range(0, args.repetitions):
                print(command)

