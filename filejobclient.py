#!/usr/bin/python3

import os.path
import time
import argparse
import subprocess
from contextlib import contextmanager
from datetime import datetime, timedelta


class Client(object):

    def __init__(self, basedir):
        """
        Instantiates the file job client
        :param basedir: directory where todo.txt is located (a simple text file with one command per line)
        """
        self.job_command = None
        self.job = None
        self.basedir = basedir

        # file names
        self.todo = os.path.join(basedir, "todo.txt")
        self.in_progress = os.path.join(basedir, "doing.txt")
        self.done = os.path.join(basedir, "done.txt")
        self.lock_file = os.path.join(basedir, ".lock")

        self.attempts = 0
        self.finished_jobs = 0

    def run(self, max_jobs=0):
        """
        Runs the file job client to execute a number of jobs
        :param max_jobs: number of jobs to execute (0 = unlimited)
        :return:
        """

        while True:
            # if I have finished as many jobs as I want, halt.
            if 0 < max_jobs <= self.finished_jobs:
                print('Finished the pre-set number of jobs (%d). Halting.' % max_jobs)
                return

            # if job is currently running, checks if it has finished
            if self.job is not None:
                if self.job.poll() is not None:  # terminated!
                    self.mark_finished()

                else:  # job is running, wait a bit
                    time.sleep(1)
                    continue

            # looks for a job
            else:
                self.find_job()

                if self.job is None:  # job not found, see if max attempts was reached
                    self.attempts += 1

                    if self.attempts >= 5:
                        print("Halting after 5 unsuccessful attempts to find a job. ")
                        return

                    else:
                        print("No job found (attempt #%d). Sleeping for 5 seconds..." % self.attempts)
                        time.sleep(5)

                else:  # job found, resets the attempt counter
                    self.attempts = 0

    def find_job(self):
        """
        Tries to start the job at the first line of todo.txt.
        If successful, moves the job to doing.txt
        :return:
        """
        with self.lock():
            # grabs a job
            todo_handler = open(self.todo, 'r')
            first_line = todo_handler.readline(4096).strip()
            if first_line is not None and first_line != '':

                # there's a valid job in the file, retrieves and runs it
                self.job_command = first_line

                print("Starting job '%s'" % self.job_command)

                # grabs the job, starts it and returns it
                self.job = subprocess.Popen(
                    self.job_command,
                    shell=True
                )

            todo_handler.close()
            if self.job is not None:  # if I found a job, move it to in progress
                self.move(self.job_command, self.todo, self.in_progress)

    def mark_finished(self):
        """
        Marks the job as finished (i.e.: moves it from doing.txt to done.txt)
        :return:
        """
        print("Job '%s' finished." % self.job_command)
        with self.lock():
            self.move(self.job_command, self.in_progress, self.done)

        # updates my statistics
        self.job = None
        self.job_command = None
        self.finished_jobs += 1

    # moves a line from file1 to file2
    @staticmethod
    def move(line_to_move, file_name1, file_name2):
        """
        Moves a command (a line in a file) from a file to another
        :param line_to_move: the line to be moved (must exist on the origin file)
        :param file_name1: source file name
        :param file_name2: destination file name
        :return:
        """

        # removes the line in file1 (opens, removes, rewrites)
        read_handler = open(file_name1, 'r')
        to_remove = [line.strip() for line in read_handler.readlines()]
        to_remove.remove(line_to_move)
        read_handler.close()

        rewrite_handler = open(file_name1, 'w')
        rewrite_handler.writelines(['%s\n' % line for line in to_remove])
        rewrite_handler.close()

        # appends the line to file2
        file2_handler = open(file_name2, 'a')
        file2_handler.write('%s\n' % line_to_move)

    @contextmanager
    def lock(self, timeout=None, retry_time=0.5):
        """
        Provides mutex lock in self.basedir for safe multiprocess usage.
        Usage:
        with self.lock():
            do your stuff
        at this level of indentation, lock is released

        (adapted from https://codereview.stackexchange.com/a/150237)

        :param timeout: Seconds to wait before giving up (or None to retry indefinitely).
        :param retry_time: Seconds to wait before retrying the lock.

        """
        if timeout is not None:
            deadline = datetime.now() + timedelta(seconds=timeout)

        while True:
            try:
                fd = os.open(self.lock_file, os.O_CREAT | os.O_EXCL)
                yield   # lock acquired, user can do whatever it needs
                break   # gets out of while True to release the lock
            except FileExistsError:
                if timeout is not None and datetime.now() >= deadline:
                    raise
                time.sleep(retry_time)

        # releases the lock
        try:
            os.close(fd)
        finally:
            os.unlink(self.lock_file)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Simple file-based job client.'
    )

    parser.add_argument(
        'basedir',
        help='Base directory where job list are stored (as a txt file with one command per line)',
    )

    parser.add_argument(
        '-m', '--max_jobs', type=int,
        default=0,
        help='Maximum number of jobs this client will perform (0=unlimited).',
    )

    print("Starting simple file job client.")

    args = parser.parse_args()
    client = Client(args.basedir)
    client.run(args.max_jobs)
    print("Goodbye.")
