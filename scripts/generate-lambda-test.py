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
        '-a', '--test-matches', required=False, type=int, default=40, 
        help='Number of test matches'
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
        '-i', '--initial-rep', help='Initial repetition', type=int,
        default=0
    )
    
    parser.add_argument(
        '-f', '--final-rep', help='Final repetition', type=int,
        default=0
    )
    
    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )
    
    return parser.parse_args()


if __name__ == '__main__':

    args = parse_args()
    
    if not args.silent:
        # registers the parameters of this call
        commandlog.log_command(' '.join(sys.argv), 'lambda test')
    
    for mapname in args.maps:
        for lambd in args.lambdas:
            command = './test.sh --test_matches %d -i %d -f %d --save_replay true -d %s/%s/fmaterialdistancehp_s%s_rwinlossdraw/m%d/d10/a0.01_e0.1_g1.0_l%s' % \
            (args.test_matches, args.initial_rep, args.final_rep, 
            args.basedir, mapname, args.strategies, args.train_matches, lambd)
        
            print(command)
            

