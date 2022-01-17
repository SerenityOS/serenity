#!/usr/bin/env python3

import os
import re
import sys
import subprocess
from pathlib import Path
from tempfile import NamedTemporaryFile

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

# Matches port names in Ports/foo/ReadMe.md
PORT_NAME_REGEX = re.compile(r'([ .()[\]{}\w-]+)\.patch')
PORTS_MISSING_DESCRIPTIONS = {
    'Another-World',
    'binutils',
    'chester',
    'cmatrix',
    'c-ray',
    'curl',
    'dash',
    'diffutils',
    'dosbox-staging',
    'dropbear',
    'ed',
    'emu2',
    'epsilon',
    'figlet',
    'flex',
    'fontconfig',
    'freeciv',
    'freedink',
    'freetype',
    'gawk',
    'gcc',
    'gdb',
    'genemu',
    'gettext',
    'git',
    'gltron',
    'gmp',
    'gnucobol',
    'gnupg',
    'gnuplot',
    'gsl',
    'harfbuzz',
    'indent',
    'jq',
    'klong',
    'libassuan',
    'libgcrypt',
    'libgd',
    'libgpg-error',
    'libiconv',
    'libicu',
    'libjpeg',
    'libksba',
    'libmodplug',
    'liboggz',
    'libpng',
    'libpuffy',
    'libsodium',
    'libvorbis',
    'libzip',
    'lua',
    'm4',
    'make',
    'mandoc',
    'mbedtls',
    'milkytracker',
    'mrsh',
    'mruby',
    'nano',
    'ncurses',
    'neofetch',
    'nethack',
    'ninja',
    'npiet',
    'npth',
    'ntbtls',
    'nyancat',
    'oksh',
    'openssh',
    'openssl',
    'openttd',
    'opentyrian',
    'p7zip',
    'patch',
    'pcre2',
    'pfetch',
    'php',
    'pkgconf',
    'pt2-clone',
    'qt6-qtbase',
    'ruby',
    'sam',
    'scummvm',
    'SDL2_image',
    'SDL2_mixer',
    'SDL2_net',
    'SDL2_ttf',
    'sl',
    'sqlite',
    'tcl',
    'tinycc',
    'tr',
    'tuxracer',
    'vitetris',
    'wget',
    'xz',
    'zsh',
    'zstd',
}

# FIXME: Once everything is converted into `git format-patch`-style patches,
#        enable this to allow only `git format-patch` patches.
REQUIRE_GIT_PATCHES = False
GIT_PATCH_SUBJECT_RE = re.compile(r'Subject: (.*)\n')


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
    package_sh_command = f"./package.sh showproperty {' '.join(PORT_PROPERTIES)}"
    res = subprocess.run(f"cd {port}; exec {package_sh_command}", shell=True, capture_output=True)
    if res.returncode == 0:
        results = res.stdout.decode('utf-8').split('\n\n')
        props = {prop: results[i].strip() for i, prop in enumerate(PORT_PROPERTIES)}
    else:
        print((
            f'Executing "{package_sh_command}" script for port {port} failed with '
            f'exit code {res.returncode}, output from stderr:\n{res.stderr.decode("utf-8").strip()}'
        ))
        props = {x: '' for x in PORT_PROPERTIES}
    return props


def check_package_files(ports):
    """Check port package.sh file for required properties.

    Args:
        ports (list): List of all ports to check

    Returns:
        bool: no errors encountered
    """

    all_good = True
    for port in ports.keys():
        package_file = f"{port}/package.sh"
        if not os.path.exists(package_file):
            continue
        props = ports[port]
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


def get_and_check_port_patch_list(ports):
    """Checks all port patches and returns the port list/properties

    Args:
        ports (list): List of all ports to check

    Returns:
        all_good (bool): No errors encountered
        all_properties (dict): Mapping of port to port properties
    """
    all_port_properties = {}
    all_good = True

    for port in ports:
        patches_directory = f"{port}/patches"

        if not os.path.exists(patches_directory):
            continue

        if not os.path.isdir(patches_directory):
            print(f"Ports/{port}/patches exists, but is not a directory. This is not right!")
            all_good = False
            continue

        patches_path = Path(patches_directory)
        patches_readme_path = patches_path / "ReadMe.md"
        patch_files = set(patches_path.glob("*.patch"))
        non_patch_files = set(patches_path.glob("*")) - patch_files - {patches_readme_path}

        port_properties = {
            "patches_path": patches_path,
            "patches_readme_path": patches_readme_path,
            "patch_files": patch_files,
            "non_patch_files": non_patch_files
        }
        all_port_properties[port] = port_properties

        if len(non_patch_files) != 0:
            print("Ports/{port}/patches contains the following non-patch files:",
                  ', '.join(x.name for x in non_patch_files))
            all_good = False

    return all_good, all_port_properties


def check_descriptions_for_port_patches(patches):
    """Ensure that ports containing patches have them documented.

    Args:
        patches (dict): Dictionary mapping ports to all their patches

    Returns:
        bool: no errors encountered
    """

    all_good = True
    for port, properties in patches.items():
        patches_readme_path = properties["patches_readme_path"]
        patch_files = properties["patch_files"]

        readme_file_exists = patches_readme_path.exists()
        if len(patch_files) == 0:
            print(f"Ports/{port}/patches exists, but contains no patches", end="")
            if readme_file_exists:
                print(", yet it contains a ReadMe.md")
            else:
                print()
            all_good = False
            continue

        if not readme_file_exists:
            if port not in PORTS_MISSING_DESCRIPTIONS:
                print(f"Ports/{port}/patches contains patches but no ReadMe.md describing them")
                all_good = False
            continue

        with open(str(patches_readme_path), 'r', encoding='utf-8') as f:
            readme_contents = []
            for line in f:
                if not line.startswith('#'):
                    continue
                match = PORT_NAME_REGEX.search(line)
                if match:
                    readme_contents.append(match.group(1))

        patch_names = set(Path(x).stem for x in patch_files)

        patches_ok = True
        for patch_name in patch_names:
            if patch_name not in readme_contents:
                if port not in PORTS_MISSING_DESCRIPTIONS:
                    print(f"Ports/{port}/patches/{patch_name}.patch does not appear to be described in"
                          " the corresponding ReadMe.md")
                    all_good = False
                    patches_ok = False

        for patch_name in readme_contents:
            if patch_name not in patch_names:
                if port not in PORTS_MISSING_DESCRIPTIONS:
                    print(f"Ports/{port}/patches/{patch_name}.patch is described in ReadMe.md, "
                          "but does not actually exist")
                    all_good = False
                    patches_ok = False

        if port in PORTS_MISSING_DESCRIPTIONS and patches_ok:
            print(f"Ports/{port}/patches are all described correctly, but the port is marked "
                  "as MISSING_DESCRIPTIONS, make sure to remove it from the list in lint-ports.py")
            all_good = False

    return all_good


def try_parse_git_patch(path_to_patch):
    with open(path_to_patch, 'rb') as f:
        contents_of_patch = f.read()

    with NamedTemporaryFile('r+b') as message_file:
        res = subprocess.run(
            f"git mailinfo {message_file.name} /dev/null",
            shell=True,
            capture_output=True,
            input=contents_of_patch)

        if res.returncode != 0:
            return None

        message = message_file.read().decode('utf-8')
        subject = GIT_PATCH_SUBJECT_RE.search(res.stdout.decode("utf-8"))
        if subject:
            message = subject.group(1) + "\n" + message

        return message


def check_patches_are_git_patches(patches):
    """Ensure that all patches are patches made by (or compatible with) `git format-patch`.

    Args:
        patches (dict): Dictionary mapping ports to all their patches

    Returns:
        bool: no errors encountered
    """

    all_good = True

    for port, properties in patches.items():
        for patch_path in properties["patch_files"]:
            result = try_parse_git_patch(patch_path)
            if not result:
                print(f"Ports/{port}/patches: {patch_path.stem} does not appear to be a valid "
                      "git patch.")
                all_good = False
                continue
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

    if not check_package_files(ports):
        all_good = False

    if not check_available_ports(from_table, ports):
        all_good = False

    patch_list_good, port_properties = get_and_check_port_patch_list(ports.keys())
    all_good = all_good and patch_list_good

    if not check_descriptions_for_port_patches(port_properties):
        all_good = False

    if REQUIRE_GIT_PATCHES and not check_patches_are_git_patches(port_properties):
        all_good = False

    if not all_good:
        sys.exit(1)

    print('No issues found.')


if __name__ == '__main__':
    os.chdir(f"{os.path.dirname(__file__)}/../Ports")
    run()
