# Based on the example .ycm_extra_conf.py from YouCompleteMe, adapted
# for SerenityOS.
#
# This file is NOT licensed under the GPLv3, which is the license for the rest
# of YouCompleteMe.
#
# Here's the license text for this file:
#
# This is free and unencumbered software released into the public domain.
#
# Anyone is free to copy, modify, publish, use, compile, sell, or
# distribute this software, either in source code form or as a compiled
# binary, for any purpose, commercial or non-commercial, and by any
# means.
#
# In jurisdictions that recognize copyright laws, the author or authors
# of this software dedicate any and all copyright interest in the
# software to the public domain. We make this dedication for the benefit
# of the public at large and to the detriment of our heirs and
# successors. We intend this dedication to be an overt act of
# relinquishment in perpetuity of all present and future rights to this
# software under copyright law.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
# IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
# OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
# ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
# OTHER DEALINGS IN THE SOFTWARE.
#
# For more information, please refer to <http://unlicense.org/>

import os
import subprocess
import ycm_core

DIR_OF_THIS_SCRIPT = os.path.abspath(os.path.dirname(__file__))
SOURCE_EXTENSIONS = ['.cpp', '.c']

gcc_path = None
for serenity_arch in ['x86_64', 'aarch64']:
    candidate_gcc_path = os.path.join(
        DIR_OF_THIS_SCRIPT, 'Toolchain',
        'Local', serenity_arch, 'bin', f'{serenity_arch}-pc-serenity-gcc'
    )
    if os.path.isfile(candidate_gcc_path):
        gcc_path = candidate_gcc_path
        break

serenity_flags = [
    '-D__serenity__',
    '-D__unix__'
]

if gcc_path:
    gcc_output = subprocess.check_output(
      [gcc_path, '-E', '-Wp,-v', '-'],
      stdin=subprocess.DEVNULL, stderr=subprocess.STDOUT
    ).rstrip().decode('utf8').split("\n")

    for line in gcc_output:
        if not line.startswith(' '):
            continue
        include_path = line.lstrip()
        if '/../Build/' in include_path:
            continue
        serenity_flags.extend(('-isystem', include_path))

database = ycm_core.CompilationDatabase(os.path.join(DIR_OF_THIS_SCRIPT, f'Build/{serenity_arch}'))


def is_header_file(filename):
    extension = os.path.splitext(filename)[1]
    return extension in ['.h', '.hxx', '.hpp', '.hh']


def find_corresponding_source_file(filename):
    if is_header_file(filename):
        basename = os.path.splitext(filename)[0]
        for extension in SOURCE_EXTENSIONS:
            replacement_file = basename + extension
            if os.path.exists(replacement_file):
                return replacement_file
    return filename


def startswith_any(string, prefixes):
    for prefix in prefixes:
        if string.startswith(prefix):
            return True
    return False


def Settings(**kwargs):
    if kwargs['language'] != 'cfamily':
        return {}
    # If the file is a header, try to find the corresponding source file and
    # retrieve its flags from the compilation database if using one. This is
    # necessary since compilation databases don't have entries for header files.
    # In addition, use this source file as the translation unit. This makes it
    # possible to jump from a declaration in the header file to its definition
    # in the corresponding source file.
    filename = find_corresponding_source_file(kwargs['filename'])

    compilation_info = database.GetCompilationInfoForFile(filename)
    if not compilation_info.compiler_flags_:
        return {}

    ignored_flags = [
        '--sysroot',
        '-fzero-call-used-regs=used-gpr',
    ]

    final_flags = [flag for flag in compilation_info.compiler_flags_ if not startswith_any(flag, ignored_flags)]
    final_flags.extend(serenity_flags)

    return {
        'flags': final_flags,
        'include_paths_relative_to_dir': DIR_OF_THIS_SCRIPT,
        'override_filename': filename
    }
