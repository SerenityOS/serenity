#!/usr/bin/env python3

import argparse
import multiprocessing
import os
from pathlib import Path
import signal
import subprocess
import sys

# Relative to Userland directory
PATHS_TO_SEARCH = [
    'Applications/Assistant',
    'Applications/Browser',
    'Applications/Spreadsheet',
    'Applications/TextEditor',
    'DevTools/HackStudio',
    'Libraries/LibJS',
    'Libraries/LibMarkdown',
    'Libraries/LibWeb',
    'Services/WebContent',
]

parser = argparse.ArgumentParser('LibJSGCVerifier', description='A Clang tool to validate usage of the LibJS GC')
parser.add_argument('-b', '--build-path', required=True, help='Path to the project Build folder')
args = parser.parse_args()

build_path = Path(args.build_path).resolve()
userland_path = build_path / '..' / 'Userland'
include_path = build_path / 'x86_64clang' / 'Root' / 'usr' / 'include'
compile_commands_path = build_path / 'x86_64clang' / 'compile_commands.json'

if not compile_commands_path.exists():
    print(f'Could not find compile_commands.json in {compile_commands_path.parent}')
    exit(1)

paths = []

for containing_path in PATHS_TO_SEARCH:
    for root, dirs, files in os.walk(userland_path / containing_path):
        for file in files:
            if file.endswith('.cpp'):
                paths.append(Path(root) / file)


def thread_init():
    signal.signal(signal.SIGINT, signal.SIG_IGN)


def thread_execute(file_path):
    clang_args = [
        './build/LibJSGCVerifier',
        '--extra-arg',
        '-DUSING_AK_GLOBALLY=1',  # To avoid errors about USING_AK_GLOBALLY not being defined at all
        '-p',
        compile_commands_path,
        file_path,
    ]
    proc = subprocess.Popen(clang_args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    sys.stdout.buffer.write(proc.communicate()[0])
    sys.stdout.buffer.flush()


with multiprocessing.Pool(processes=multiprocessing.cpu_count() - 2, initializer=thread_init) as pool:
    try:
        pool.map(thread_execute, paths)
    except KeyboardInterrupt:
        pool.terminate()
        pool.join()
