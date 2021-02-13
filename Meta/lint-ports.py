#!/usr/bin/env python3

import os
import re

# Matches e.g. "| [`bash`]..." and captures "bash" in group 1
PORT_TABLE_REGEX = re.compile(r'^\| \[`([^`]+)`\][^`]+$', re.MULTILINE)

PORT_TABLE_FILE = 'AvailablePorts.md'
IGNORE_FILES = {'.gitignore', '.port_include.sh', PORT_TABLE_FILE, 'build_all.sh', 'ReadMe.md'}


def read_port_table(filename):
    with open(filename, 'r') as fp:
        return set(PORT_TABLE_REGEX.findall(fp.read()))


def read_port_dirs():
    ports = set()
    all_good = True
    for entry in os.listdir():
        if entry in IGNORE_FILES:
            continue
        if not os.path.isdir(entry):
            print('"Ports/{}" is neither a port (not a directory) nor an ignored file?!'.format(entry))
            all_good = False
            continue
        if not os.path.exists(entry + '/package.sh'):
            print('"Ports/{}/" is missing its package.sh?!'.format(entry))
            all_good = False
            continue
        ports.add(entry)

    return ports, all_good


def run():
    from_table = read_port_table(PORT_TABLE_FILE)
    from_fs, all_good = read_port_dirs()

    if from_table - from_fs:
        all_good = False
        print('AvailablePorts.md lists ports that do not appear in the file system:')
        for port in sorted(from_table - from_fs):
            print('    {}'.format(port))

    if from_fs - from_table:
        all_good = False
        print('AvailablePorts.md is missing the following ports:')
        for port in sorted(from_fs - from_table):
            print('    {}'.format(port))

    if not all_good:
        exit(1)

    print('No issues found.')


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/../Ports")
    # Ignore argv
    run()
