# lists all unfinished train experiments from a root dir
import os
import sys
import argparse


def check_unfinished(params):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, checks if
    it contains the finished file. If not, it is reported as an unfinished training
    to the specified output
    :param params: a dict with the required arguments
    :return:
    """
    outstream = open(params['output'], 'w') if params['output'] is not None else sys.stdout

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(params['basedir']):
        # if not dirs, then root is a leaf directory
        # checks if the file '.finished' is there
        if not dirs:
            # checks if the leaf dir is an experiment repetition and if .finished is missing
            if 'rep' in root and 'finished' not in files:
                outstream.write('%s\n' % root)

    # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Lists all unfinished train experiments from a root dir'
    )

    parser.add_argument(
        'basedir', help='Root directory of results (will look recursively from there)',
    )

    parser.add_argument(
        '-o', '--output', help='Appends the generated commands to this file (if omitted, outputs to stdout)',
    )

    args = parser.parse_args()
    check_unfinished(vars(args))  # to access as dict
