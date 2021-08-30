#!/usr/bin/env python3

import os
import re
import sys
import subprocess

# Matches e.g. "| [`bash`](bash/) | GNU Bash | 5.0 | https://www.gnu.org/software/bash/ |"
# and captures "bash" in group 1, "bash/" in group 2, "<spaces>" in group 3, "GNU Bash" in group 4, "5.0" in group 5
# and "https://www.gnu.org/software/bash/" in group 6.
PORT_TABLE_REGEX = re.compile(
    r'^\| \[`([^`]+)`\]\(([^\)]+)\)([^\|]+) \| ([^\|]+) \| ([^\|]+?) \| ([^\|]+) \|+$', re.MULTILINE
)

# Matches non-abbreviated git hashes
GIT_HASH_REGEX = re.compile(r'^[0-9a-f]{40}$')

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
        filename (str): filename

    Returns:
        set: all PORT_TABLE_REGEX matches
    """
    ports = {}
    with open(filename, 'r') as fp:
        matches = PORT_TABLE_REGEX.findall(fp.read())
        for match in matches:
            line_len = sum([len(part) for part in match])
            ports[match[0]] = {
                "dir_ref": match[1],
                "name": match[2].strip(),
                "version": match[4].strip(),
                "url": match[5].strip(),
                "line_len": line_len
            }
    return ports


def read_port_dirs():
    """Check Ports directory for unexpected files and check each port has a package.sh file.

    Returns:
        list: all ports (set), no errors encountered (bool)
    """

    ports = {}
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
        ports[entry] = get_port_properties(entry)

    return ports, all_good


PORT_PROPERTIES = ('port', 'version', 'files', 'auth_type')


def get_port_properties(port):
    """Retrieves common port properties from its package.sh file.

    Returns:
        dict: keys are values from PORT_PROPERTIES, values are from the package.sh file
    """

    props = {}
    for prop in PORT_PROPERTIES:
        res = subprocess.run(f"cd {port}; exec ./package.sh showproperty {prop}", shell=True, capture_output=True)
        if res.returncode == 0:
            props[prop] = res.stdout.decode('utf-8').strip()
        else:
            print((
                f'Executing "./package.sh showproperty {prop}" script for port {port} failed with '
                f'exit code {res.returncode}, output from stderr:\n{res.stderr.decode("utf-8").strip()}'
            ))
            props[prop] = ''
    return props


def check_package_files(ports):
    """Check port package.sh file for required properties.

    Args:
        ports (list): List of all ports to check

    Returns:
        bool: no errors encountered
    """

    all_good = True
    for port in ports:
        package_file = f"{port}/package.sh"
        if not os.path.exists(package_file):
            continue

        props = get_port_properties(port)

        if not props['auth_type'] in ('sha256', 'sig', ''):
            print(f"Ports/{port} uses invalid signature algorithm '{props['auth_type']}' for 'auth_type'")
            all_good = False

        for prop in PORT_PROPERTIES:
            if prop == 'auth_type' and re.match('^https://github.com/SerenityPorts/', props["files"]):
                continue
            if props[prop] == '':
                print(f"Ports/{port} is missing required property '{prop}'")
                all_good = False

    return all_good


def check_available_ports(from_table, ports):
    """Check AvailablePorts.md for correct properties.

    Args:
        from_table (dict): Ports table from AvailablePorts.md
        ports (dict): Dictionary with port properties from package.sh

    Returns:
        bool: no errors encountered
    """

    all_good = True

    previous_line_len = None

    for port in from_table.keys():
        if previous_line_len is None:
            previous_line_len = from_table[port]["line_len"]
        if previous_line_len != from_table[port]["line_len"]:
            print(f"Table row for port {port} is improperly aligned with other rows.")
            all_good = False
        else:
            previous_line_len = from_table[port]["line_len"]

        actual_ref = from_table[port]["dir_ref"]
        expected_ref = f"{port}/"
        if actual_ref != expected_ref:
            print((
                f'Directory link target in AvailablePorts.md for port {port} is '
                f'incorrect, expected "{expected_ref}", found "{actual_ref}"'
            ))
            all_good = False

        actual_version = from_table[port]["version"]
        expected_version = ports[port]["version"]
        if GIT_HASH_REGEX.match(expected_version):
            expected_version = expected_version[0:7]
        if expected_version == "git":
            expected_version = ""
        if actual_version != expected_version:
            print((
                f'Version in AvailablePorts.md for port {port} is incorrect, '
                f'expected "{expected_version}", found "{actual_version}"'
            ))
            all_good = False

    return all_good


def run():
    """Check Ports directory and package files for errors."""

    from_table = read_port_table(PORT_TABLE_FILE)
    ports, all_good = read_port_dirs()

    from_table_set = set(from_table.keys())
    ports_set = set(ports.keys())

    if from_table_set - ports_set:
        all_good = False
        print('AvailablePorts.md lists ports that do not appear in the file system:')
        for port in sorted(from_table_set - ports_set):
            print(f"    {port}")

    if ports_set - from_table_set:
        all_good = False
        print('AvailablePorts.md is missing the following ports:')
        for port in sorted(ports_set - from_table_set):
            print(f"    {port}")

    if not check_package_files(ports.keys()):
        all_good = False

    if not check_available_ports(from_table, ports):
        all_good = False

    if not all_good:
        sys.exit(1)

    print('No issues found.')


if __name__ == '__main__':
    os.chdir(f"{os.path.dirname(__file__)}/../Ports")
    run()
