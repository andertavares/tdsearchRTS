#!/usr/bin/python3
import os
import sys
import glob
import shutil
import argparse

if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Moves weights from experiment repetitions to rep0/ dir. They will be named crowdweight-pP-mM-rR.bin (P for player position, M for the checkpoint number and x for the original rep).'
    )
    
    parser.add_argument(
        'basedir', help='Base directory of results (where all reps are)',
    )
    
    parser.add_argument(
       '-c', '--checkpoint', help='Which checkpoint shall be gathered.',
       required=True, type=int
    )
    
    parser.add_argument(
       '-p', '--prefix', help='Prefix of the files in the new folder',
       default='crowd'
    )

    args = parser.parse_args()
    
    rep0_dir = os.path.join(args.basedir, 'rep0')
    
    for rep_dir in glob.glob(os.path.join(args.basedir, 'rep*')):
        rep_num = int(rep_dir.split('/')[-1][3:])  # -1 gets rep* dir and 3: gets the *
        #print(rep_num, rep_dir)
        for p in [0, 1]:  # copies the files for both player positions
            shutil.copyfile(
                os.path.join(rep_dir, f'weights_{p}-m{args.checkpoint}.bin'), 
                os.path.join(rep0_dir, f'{args.prefix}_p{p}-m{args.checkpoint}-r{rep_num}.bin')
            )
    print(f'Done. Check {rep0_dir}.')



