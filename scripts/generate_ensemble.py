#!/usr/bin/python3
import itertools

import argparse
import sys
import commandlog


def arg_parser(description='Generates commands to run ensemble experiments'):
    parser = argparse.ArgumentParser(
        description=description
    )
    
    parser.add_argument(
        'basedir', help='Base directory of results',
    )

    parser.add_argument(
        '--configs', help='List of ensemble configurations to evaluate', nargs='+',
        required=False
    )
    
    parser.add_argument(
        '--ensemble-paths', help='Pattern to match ensemble policy files (must include a %d for player position)',
        default='crowd_p%d-m*-r*.bin'
    )

    parser.add_argument(
        '-t', '--train-matches', required=True, type=int,
        help='Number of train matches'
    )
    
    parser.add_argument(
        '--test-matches', type=int, default=40,
        help='Number of test matches'
    )
    
    parser.add_argument(
        '-b', '--search-timebudget', type=int, default=[0], nargs='+',
        help='Time budget of the search algorithm during tests our learning curve matches.'
    )

    parser.add_argument(
        '--lcurve-matches', type=int, default=20,
        help='Number of learning curve matches'
    )
    
    parser.add_argument(
        '-c', '--checkpoint', type=int, default=10,
        help='Save weights every "checkpoint" matches'
    )

    parser.add_argument(
        '--specific-checkpoints', type=int, default=[], nargs='+',
        help='Run learning curve experiments of specific checkpoints.'
    )

    parser.add_argument(
        '-i', '--initial-rep', required=True, type=int,
        help='Initial repetition'
    )
    
    parser.add_argument(
        '-p', '--portfolio', default='WR,LR,RR,HR',
        help='Portfolio (csv list of WR,LR,RR,HR,WD,LD,RD,HD,BB and/or BK)'
    )

    parser.add_argument(
        '-f', '--final-rep', required=True, type=int,
        help='Final repetition'
    )

    parser.add_argument(
        '-m', '--maps', help='List of maps', nargs='+',
        required=True
        #default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
    )

    parser.add_argument(
        '-l', '--lambdas', help='List of lambda values', nargs='+',
        #default=[0.0, 0.1, 0.3, 0.5, 0.7, 0.9]
        default=[0.5]
    )

    parser.add_argument(
        '-e', '--epsilons', help='List of exploration rates', nargs='+',
        default=[0.1]
        #default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    parser.add_argument(
        '-a', '--alphas', help='List of alphas to test', nargs='+',
        #default=[0.001, 0.01, 0.1, 0.3, 0.5]
        default=[0.01]
    )

    parser.add_argument(
        '-g', '--gammas', help='List of gammas to test', nargs='+',
        #default=[0.5, 0.7, 0.9, 0.99, 0.999, 1.0]
        default=[0.99]
    )

    parser.add_argument(
        '-d', '--decision-intervals', type=int, help='List of decision intervals', nargs='+',
        default=[100]
        # default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    parser.add_argument(
        '--features', help='List of feature extractors', nargs='+',
        default=['quadrantmodel']
        # default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    parser.add_argument(
        '--other', help='A string with as many other parameters as you want', 
        default=''
    )

    parser.add_argument(
        '--train-opponents', help='Training opponents', nargs='+', default=['selfplay']
    )

    parser.add_argument(
        '--test-opponents', help='Test opponents', nargs='+', 
        default=['players.A3N']
    )

    parser.add_argument(
        '--resume', help='Resume previously training sessions rather than start new ones.', action='store_true',
    )

    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )

    parser.add_argument(
        '-o', '--output', help='Appends the generated commands to this file (if omitted, outputs to stdout)',
    )

    parser.add_argument(
        '--overwrite', help='Overwrite rather than append the commands to the output file.',
        action='store_true'
    )
    
    return parser


def cartesian_product(params_dict):
    """
    Returns a generator for the cartesian product of
    parameters that have lists of values
    """
    
    params_list = [
        params_dict[attr] for attr in ['configs', 'maps', 'decision_intervals', 'features', 'alphas', 'gammas', 'lambdas', 'epsilons',
                                  'train_opponents', 'test_opponents', 'search_timebudget']
    ]
    
    return itertools.product(*params_list)
    
def ensemble_commands(params, outstream):
    """
    Writes the commands of the ensemble jobs to the outstream
    """
    
    if params['configs'] is not None:
        for cfg, mapname, interval, feature, alpha, gamma, lamda, epsilon, train_opp, test_opp, budget in cartesian_product(params):
                command = './ensemble_test.sh -c %s -d %s/%s/%s/f%s_p%s_rwinlossdraw/m%d/d%d/a%s_e%s_g%s_l%s ' \
                          '--test_matches %d --save_replay true --test_opponent %s --search_timebudget %s %s' % \
                          (cfg, params['basedir'], train_opp, mapname, feature, params['portfolio'], params['train_matches'], interval,
                           alpha, epsilon, gamma, lamda, params['test_matches'], test_opp, budget, params['other'])
        
                for rep in range(params['initial_rep'], params['final_rep']+1):
                    outstream.write('%s -i %d -f %d\n' % (command, rep, rep))

    else:
        params_list = [
            params[attr] for attr in ['maps', 'decision_intervals', 'features', 'alphas', 'gammas', 'lambdas', 'epsilons',
                                      'train_opponents', 'test_opponents', 'search_timebudget']
        ]
        
        for mapname, interval, feature, alpha, gamma, lamda, epsilon, train_opp, test_opp, budget in itertools.product(*params_list):
                command = './ensemble_test.sh --ensemble_paths %s -d %s/%s/%s/f%s_p%s_rwinlossdraw/m%d/d%d/a%s_e%s_g%s_l%s ' \
                          '--test_matches %d --save_replay true --test_opponent %s --search_timebudget %s %s' % \
                          (params['ensemble_paths'], params['basedir'], train_opp, mapname, feature, params['portfolio'], params['train_matches'], interval,
                           alpha, epsilon, gamma, lamda, params['test_matches'], test_opp, budget, params['other'])
        
                for rep in range(params['initial_rep'], params['final_rep']+1):
                    outstream.write('%s -i %d -f %d\n' % (command, rep, rep))


def generate_commands(params, silent=False):
    if not silent:
        commandlog.log_command(' '.join(sys.argv), 'experiment')

    mode = 'w' if params['overwrite'] else 'a'
    outstream = open(params['output'], mode) if params['output'] is not None else sys.stdout

    ensemble_commands(params, outstream)
    
    # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    args = arg_parser().parse_args()
    
    generate_commands(vars(args), args.silent)

