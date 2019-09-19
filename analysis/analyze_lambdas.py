#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import statistics
#import commandlog #TODO: solve this
from a3n_vs_a1n_table import generate_table

def parse_args():
    parser = argparse.ArgumentParser(
        description='Analyses the results of lambda tests'
    )

    parser.add_argument(
        'basedir', help='Base directory or results',
    )

    parser.add_argument(
        '-q', '--stdout', action='store_true', 
        default=False,
        help='Quiet: output to stdout rather than to a file',
    )
    
    parser.add_argument(
        '-t', '--train-matches', required=True, type=int, 
        help='Number of train matches'
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
        '-r', '--rep', help='Repetition number to analyse', type=int,
        default=0
    )
    
    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )
    
    parser.add_argument(
        '-m', '--maps', help='List of maps', nargs='+',
        default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
    )

    return parser.parse_args()
    
def raw_analysis(basedir, rep, trainmatches, maps, strategies, lambdas, stdout):
    
    for player in [0, 1]:
        outfile = os.path.join(basedir, 'lambdas_p%d_rep%d.csv' % (player, rep) )
        
        outstream = open(outfile, 'w') if not stdout else sys.stdout 
        outstream.write(
            'trainmatches,map,lambda,features,strategy,position,wins,draws,losses,matches,score,%score\n'
        )
        
        for m in maps:
            for lam in lambdas:
                path = os.path.join(
                    basedir, m, 'fmaterialdistancehp_s%s_rwinlossdraw' % strategies,
                    'm%d' % trainmatches, 'd10', 'a0.01_e0.1_g1.0_l%s' % lam, 
                    'rep%d' % rep, 'test-vs-A3N_p%d.csv' % player
                )
                
                if os.path.exists(path):
                    outstream.write('%d,%s,%s,materialdistancehp,%s,%d,%s\n' % (
                        trainmatches, m, lam, strategies.replace(',', ' '), player, 
                        ','.join([str(x) for x in statistics.average_score([path], player)])    
                    ))
                else:
                    print('%s does not exist' % path) # this one always go to stdout
        
        if not sys.stdout: # prevents closing sys.stdout
            outstream.close()
    

if __name__ == '__main__':
    args = parse_args()
    
    #if not args.silent: # registers the parameters of this call
    #    commandlog.log_command(' '.join(sys.argv), 'lambda analysis')
    
    # TODO customize output dir or prefix

    raw_analysis(args.basedir, args.rep, args.train_matches, args.maps, args.strategies, args.lambdas, args.stdout)
    
    if not args.stdout: # also calls a3n-vs-a1n-table.generate_table if -q was omitted
        '''out_table_format = os.path.join(args.basedir, 'A3N_p%d_' + args.metric + '.csv')
        
        for player in [0, 1]:
            infile = os.path.join(args.basedir, 'A3N_p%d.csv' % player)
            outfile = out_table_format % player
            generate_table(infile, outfile, args.metric)
            
        if args.metric != '%score':
            # adds the two tables into one
            df1 = pd.read_csv(out_table_format % 0)
            df2 = pd.read_csv(out_table_format % 1)
            
            # replaces columns 2:6 on df1 with the sum of these columns on both dataframes
            # TODO it is generating a first column with unnecessary integer indices
            df1.loc[:, 2:6] = df1.iloc[:, 2:6] + df2.iloc[:, 2:6]
            df1.to_csv(os.path.join(args.basedir, 'A3N_%s_sum.csv' % args.metric))
            '''
            
        print('Results are in .csv files at %s' % args.basedir)

