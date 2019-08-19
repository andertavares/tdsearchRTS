#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import commandlog

def parse_args():
    parser = argparse.ArgumentParser(
        description='Generates the commands to run lambda learning curves, printing to stdout'
    )

    parser.add_argument(
        'basedir', help='Base directory where results will be stored',
    )
    
    parser.add_argument(
        '-t', '--train-matches', required=True, type=int, 
        help='Number of train matches'
    )

    parser.add_argument(
        '-a', '--test-matches', required=False, type=int, default=10, 
        help='Number of test matches'
    )
    
    # TODO: support --test-opponent
    # TODO: join parameters with other generate-lambda's.py
    
    parser.add_argument(
        '--decision-interval', required=False, type=int, default=10, 
        help='Decision interval (how long to stick with a choice)'
    )
    
    parser.add_argument(
        '--checkpoint', required=False, type=int, default=10, 
        help='Frequency of checkpoints (matching the checkpoint  parameter used during training)'
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
        '-s', '--strategies', help='List of strategies (comma-separated list without spaces)', 
        default='CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M'
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
        commandlog.log_command(' '.join(sys.argv), 'lambda lcurve')
    
    for mapname in args.maps:
        for lambd in args.lambdas:
            for c in range(args.checkpoint, args.train_matches+1, args.checkpoint):  # +1 in second argument to ensure the last checkpoint is also picked 
                command = './learningcurve.sh --test_matches %d --checkpoint %d -i %d -f %d -d %s/%s/fmaterialdistancehp_s%s_rwinlossdraw/m%d/d%d/a0.01_e0.1_g1.0_l%s' % \
                (args.test_matches, c, args.initial_rep, args.final_rep, 
                args.basedir, mapname, args.strategies, args.train_matches, 
                args.decision_interval, lambd)
            
                print(command)
            

