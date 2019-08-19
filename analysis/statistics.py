#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import os


def stats(filename, player_index):
    """
    Calculates the statistics (win, loss, draws) from the results in a given .csv file
    Victories are matches with result 0, draws are matches with result -1 (losses have result 1)
    :param filename:
    :param player_index
    :return: wins, draws, losses, #matches, score (wins + 0.5*draws) and %score (according to the number of matches) 
    """
    df = pd.read_csv(filename)
    # renames to access with df.result
    df.rename(columns={'#result': 'result'}, inplace=True)

    # score =  number of wins + 0.5 number of draws
    wins = df[df.result == player_index].count()['result']
    draws = df[df.result == -1].count()['result']
    losses = df[df.result == (1 - player_index)].count()['result']
    
    score = wins + 0.5 * draws
    
    num_matches = wins + draws + losses
    
    return wins, draws, losses, num_matches, score,  100 * score / num_matches #returns the score and its percent


def average_score(files, position):
    """
    Calculates the average score in a series of repeated test matches
    :param files: csv files with results
    :param position: player position (0 or 1)
    :return:
    """
    #filename = 'test-vs-%s_p%d.csv' % (opponent, position) if position is not None else 'test-vs-%s.csv' % opponent
    #files = [os.path.join(directory,  filename) for directory in files]
    #print([score(f) for f in files])
    
    #print ( [stats(f, position) for f in files])
    #player_index = position if position is not None else 0
    return np.mean([stats(f, position) for f in files], axis=0)


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Calculates the statistics in a series of experiments '
                    'against an opponent and returns them.\n'
                    'The returned statistics are wins,draws,losses,#matches,score and %score. '
                    'In an experiment, the score is #victories + 0.5 * #draws. '
                    'The script looks for files named test-vs-[opponent]_p%d.csv in each of the files, ' 
                    'where %d is the player index (0 or 1)\n'
                    'Usage example:\n'
                    'python3 analysis/average_score.py results/rep* -o LightRush\n'
                    'For multiple opponents:\n'
                    'for o in {"HeavyRush","RangedRush","WorkerRush","LightRush"}; do echo $o; python3 analysis/average_score.py results/rep* -o $o; done'
    )

    parser.add_argument(
        'files', help='list of files to analyze (you can use terminal wildcards)', nargs="+"
    )

#     parser.add_argument(
#         '-o', '--opponent', required=True,
#         help='Opponent name.'
#     )
    
    parser.add_argument(
        '-p', '--position', required=False, type=int, choices=[0,1], default=0,
        help='Learning agent position (0 or 1)'
    )
    
    parser.add_argument(
        '-e', '--explain', required=False, action='store_true', default=False,
        help='Describe what each number is.'
    )
    
    

    args = parser.parse_args()

    if args.explain:
        print("wins,draws,losses,matches,score,%score")
        
    print(",".join([str(x) for x in average_score(args.files, args.position)] ))

