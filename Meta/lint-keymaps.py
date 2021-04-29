#!/usr/bin/env python3

import json
import os
import sys

PERMITTED_MAPS = ['map', 'shift_map', 'alt_map', 'altgr_map', 'shift_altgr_map']
REQUIRED_MAPS = ['map', 'shift_map', 'alt_map']
# See Userland/Libraries/LibKeyboard/CharacterMapFile.cpp
# and Userland/Libraries/LibKeyboard/CharacterMap.cpp.
GOOD_MAP_LENGTHS = {90, 128}


def report(filename, problem):
    """Print a lint problem to stdout.

    Args:
        filename (str): keymap filename
        problem (str): problem message
    """
    print('{}: {}'.format(filename, problem))


def validate_single_map(filename, mapname, values):
    """Validate a key map.

    Args:
        filename (str): keymap filename
        mapname (str): map name (altgr_map, alt_map, shift_altgr_map)
        values (list): key values

    Returns:
        bool: key map is valid
    """

    all_good = True

    if not isinstance(values, list):
        report(filename, '"{}" is not an array'.format(mapname))
        return False  # Cannot continue other checks

    if not any(values):
        report(filename, 'no values set in {}'.format(mapname))
        all_good = False

    for i, c in enumerate(values):
        if len(c) > 1:
            report(filename, 'more than one character ("{}") for charmap index {} of {}'.format(c, i, mapname))
            all_good = False

    if len(values) == 0:
        report(filename, 'map {} is empty.'.format(mapname))
        all_good = False

    if len(values) not in GOOD_MAP_LENGTHS:
        report(filename, 'length {} of map {} is suspicious. Off-by-one?'.format(len(values), mapname))
        all_good = False

    return all_good


def validate_fullmap(filename, fullmap):
    """Validate a full key map for all map names (including maps for key modifiers).

    Args:
        filename (str): keymap filename
        fullmap (dict): key mappings

    Returns:
        bool: keymap file contains valid key mappings
    """

    all_good = True

    if not isinstance(fullmap, dict):
        report(filename, 'is not an object')
        return False  # Cannot continue other checks

    for name, map_ in fullmap.items():
        if name not in PERMITTED_MAPS:
            report(filename, 'contains unknown entry {}'.format(name))
            all_good = False

        all_good &= validate_single_map(filename, name, map_)

    for name in REQUIRED_MAPS:
        if name not in fullmap:
            report(filename, 'map {} is missing'.format(name))
            all_good = False

    if 'altgr_map' in fullmap and 'alt_map' in fullmap and fullmap['altgr_map'] == fullmap['alt_map']:
        report(filename, 'altgr_map is identical to alt_map. Remove altgr_map for the same effect.')
        report(filename, '(Or add new characters!)')
        all_good = False

    if 'shift_altgr_map' in fullmap and 'alt_map' in fullmap and fullmap['shift_altgr_map'] == fullmap['alt_map']:
        report(filename, 'shift_altgr_map is identical to alt_map. Remove shift_altgr_map for the same effect.')
        report(filename, '(Or add new characters!)')
        all_good = False

    return all_good


def run_with(filenames):
    """Check list of keymap files for errors.

    Args:
        filenames (list): keymap files to check

    Returns:
        bool: All keymap files are valid
    """

    passed = 0
    for filename in filenames:
        with open(filename, 'r') as fp:
            fullmap = json.load(fp)
        if validate_fullmap(filename, fullmap):
            passed += 1

    print('{} out of {} keymaps passed.'.format(passed, len(filenames)))
    return passed == len(filenames)


def list_files_here():
    """Retrieve a list of all '.json' files in the working directory.

    Returns:
        list: JSON filenames
    """

    filelist = []
    for filename in os.listdir():
        if filename.endswith('.json'):
            filelist.append(filename)
        else:
            report(filename, 'weird filename (ignored)')
    # Files are in "filesystem" order. Sort them for slightly more
    # aesthetically pleasing output.
    filelist.sort()
    return filelist


def run_here():
    """Check all keymap files in the working directory for errors.

    Returns:
        bool: All keymap files are valid
    """

    return run_with(list_files_here())


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/../Base/res/keymaps/")
    if not run_here():
        sys.exit(1)
