#!/usr/bin/env python3

import os
import pathlib
import re
import subprocess
import sys

# Ensure copyright headers match this format and are followed by a blank line:
# /*
#  * Copyright (c) YYYY(-YYYY), Whatever
#  * ... more of these ...
#  *
#  * SPDX-License-Identifier: BSD-2-Clause
#  */
GOOD_LICENSE_HEADER_PATTERN = re.compile(
    '^/\\*\n' +
    '( \\* Copyright \\(c\\) [0-9]{4}(-[0-9]{4})?, .*\n)+' +
    ' \\*\n' +
    ' \\* SPDX-License-Identifier: BSD-2-Clause\n' +
    ' \\*/\n' +
    '\n')
LICENSE_HEADER_CHECK_EXCLUDES = {
    'AK/Checked.h',
    'AK/Function.h',
    'Meta/Lagom/Tools/CodeGenerators/JSSpecCompiler/Tests/',
    'Userland/Libraries/LibJS/SafeFunction.h',
    'Userland/Libraries/LibELF/ELFABI.h',
    'Userland/Libraries/LibCodeComprehension/Cpp/Tests/',
    'Userland/Libraries/LibCpp/Tests/parser/',
    'Userland/Libraries/LibCpp/Tests/preprocessor/'
}

# We check that "#pragma once" is present
PRAGMA_ONCE_STRING = '#pragma once'
PRAGMA_ONCE_CHECK_EXCLUDES = {
    'Userland/Libraries/LibC/assert.h',
    'Ladybird/AppKit/System/Detail/Header.h',
    'Ladybird/AppKit/System/Detail/Footer.h',
}

# We make sure that there's a blank line before and after pragma once
GOOD_PRAGMA_ONCE_PATTERN = re.compile('(^|\\S\n\n)#pragma once(\n\n\\S.|$)')

# LibC is supposed to be a system library; don't mention the directory.
BAD_INCLUDE_LIBC = re.compile("# *include <LibC/")

# Serenity C++ code must not use LibC's or libc++'s complex number implementation.
BAD_INCLUDE_COMPLEX = re.compile("# *include <c[c]?omplex")

# Make sure that all includes are either system includes or immediately resolvable local includes
ANY_INCLUDE_PATTERN = re.compile('^ *# *include\\b.*[>"](?!\\)).*$', re.M)
SYSTEM_INCLUDE_PATTERN = re.compile("^ *# *include *<([^>]+)>(?: /[*/].*)?$")
LOCAL_INCLUDE_PATTERN = re.compile('^ *# *include *"([^>]+)"(?: /[*/].*)?$')

INCLUDE_CHECK_EXCLUDES = {
    "Userland/Libraries/LibCodeComprehension/Cpp/Tests/",
    "Userland/Libraries/LibCpp/Tests/parser/",
    "Userland/Libraries/LibCpp/Tests/preprocessor/",
}

LOCAL_INCLUDE_ROOT_OVERRIDES = {
    "Meta/Lagom/Tools/CodeGenerators/JSSpecCompiler/",
}

LOCAL_INCLUDE_SUFFIX_EXCLUDES = [
    # Some Qt files are required to include their .moc files, which will be located in a deep
    # subdirectory that we won't find from here.
    '.moc',
]


def should_check_file(filename):
    if not filename.endswith('.cpp') and not filename.endswith('.h'):
        return False
    if filename.startswith('Base/'):
        return False
    if filename == 'Kernel/Devices/HID/VirtIO/EvDevDefinitions.h':
        return False
    if filename == 'Kernel/FileSystem/Ext2FS/Definitions.h':
        return False
    if filename == 'Kernel/FileSystem/FUSE/Definitions.h':
        return False
    return True


def find_files_here_or_argv():
    if len(sys.argv) > 1:
        raw_list = sys.argv[1:]
    else:
        process = subprocess.run(["git", "ls-files"], check=True, capture_output=True)
        raw_list = process.stdout.decode().strip('\n').split('\n')

    return filter(should_check_file, raw_list)


def is_in_prefix_list(filename, prefix_list):
    return any(
        filename.startswith(prefix) for prefix in prefix_list
    )


def find_matching_prefix(filename, prefix_list):
    matching_prefixes = [prefix for prefix in prefix_list if filename.startswith(prefix)]
    assert len(matching_prefixes) <= 1
    return matching_prefixes[0] if matching_prefixes else None


def run():
    errors_license = []
    errors_pragma_once_bad = []
    errors_pragma_once_missing = []
    errors_include_libc = []
    errors_include_weird_format = []
    errors_include_missing_local = []
    errors_include_bad_complex = []

    for filename in find_files_here_or_argv():
        with open(filename, "r") as f:
            file_content = f.read()
        if not is_in_prefix_list(filename, LICENSE_HEADER_CHECK_EXCLUDES):
            if not GOOD_LICENSE_HEADER_PATTERN.search(file_content):
                errors_license.append(filename)
        if filename.endswith('.h'):
            if is_in_prefix_list(filename, PRAGMA_ONCE_CHECK_EXCLUDES):
                # File was excluded
                pass
            elif GOOD_PRAGMA_ONCE_PATTERN.search(file_content):
                # Excellent, the formatting is correct.
                pass
            elif PRAGMA_ONCE_STRING in file_content:
                # Bad, the '#pragma once' is present but it's formatted wrong.
                errors_pragma_once_bad.append(filename)
            else:
                # Bad, the '#pragma once' is missing completely.
                errors_pragma_once_missing.append(filename)
        if BAD_INCLUDE_LIBC.search(file_content):
            errors_include_libc.append(filename)
        if BAD_INCLUDE_COMPLEX.search(file_content):
            errors_include_bad_complex.append(filename)
        if not is_in_prefix_list(filename, INCLUDE_CHECK_EXCLUDES):
            if include_root := find_matching_prefix(filename, LOCAL_INCLUDE_ROOT_OVERRIDES):
                local_include_root = pathlib.Path(include_root)
            else:
                local_include_root = pathlib.Path(filename).parent
            for include_line in ANY_INCLUDE_PATTERN.findall(file_content):
                if SYSTEM_INCLUDE_PATTERN.match(include_line):
                    # Don't try to resolve system-style includes, as these might depend on generators.
                    continue
                local_match = LOCAL_INCLUDE_PATTERN.match(include_line)
                if local_match is None:
                    print(f"Cannot parse include-line '{include_line}' in {filename}")
                    if filename not in errors_include_weird_format:
                        errors_include_weird_format.append(filename)
                    continue

                relative_filename = local_match.group(1)
                referenced_file = local_include_root.joinpath(relative_filename)

                if referenced_file.suffix in LOCAL_INCLUDE_SUFFIX_EXCLUDES:
                    continue

                if not referenced_file.exists():
                    print(f"In {filename}: Cannot find {referenced_file}")
                    if filename not in errors_include_missing_local:
                        errors_include_missing_local.append(filename)

    have_errors = False
    if errors_license:
        print("Files with bad licenses:", " ".join(errors_license))
        have_errors = True
    if errors_pragma_once_missing:
        print("Files without #pragma once:", " ".join(errors_pragma_once_missing))
        have_errors = True
    if errors_pragma_once_bad:
        print("Files with a bad #pragma once:", " ".join(errors_pragma_once_bad))
        have_errors = True
    if errors_include_libc:
        print(
            "Files that include a LibC header using #include <LibC/...>:",
            " ".join(errors_include_libc),
        )
        have_errors = True
    if errors_include_weird_format:
        print(
            "Files that contain badly-formatted #include statements:",
            " ".join(errors_include_weird_format),
        )
        have_errors = True
    if errors_include_missing_local:
        print(
            "Files that #include a missing local file:",
            " ".join(errors_include_missing_local),
        )
        have_errors = True
    if errors_include_bad_complex:
        print(
             "Files that include a non-AK complex header:",
             " ".join(errors_include_bad_complex),
        )
        have_errors = True

    if have_errors:
        sys.exit(1)


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/..")
    run()
