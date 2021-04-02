#!/usr/bin/env python3

import os
import re
import sys

# Matches e.g. "| [`bash`]..." and captures "bash" in group 1
PORT_TABLE_REGEX = re.compile(r'^\| \[`([^`]+)`\][^`]+$', re.MULTILINE)

PORT_TABLE_FILE = 'AvailablePorts.md'
IGNORE_FILES = {
    '.gitignore',
    '.port_include.sh',
    PORT_TABLE_FILE,
    'build_all.sh',
    'build_installed.sh',
    'ReadMe.md'
}


def read_port_table(filename):
    """Open a file and find all PORT_TABLE_REGEX matches.

    Args:
        filename (str): file name

    Returns:
        set: all PORT_TABLE_REGEX matches
    """
    with open(filename, 'r') as fp:
        return set(PORT_TABLE_REGEX.findall(fp.read()))


def read_port_dirs():
    """Check Ports directory for unexpected files and check each port has a package.sh file.

    Returns:
        list: all ports (set), errors encountered (bool)
    """

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
    """Check Ports directory contents for errors."""

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
        sys.exit(1)

    print('No issues found.')


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/../Ports")
    run()
