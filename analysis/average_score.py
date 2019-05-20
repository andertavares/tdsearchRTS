#/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import os


def score(filename):
    """
    Calculates the score (victories + 0.5*draws) from the results in a given .csv file
    Victories are matches with result 0, draws are matches with result -1 (losses have result 1)
    :param filename:
    :return:
    """
    df = pd.read_csv(filename)
    # renames to access with df.result
    df.rename(columns={'#result': 'result'}, inplace=True)

    # score =  number of victories + 0.5 number of draws
    victories = df[df.result == 0].count()['result']
    draws = df[df.result == -1].count()['result']
    return victories + 0.5 * draws


def average_score(basedir, initial_rep, final_rep, map_name, opponent):
    """
    Calculates the average score in a series of repeated test matches
    against an opponent in a given map
    :param basedir: base directory where results are stored
    :param initial_rep:
    :param final_rep:
    :param map_name:
    :param opponent:
    :return:
    """
    files = [os.path.join(basedir, 'selfplay-%s' % map_name, 'rep%d' % rep, 'test-vs-%s.csv' % opponent) for rep in range(initial_rep, final_rep + 1)]
    #print([score(f) for f in files])
    return np.mean([score(f) for f in files])


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Calculates the average score in a series of repeated test matches '
                    'against an opponent in a given map.'
                    'In an experiment, the score is #victories + 0.5 * #draws.'
                    'The script looks in directories with the pattern:'
                    'basedir/selfplay-[map]/rep[%d]/test-vs-[opponent].csv, where %d is in [initial_rep, final_rep]'
    )

    parser.add_argument(
        'basedir', help='base directory where all results are stored.'
    )

    parser.add_argument(
        '-i', '--initial-rep', type=int,
        help='Number of initial repetition.'
    )

    parser.add_argument(
        '-f', '--final-rep', type=int,
        help='Number of the final repetition.'
    )

    parser.add_argument(
        '-o', '--opponent',
        help='Opponent name.'
    )

    parser.add_argument(
        '-m', '--map',
        help='Map name.'
    )

    args = parser.parse_args()

    average_score(args.basedir, args.initial_rep, args.final_rep, args.map, args.opponent)

