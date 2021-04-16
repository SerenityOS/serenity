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
    'README.md',
    '.hosted_defs.sh'
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
        list: all ports (set), no errors encountered (bool)
    """

    ports = set()
    all_good = True
    for entry in os.listdir():
        if entry in IGNORE_FILES:
            continue
        if not os.path.isdir(entry):
            print(f"Ports/{entry} is neither a port (not a directory) nor an ignored file?!")
            all_good = False
            continue
        if not os.path.exists(entry + '/package.sh'):
            print(f"Ports/{entry}/ is missing its package.sh?!")
            all_good = False
            continue
        ports.add(entry)

    return ports, all_good


def check_package_files(ports):
    """Check port package.sh file for required properties.

    Args:
        ports (set): List of all ports to check

    Returns:
        bool: no errors encountered
    """

    packages = set()
    all_good = True
    for port in ports:
        package_file = f"{port}/package.sh"
        if not os.path.exists(package_file):
            continue
        packages.add(package_file)

    properties = ['port', 'version', 'files', 'auth_type']
    for package in packages:
        with open(package, 'r') as fp:
            data = fp.read()
            for p in properties:
                if not re.findall(f"^{p}=", data, re.M):
                    print(f"Ports/{package} is missing '{p}'")
                    all_good = False

    return all_good


def run():
    """Check Ports directory and package files for errors."""

    from_table = read_port_table(PORT_TABLE_FILE)
    ports, all_good = read_port_dirs()

    if from_table - ports:
        all_good = False
        print('AvailablePorts.md lists ports that do not appear in the file system:')
        for port in sorted(from_table - ports):
            print(f"    {port}")

    if ports - from_table:
        all_good = False
        print('AvailablePorts.md is missing the following ports:')
        for port in sorted(ports - from_table):
            print(f"    {port}")

    if not check_package_files(ports):
        all_good = False

    if not all_good:
        sys.exit(1)

    print('No issues found.')


if __name__ == '__main__':
    os.chdir(f"{os.path.dirname(__file__)}/../Ports")
    run()
