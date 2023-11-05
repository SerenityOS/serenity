#!/usr/bin/env python3

'''Runs `pdf --debugging-stats` on a bunch of PDF files in parallel.

Give it one or more folders containing PDF files, and the optional -n flag
to pick a random subset of n PDFs:

    test_pdf.py -n 200 ~/Downloads/0000 ~/src/pdffiles

https://pdfa.org/new-large-scale-pdf-corpus-now-publicly-available/ has
8 TB of test PDFs, organized in a bunch of zip files with 1000 PDFs each.
One of those zip files in unzipped makes for a good input folder.
'''

import argparse
import collections
import glob
import multiprocessing
import os
import random
import re
import subprocess


Result = collections.namedtuple(
             'Result', ['filename', 'returncode', 'stdout', 'stderr'])


def elide_aslr(s):
    return re.sub(rb'\b0x[0-9a-f]+\b', b'0xc3ns0r3d', s)


def elide_parser_offset(s):
    return re.sub(rb'\bParser error at offset [0-9]+:', b'Parser error:', s)


def test_pdf(filename):
    pdf_path = os.path.join(os.path.dirname(__file__), '../Build/lagom/bin/pdf')
    r = subprocess.run([pdf_path, '--debugging-stats', filename],
                       capture_output=True)
    return Result(filename, r.returncode, r.stdout,
                  elide_parser_offset(elide_aslr(r.stderr)))


def main():
    parser = argparse.ArgumentParser(
        epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument('input', nargs='+', help='input directories')
    parser.add_argument('-n', type=int, help='render at most n pdfs')
    args = parser.parse_args()

    files = []
    for input_directory in args.input:
        files += glob.glob(os.path.join(input_directory, '*.pdf'))
    if args.n is not None:
        random.seed(42)
        files = random.sample(files, k=args.n)

    results = multiprocessing.Pool().map(test_pdf, files)

    num_files_without_issues = 0
    failed_files = []
    num_crashes = 0
    stack_to_files = {}
    for r in results:
        print(r.filename)
        print(r.stdout.decode('utf-8'))
        if r.returncode == 0:
            if b'no issues found' in r.stdout:
                num_files_without_issues += 1
            continue
        if r.returncode == 1:
            failed_files.append(r.filename)
        else:
            num_crashes += 1
            stack_to_files.setdefault(r.stderr, []).append(r.filename)

    print('Top 5 crashiest stacks')
    keys = list(stack_to_files.keys())
    keys.sort(key=lambda x: len(stack_to_files[x]), reverse=True)
    for stack in reversed(keys[:5]):
        files = stack_to_files[stack]
        print(stack.decode('utf-8', 'backslashreplace'), end='')
        print(f'In {len(files)} files:')
        for file in files:
            print(f'    {file}')
        print()

    percent = 100 * num_files_without_issues / len(results)
    print(f'{num_files_without_issues} files without issues ({percent:.1f}%)')
    print()

    percent = 100 * num_crashes / len(results)
    print(f'{num_crashes} crashes ({percent:.1f}%)')
    print(f'{len(keys)} distinct crash stacks')

    percent = 100 * len(failed_files) / len(results)
    print()
    print(f'{len(failed_files)} failed to open ({percent:.1f}%)')
    for f in failed_files:
        print(f'    {f}')


if __name__ == '__main__':
    main()
