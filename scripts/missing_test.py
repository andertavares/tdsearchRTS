# lists all unfinished train experiments from a root dir
import os
import sys
import argparse
from itertools import product

def check_unfinished(params):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, checks if
    it contains the test-vs-[opponent]_p[position].csv file. If not, it is reported as unfinished 
    to the specified output
    :param params: a dict with the required arguments
    :return:
    """
    outstream = open(params['output'], 'w') if params['output'] is not None else sys.stdout

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(params['basedir']):
        # if not dirs, then root is a leaf directory
        if not dirs:
            # checks if the leaf dir is an experiment repetition
            if 'rep' in root:
                
                for opponent in params['opponent']:
                    for position in params['position']:
                        target_name = 'test-vs-%s_p%d.csv' % (opponent, position)

                        incomplete = False
                        if target_name not in files:
                            incomplete = True
                            print(os.path.join(root, target_name))

                        else:  # file exists, count number of non-empty lines
                            with open(filename) as f:
                                non_blank_lines = sum(not line.isspace() for line in f) - 1  # -1 to discount the header

                                if params['test_matches'] // 2 > non_blank_lines:
                                    incomplete = True

                        if incomplete:
                            # the first parameters extract the rep%d part of root
                            # the last two parameters extract the rep number from current directory
                            # TODO accomodate for more than 10 repetitions
                            outstream.write(
                                './test.sh -d %s --test_matches %d --save_replay true -i %s -f %s\n' % (
                                    root[:-4], params['test_matches'], root[-1], root[-1]
                            ))
                            break # gets out of the position-traversing loop to avoid generating the same command twice

                            # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Lists all unfinished test experiments from a root dir'
    )

    parser.add_argument(
        'basedir', help='Root directory of results (will look recursively from there)',
    )

    parser.add_argument(
        '-o', '--output', help='Writes output to this file (if omitted, outputs to stdout)',
    )
    
    parser.add_argument(
        '-p', '--position', help='List of test positions (0, 1 or both)', type=int, nargs='+', default=[0, 1]
    )
    
    parser.add_argument(
        '--opponent', help='List of opponents', nargs='+', default=['A3N']
    )

    parser.add_argument(
        '-t','--test_matches', help='Number of test matches', type=int, default=40
    )
    

    args = parser.parse_args()
    check_unfinished(vars(args))  # to access as dict
