#!/usr/bin/python3
import itertools

import argparse
import sys
import commandlog


def arg_parser(description='Generates commands to run experiments: train, learning curve and test (in this order)'):
    parser = argparse.ArgumentParser(
        description=description
    )

    parser.add_argument(
        'basedir', help='Base directory of results',
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
        '--lcurve-matches', type=int, default=20,
        help='Number of learning curve matches'
    )
    
    parser.add_argument(
        '-c', '--checkpoint', type=int, default=10,
        help='Save weights every "checkpoint" matches'
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
        default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
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
        default=[0.9]
    )

    parser.add_argument(
        '-d', '--decision-intervals', type=int, help='List of decision intervals', nargs='+',
        default=[10]
        # default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    '''parser.add_argument(
        '-s', '--strategies', help='List of sets of strategies (each set is a comma-separated string without spaces)',
        nargs='+',
        default=['CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M']
    )'''

    parser.add_argument(
        '--train-opponents', help='Training opponents', nargs='+', default=['selfplay']
    )

    parser.add_argument(
        '--test-opponents', help='Test opponents', nargs='+', 
        default=['players.A3N']
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
    
    parser.add_argument(
        '--no-train', help="Don't generate train jobs",
        action='store_true'
    )
    
    parser.add_argument(
        '--no-test', help="Don't generate test jobs",
        action='store_true'
    )
    
    parser.add_argument(
        '--no-lcurve', help="Don't generate learning curve jobs",
        action='store_true'
    )

    return parser


def cartesian_product(params_dict):
    """
    Returns a generator for the cartesian product of
    parameters that have lists of values
    """
    
    params_list = [
        params_dict[attr] for attr in ['maps', 'decision_intervals', 'alphas', 'gammas', 'lambdas', 'epsilons',
                                  'train_opponents', 'test_opponents']
    ]
    
    return itertools.product(*params_list)


def train_commands(params, outstream):
    """
    Writes the commands of the train jobs to the outstream
    """
    for mapname, interval, alpha, gamma, lamda, epsilon, train_opp, _ in cartesian_product(params):
            command = './train.sh -c config/%s.properties -d %s/%s --train_matches %s --decision_interval %d ' \
                      '--train_opponent %s -p %s -e materialdistancehp -r winlossdraw ' \
                      '--td_alpha_initial %s --td_gamma %s --td_epsilon_initial %s --td_lambda %s ' \
                      '--checkpoint %d' % \
                      (mapname, params['basedir'], train_opp, params['train_matches'], interval,
                       train_opp, params['portfolio'], alpha, gamma, epsilon, lamda, params['checkpoint'])
    
            for rep in range(params['initial_rep'], params['final_rep']+1):
                outstream.write('%s\n' % command)


def test_commands(params, outstream):
    """
    Writes the commands of the test jobs to the outstream
    """
    for mapname, interval, alpha, gamma, lamda, epsilon, train_opp, test_opp in cartesian_product(params):
            command = './test.sh -d %s/%s/%s/fmaterialdistancehp_p%s_rwinlossdraw/m%d/d%d/a%s_e%s_g%s_l%s ' \
                      '--test_matches %d --save_replay true --test_opponent %s' % \
                      (params['basedir'], train_opp, mapname, params['portfolio'], params['train_matches'], interval,
                       alpha, epsilon, gamma, lamda, params['test_matches'], test_opp)
    
            for rep in range(params['initial_rep'], params['final_rep']+1):
                outstream.write('%s -i %d -f %d\n' % (command, rep, rep))


def lcurve_commands(params, outstream):
    """
    Writes the commands of the learning curve jobs to the outstream
    """
    for mapname, interval, alpha, gamma, lamda, epsilon, train_opp, test_opp in cartesian_product(params):
            for c in range(params['checkpoint'], params['train_matches']+1, params['checkpoint']):  # +1 in second argument to ensure the last checkpoint is also picked 
                    command = './learningcurve.sh -d %s/%s/%s/fmaterialdistancehp_p%s_rwinlossdraw/m%d/d%d/a%s_e%s_g%s_l%s ' \
                      '--test_matches %d --checkpoint %d ' % \
                      (params['basedir'], train_opp, mapname, params['portfolio'], params['train_matches'], interval,
                       alpha, epsilon, gamma, lamda, params['lcurve_matches'], c)
                       
                    for rep in range(params['initial_rep'], params['final_rep']+1):
                        outstream.write('%s -i %d -f %d\n' % (command, rep, rep))


def generate_commands(params, silent=False):
    if not silent:
        commandlog.log_command(' '.join(sys.argv), 'experiment')

    mode = 'w' if params['overwrite'] else 'a'
    outstream = open(params['output'], mode) if params['output'] is not None else sys.stdout

    # writes train jobs
    if not params['no_train']:
        train_commands(params, outstream)
    
    # writes learning curve jobs
    if not params['no_lcurve']:
        lcurve_commands(params, outstream)
        
    # writes test jobs
    if not params['no_test']:
        test_commands(params, outstream)
    
    # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    args = arg_parser().parse_args()
    
    generate_commands(vars(args), args.silent)

