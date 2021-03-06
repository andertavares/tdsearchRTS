import os
import sys
import fire
import glob
import stats
import numpy as np
import collections

from tqdm import tqdm
from pprint import pprint


def produce_results(basedir, raw_output='raw.csv', avg_output='avg.csv', sep=',', 
    file_prefix='test-vs-', search_budget=None, test_opp=('A3N',), rep_num=-1):
    """
    Traverses the base dir all the way down to each experiment, collecting the results and writing csv 
    files with raw results of the repetitions of each parameter configuration and an average of all repetitions.

    :param basedir:
    :param raw_outstream:
    :param avg_outstream:
    :param sep:
    :param rep_num: look at a specific rep? -1 to look at all reps
    :param test_opp: collects statistics about test opponents in a list
    :param file_prefix: specify if the csv files to be analysed have a different beginning
    :search_budget: collects statistics of tests on a specific budget for the search algorithm. 
    Defaults to None to maintain compatibility with older versions that generated files without the budget.

    TODO: unify with parse_lcurve_output
    """
    print(basedir, test_opp)
    for opp in test_opp:

        raw_outstream = open(os.path.join(basedir, f'{opp}_{raw_output}'), 'w')
        avg_outstream = open(os.path.join(basedir, f'{opp}_{avg_output}'), 'w')   
    
        # key in results_collection will be a tuple of parameters, each result is a list of metrics
        results_collection = collections.defaultdict(list)
    
        # writes the headers
        raw_outstream.write('%s\n' % sep.join(
            ['train_opp', 'test_opp', 'test_pos', 'mapname', 'feature', 'strat', 'reward', 'train_matches', 'dec_int', 'alpha',
             'epsilon', 'gamma', '_lambda'] + \
            ['wins,draws,losses,test_matches,score,%score']
        ))
    
        avg_outstream.write('%s\n' % sep.join(
            ['num_reps', 'train_opp', 'test_opp', 'test_pos', 'mapname', 'feature', 'strat', 'reward', 'train_matches', 'dec_int',
             'alpha', 'epsilon', 'gamma', '_lambda'] + \
            ['wins,draws,losses,test_matches,score,%score']
        ))
    
        # the last glob parameter are actually 7 parameters expanded from a list of 7 asterisks
        pattern = os.path.join(basedir, *['*'] * 7)
        # if the user specified a rep, look into it
        if rep_num != -1:
            pattern = os.path.join(basedir, *['*'] * 6, f'rep{rep_num}')
            
        for rep_dir in tqdm(glob.glob(pattern)):
            dirs = rep_dir.split(os.sep)
            param_dirs = dirs[-7:]  # ignores directories before the mapname
    
            # gets all directory parts from the parameters and post-process those with appended letters and symbols
            train_opp, mapname, feat_strat_rwd, matches, dec_int, rlparams, rep = param_dirs
    
            # feature, strategy and reward are obtained by removing the first letter after spliting on _
            feature, strat, reward = (x[1:] for x in feat_strat_rwd.split('_'))
    
            # replaces commas to avoid problems when saving .csv files
            strat = strat.replace(',', '_')
    
            # matches and dec_int are obtained by removing the first letter on the dir name
            matches, dec_int = matches[1:], dec_int[1:]
    
            # alpha, epsilon, gamma, lambda are obtained by removing the first letter after spliting on _
            alpha, epsilon, gamma, _lambda = (x[1:] for x in rlparams.split('_'))
    
            results = dict() # player_index -> statistics
            for test_pos in [0, 1]:
    
                test_file = os.path.join(rep_dir, '%s%s_p%d.csv' % (file_prefix, opp, test_pos))
                if search_budget is not None:
                    test_file = os.path.join(rep_dir, '%s%s_p%d_b%d.csv' % (file_prefix, opp, test_pos, search_budget))
                # print(test_file)
    
                if not os.path.exists(test_file):
                    print('WARNING: %s does not exist' % test_file)
                    continue
    
                all_params = [train_opp, opp, str(test_pos), mapname, feature, strat, reward, matches, dec_int, alpha,
                              epsilon, gamma, _lambda]
    
                results[test_pos] = stats.stats(test_file, test_pos)
                results_collection[tuple(all_params)].append(results[test_pos])
    
                # writes the raw output stream
                raw_outstream.write('%s\n' % sep.join(
                    all_params + [str(x) for x in results]
                ))
                #print(results[test_pos])
            # Results were collected for positions 0 and 1, now computes the average and stores as 'position' 2
            try:
                results[2] = np.mean([results[0], results[1]], axis=0)
                all_params[2] = '2'  # player_position <- '2' (string to avoid error afterwards
                results_collection[tuple(all_params)].append(results[2])
            except KeyError as e:
                print('Unable to calculate average results due to missing results in position {}'.format(e))
    
        # after all results are collected, writes them to avg_outstream
        for params, results in results_collection.items():
            # print(params, results)
            # print(np.mean(results, axis=0))
            avg_outstream.write('%s\n' % sep.join(
                [str(len(results))] + list(params) + [str(x) for x in np.mean(results, axis=0)]
            ))
    
        raw_outstream.close()
        avg_outstream.close()


if __name__ == '__main__':
    fire.Fire(produce_results)
    print("Done")
