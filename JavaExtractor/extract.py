#!/usr/bin/python
import glob
import multiprocessing
import os
import shutil
import subprocess
import sys
from argparse import ArgumentParser
from threading import Timer
import itertools

def get_immediate_subdirectories(a_dir):
    return [(os.path.join(a_dir, name)) for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]

def get_immediate_files(dir):
    return [(os.path.join(dir, name)) for name in os.listdir(dir)
            if os.path.isfile(os.path.join(dir, name))]

TMP_DIR = ""


def ParallelExtractDir(args, dir):
    return ExtractFeaturesForDir(args, dir, "")

def ExtractFeaturesForFile(args, file):
    command = get_java_command(args, file, is_file=True)
    # print command
    # os.system(command)
    kill = lambda process: process.kill()
    outputFileName = TMP_DIR + file.replace('/', '_').replace('~','').lstrip('_')
    failed = False
    with open(outputFileName, 'a') as outputFile:
        sleeper = subprocess.Popen(command, stdout=outputFile, stderr=subprocess.PIPE)
        timer = Timer(60 * 10, kill, [sleeper])

        try:
            timer.start()
            stdout, stderr = sleeper.communicate()
        finally:
            timer.cancel()

        if sleeper.poll() == 0:
            if len(stderr) > 0:
                print(stderr.decode('ascii'), file=sys.stderr)
        else:
            print('file: ' + str(file) + ' was not completed in time', file=sys.stderr)
            failed = True
            
    if failed:
        if os.path.exists(outputFileName):
            os.remove(outputFileName)

def ExtractFeaturesForDir(args, dir, prefix):
    command = get_java_command(args, dir)
    # print command
    # os.system(command)
    kill = lambda process: process.kill()
    outputFileName = TMP_DIR + prefix + dir.split('/')[-1]
    failed = False
    remaining_files = []
    with open(outputFileName, 'a') as outputFile:
        sleeper = subprocess.Popen(command, stdout=outputFile, stderr=subprocess.PIPE)
        timer = Timer(60 * 10, kill, [sleeper])

        try:
            timer.start()
            stdout, stderr = sleeper.communicate()
        finally:
            timer.cancel()

        if sleeper.poll() == 0:
            if len(stderr) > 0:
                print(stderr.decode('ascii'), file=sys.stderr)
        else:
            failed = True
    
    if failed:
        os.remove(outputFileName)
        print('dir: ' + str(dir) + ' was not completed in time', file=sys.stderr)
        subdirs = get_immediate_subdirectories(dir)
        
        if len(subdirs) == 0:
            return list(glob.iglob(dir + '/**/*', recursive=True))
        for subdir in subdirs:
            remaining_files_for_subdir = ExtractFeaturesForDir(args, subdir, prefix + dir.split('/')[-1] + '_')
            remaining_files += remaining_files_for_subdir
        
    return remaining_files


def get_java_command(args, path, is_file=False):
    command = ['java', '-Xmx100g', '-XX:MaxNewSize=60g', '-cp', args.jar, 'JavaExtractor.App',
               '--max_path_length', str(args.max_path_length), '--max_path_width', str(args.max_path_width),
               #'--max_relative_path_length', str(args.max_relative_path_length),
               '--max_child_id', str(args.max_child_id),
               '--num_threads', str(args.num_threads), "--max_internal_paths", str(args.max_internal_paths)]
    if is_file:
        command.append('--file')
    else:
        command.append('--dir')
    command.append(path)
    if args.json_max_contexts:
        command.append('--json_output')
        command.append('--max_contexts')
        command.append(args.json_max_contexts)
    if args.exp:
        command.append('--exp')
    command.extend(['--max_code_len', str(20)])
    return command


def ExtractFeaturesForDirsList(args, dirs):
    global TMP_DIR
    TMP_DIR = "./tmp/feature_extractor%d/" % (os.getpid())
    if os.path.exists(TMP_DIR):
        shutil.rmtree(TMP_DIR, ignore_errors=True)
    os.makedirs(TMP_DIR)
    try:
        with multiprocessing.Pool(6) as p:
            remaining_files = p.starmap(ParallelExtractDir, zip(itertools.repeat(args), dirs))
        
        flat_remaining_files = list(itertools.chain.from_iterable(remaining_files))
        print('Extracted from dirs, extracting from {} files of {} dirs'.format(
            len(flat_remaining_files), len([files_list for files_list in remaining_files if len(files_list) > 0])),
              file=sys.stderr)
        with multiprocessing.Pool(6) as p:
            p.starmap(ExtractFeaturesForFile, zip(itertools.repeat(args), flat_remaining_files))
        # for dir in dirs:
        #    ExtractFeaturesForDir(args, dir, '')
        output_files = os.listdir(TMP_DIR)
        for f in output_files:
            os.system("cat %s/%s" % (TMP_DIR, f))
    finally:
        shutil.rmtree(TMP_DIR, ignore_errors=True)


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-maxlen", "--max_path_length", dest="max_path_length", required=True)
    parser.add_argument("-maxwidth", "--max_path_width", dest="max_path_width", required=True)
    #parser.add_argument("--max_relative_path_length", dest="max_relative_path_length", required=True)
    parser.add_argument("--max_child_id", dest="max_child_id", required=True)
    parser.add_argument("-threads", "--num_threads", dest="num_threads", required=False, default=64)
    parser.add_argument("-j", "--jar", dest="jar", required=True)
    parser.add_argument("-dir", "--dir", dest="dir", required=False)
    parser.add_argument("-file", "--file", dest="file", required=False)
    parser.add_argument("--json_max_contexts", dest="json_max_contexts", required=True)
    parser.add_argument("--max_internal_paths", dest="max_internal_paths", required=True)
    parser.add_argument('--exp', action='store_true')
    args = parser.parse_args()

    if args.file is not None:
        command = 'java -cp ' + args.jar + ' JavaExtractor.App --max_path_length ' + \
                  str(args.max_path_length) + ' --max_path_width ' + str(args.max_path_width) + ' --file ' + args.file
        os.system(command)
    elif args.dir is not None:
        subdirs = get_immediate_subdirectories(args.dir)
        if len(subdirs) == 0:
            subdirs = [args.dir]
        ExtractFeaturesForDirsList(args, subdirs)
