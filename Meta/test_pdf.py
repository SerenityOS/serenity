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
import dataclasses
import glob
import json
import multiprocessing
import os
import random
import re
import subprocess


Result = collections.namedtuple(
             'Result', ['filename', 'returncode', 'stdout', 'stderr'])


@dataclasses.dataclass
class Issues:
    filenames: [str]
    filename_to_issues: {str: [int]}
    num_pages: int
    count: int


def elide_aslr(s):
    return re.sub(rb'\b0x[0-9a-f]+\b', b'0xc3ns0r3d', s)


def elide_parser_offset(s):
    return re.sub(rb'\bParser error at offset [0-9]+:', b'Parser error:', s)


def test_pdf(filename):
    pdf_path = os.path.join(os.path.dirname(__file__), '../Build/lagom/bin/pdf')
    r = subprocess.run([pdf_path, '--debugging-stats', '--json', filename],
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
    num_files_with_password = 0
    num_files_with_issues = 0
    failed_files = []
    num_crashes = 0
    stack_to_files = {}
    issues = {}
    for r in results:
        if r.returncode == 0:
            if b'PDF requires password' in r.stderr:
                num_files_with_password += 1
                continue

            j = json.loads(r.stdout.decode('utf-8'))
            if not j['issues']:
                num_files_without_issues += 1
            else:
                num_files_with_issues += 1
                for diag in j['issues']:
                    issue = issues.setdefault(diag, Issues([], {}, 0, 0))
                    issue.filenames.append(r.filename)
                    issue.filename_to_issues[r.filename] = j['issues'][diag]
                    issue.num_pages += len(j['issues'][diag])
                    issue.count += sum(count for (page, count) in j['issues'][diag])
            continue
        if r.returncode == 1:
            failed_files.append(r.filename)
        else:
            num_crashes += 1
            stack_to_files.setdefault(r.stderr, []).append(r.filename)

    percent = 100 * num_files_with_issues / len(results)
    print(f'{len(issues)} distinct issues, in {num_files_with_issues} files ({percent}%):')
    issue_keys = list(issues.keys())
    issue_keys.sort(reverse=True, key=lambda x: len(issues[x].filenames))
    for issue_key in issue_keys:
        issue = issues[issue_key]
        print(issue_key, end='')
        print(f', in {len(issue.filenames)} files, on {issue.num_pages} pages, {issue.count} times')
        filenames = sorted(issue.filenames, reverse=True, key=lambda x: len(issue.filename_to_issues[x]))
        for filename in filenames:
            page_counts = issue.filename_to_issues[filename]
            page_counts = ' '.join([f'{page} ({count}x)' if count > 1 else f'{page}' for (page, count) in page_counts])
            print(f'  {filename} {page_counts}')
        print()
    print()

    print('Stacks:')
    keys = list(stack_to_files.keys())
    keys.sort(key=lambda x: len(stack_to_files[x]), reverse=True)
    for stack in reversed(keys):
        files = stack_to_files[stack]
        print(stack.decode('utf-8', 'backslashreplace'), end='')
        print(f'In {len(files)} files:')
        for file in files:
            print(f'    {file}')
        print()

    percent = 100 * num_crashes / len(results)
    print(f'{num_crashes} crashes ({percent:.1f}%)')
    print(f'{len(keys)} distinct crash stacks')

    percent = 100 * len(failed_files) / len(results)
    print()
    print(f'{len(failed_files)} failed to open ({percent:.1f}%)')
    for f in failed_files:
        print(f'    {f}')
    print()

    percent = 100 * num_files_with_password / len(results)
    print(f'{num_files_with_password} files with password ({percent:.1f}%)')
    print()

    percent = 100 * num_files_without_issues / len(results)
    print(f'{num_files_without_issues} files without issues ({percent:.1f}%)')
    print()


if __name__ == '__main__':
    main()
