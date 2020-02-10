import os
import re
import sys
import fire
import tarfile
import collections
from tqdm import tqdm


def pack_checkpoints(basedir):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, creates a .tar.gz 
    with all the weights_P-mM.bin files (P=position, M=match)
    :return:
    """

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(basedir):
        if not dirs: # if not dirs, then root is a leaf directory
            if 'rep' in root: # checks if the leaf dir is an experiment repetition 
                print(root)

                # finds the checkpoint weight files (pattern: weights_P-mM.bin (P=position, M=match))
                checkpoints = [f for f in files if re.search('weights_\d-m\d+\.bin',f)]

                # packs the checkpoints in a .tar.gz
                if len(checkpoints) > 0:
                    with tarfile.open(os.path.join(root, 'checkpoints.tar.gz'), "w:gz") as tar:
                        for c in tqdm(checkpoints):
                            # arcname=c prevents the creation of a chain of subdirs
                            tar.add(os.path.join(root,c), arcname=c) 
                    # removes all checkpoint files after packing
                    os.unlink([os.path.join(root,c) for c in checkpoints])
                

if __name__ == '__main__':
    fire.Fire(pack_checkpoints)
    print("Done")