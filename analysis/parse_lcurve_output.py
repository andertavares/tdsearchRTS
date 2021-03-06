import os
import re
import sys
import fire
import glob
import stats
import numpy as np
import collections

from tqdm import tqdm
from pprint import pprint


def produce_results(basedir, raw_outstream=sys.stdout, avg_outstream=sys.stdout, sep=',', 
    test_opp='A3N', file_prefix='test-vs-', search_timebudget=None):
    """
    Traverses the base dir all the way down to each experiment, collecting the results and writing csv 
    files with raw results of the repetitions of each parameter configuration and an average of all repetitions.

    :param basedir:
    :param raw_outstream:
    :param avg_outstream:
    :param sep:
    :param test_opp: collects statistics about a specific test opponent
    :param file_prefix: specify if the csv files to be analysed have a different beginning
    :search_timebudget: collects statistics of tests on a specific budget for the search algorithm. 
    Defaults to None to maintain compatibility with older versions that generated files without the budget.
    """


    


def main(basedir, raw_output='raw.csv', avg_output='avg.csv', sep=',', test_opp=('A3N',), file_prefix='lcurve-vs-', search_timebudget=None):
    """
    Generates a .csv file by parsing all experiment data found by recursively traversing basedir.

    The expected directory structure of experiments is:
    basedir/train_opp/map/fFEATURE_sSTRAT_rREWARD/mTRAIN/dDECISION/aALPHA_eEPSILON_gGAMMA_lLAMBDA/repREP

    results/selfplay/TwoBasesBarracks16x16/fmaterialdistancehp_sCC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M_rwinlossdraw/m1000/d10/a0.01_e0.1_g1_l0/rep0


    :param basedir: experiment root dir.
    :param raw_output: file to write the raw results of each repetition.
    :param avg_output: file to write the average results across repetitions.
    :param sep: separator of the .csv file
    :param test_opp: list of test opponents -- without spaces! E.g.: [A3N,WorkerRush]
    :param file_prefix: specify if the csv files to be analysed have a different beginning
    :param search_timebudget: time budget of the search algorithm. (defaults to None to maintain compatibility with versions that generated output files without the budget.)
    :return:
    """
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
        for rep_dir in tqdm(glob.glob(os.path.join(basedir, *['*'] * 7))):
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
    
            #print(rep_dir)
            results = collections.defaultdict(dict) # matches,player_index -> statistics
            for test_pos in [0, 1]:
    
                # lcurve file pattern is 'lcurve-vs-O_pP_mM_bB.csv'; O=opponent, P=position, M=checkpoint on training, B=search budget
                exp_result_pattern = os.path.join(rep_dir, '%s%s_p%d_m*.csv' % (file_prefix, opp, test_pos))
                if search_timebudget is not None:
                    exp_result_pattern = os.path.join(rep_dir, '%s%s_p%d_m*_b%d.csv' % (file_prefix, opp, test_pos, search_timebudget))
                
                for lcurve_filename in tqdm(glob.glob(exp_result_pattern)):
                    # extracts the number of training matches from the filename and overrides the 'matches' variable
                    matches = re.findall('m(\d+)', lcurve_filename)[1] #the first m%d is on the directory, the second, which we use, is on the filename
                    #print(lcurve_filename, matches)
    
                    all_params = [train_opp, opp, str(test_pos), mapname, feature, strat, reward, matches, dec_int, alpha,
                                  epsilon, gamma, _lambda]
    
                    results[matches][test_pos] = stats.stats(lcurve_filename, test_pos)
                    results_collection[tuple(all_params)].append(results[matches][test_pos])
    
                    # writes the raw output stream
                    raw_outstream.write('%s\n' % sep.join(
                        all_params + [str(x) for x in results]
                    ))
                #print(results)
            # Results were collected for positions 0 and 1, now computes the average and stores as 'position' 2
            try:
                for match_num, pdict in results.items():
                    results[match_num][2] = np.mean([results[match_num][0], results[match_num][1]], axis=0)
                    all_params[2] = '2'  # player_position <- '2' (string to avoid error afterwards)
                    all_params[7] = match_num # matches <- number of matches found during analysis in the index tuple
                    results_collection[tuple(all_params)].append(results[match_num][2])
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
    fire.Fire(main)
    print("Done")
