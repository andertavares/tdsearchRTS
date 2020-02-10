import os
import re
import sys
import fire
import tarfile
import collections


def unpack_checkpoints(basedir, checkpoints_file="checkpoints.tar.gz", remove_compressed=False):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, unpacks the content of 
    checkpoints.tar.gz 
    :return:
    """

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(basedir):
        sys.stdout.write('\rScanning: {}'.format(root))
        if not dirs: # if not dirs, then root is a leaf directory
            if 'rep' in root: # checks if the leaf dir is an experiment repetition 
                # tries to open the file rather than checking its existence w/ os.path.exists as it can be time-consuming 
                compressed_file = os.path.join(root, checkpoints_file)
                try:
                    with tarfile.open(compressed_file) as tf: 
                        print('\nFound packed checkpoints at {}'.format(root))
                        tf.extractall(root)
                except FileNotFoundError as e:
                    pass

                if remove_compressed: 
                    os.unlink(compressed_file)
                    print('Removed {}'.format(compressed_file))
                    

                

if __name__ == '__main__':
    fire.Fire(unpack_checkpoints)
    print("Done")