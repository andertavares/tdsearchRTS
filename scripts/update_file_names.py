import os
import re
import sys
import fire
import collections


def update_file_names(basedir):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, checks for result 
    files with old name conventions and updates them to new. In the old convention, 
    test and lcurve .csv did not have the time budget (which was 0)
    :return:
    """

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(basedir):
        if not dirs: # if not dirs, then root is a leaf directory
            if 'rep' in root: # checks if the leaf dir is an experiment repetition 
                print(root)
                for f in files:
                    # old file pattern is 'LT-vs-O_pP_mM_bB.csv'; LT=lcurve or test, O=opponent, P=position, M=checkpoint on training, B=search budget
                    if re.search('lcurve-vs-\w+_p\d_m\d+\.csv', f) or re.search('test-vs-\w+_p\d\.csv', f):
                        print('Renaming {} to {}'.format(f, '{}_b0.csv'.format(name)))
                        os.rename(os.path.join(root,f), os.path.join(root,'{}_b0.csv'.format(name)))

if __name__ == '__main__':
    fire.Fire(update_file_names)
    print("Done")