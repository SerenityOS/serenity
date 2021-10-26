#!/usr/bin/env python3

import os
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
    'Userland/Libraries/LibC/elf.h',
    'Userland/DevTools/HackStudio/LanguageServers/Cpp/Tests/',
    'Userland/Libraries/LibCpp/Tests/parser/',
    'Userland/Libraries/LibCpp/Tests/preprocessor/'
}

# We check that "#pragma once" is present
PRAGMA_ONCE_STRING = '#pragma once'

# We make sure that there's a blank line before and after pragma once
GOOD_PRAGMA_ONCE_PATTERN = re.compile('(^|\\S\n\n)#pragma once(\n\n\\S.|$)')

# We check that "#include <LibM/math.h>" is not being used
LIBM_MATH_H_INCLUDE_STRING = '#include <LibM/math.h>'

GIT_LS_FILES = ['git', 'ls-files', '--', '*.cpp', '*.h', ':!:Base', ':!:Kernel/FileSystem/ext2_fs.h']


def run():
    files = subprocess.run(GIT_LS_FILES, check=True, capture_output=True).stdout.decode().strip('\n').split('\n')
    assert len(files) > 1000

    errors_license = []
    errors_libm_math_h = []
    errors_pragma_once_bad = []
    errors_pragma_once_missing = []

    for filename in files:
        with open(filename, "r") as f:
            file_content = f.read()
        if not any(filename.startswith(forbidden_prefix) for forbidden_prefix in LICENSE_HEADER_CHECK_EXCLUDES):
            if not GOOD_LICENSE_HEADER_PATTERN.search(file_content):
                errors_license.append(filename)
        if LIBM_MATH_H_INCLUDE_STRING in file_content:
            errors_libm_math_h.append(filename)
        if filename.endswith('.h'):
            if GOOD_PRAGMA_ONCE_PATTERN.search(file_content):
                # Excellent, the formatting is correct.
                pass
            elif PRAGMA_ONCE_STRING in file_content:
                # Bad, the '#pragma once' is present but it's formatted wrong.
                errors_pragma_once_bad.append(filename)
            else:
                # Bad, the '#pragma once' is missing completely.
                errors_pragma_once_missing.append(filename)

    if errors_license:
        print("Files with bad licenses:", " ".join(errors_license))
    if errors_pragma_once_missing:
        print("Files without #pragma once:", " ".join(errors_pragma_once_missing))
    if errors_pragma_once_bad:
        print("Files with a bad #pragma once:", " ".join(errors_pragma_once_bad))
    if errors_libm_math_h:
        print("Files including LibM/math.h (include just 'math.h' instead):", " ".join(errors_libm_math_h))

    if errors_license or errors_pragma_once_missing or errors_pragma_once_bad or errors_libm_math_h:
        sys.exit(1)


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/..")
    run()
