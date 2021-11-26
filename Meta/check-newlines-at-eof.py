#!/usr/bin/env python3

import os
import subprocess
import sys


def run():
    """Check files checked in to git for trailing newlines at end of file."""
    files = subprocess.run(
        [
            "git", "ls-files", "--",
            "*.cpp",
            "*.h",
            "*.gml",
            "*.html",
            "*.js",
            "*.css",
            "*.sh",
            "*.py",
            "*.json",
            "CMake*.txt",
            "**/CMake*.txt",
            ":!:Kernel/FileSystem/ext2_fs.h",
            ':!:Userland/DevTools/HackStudio/LanguageServers/Cpp/Tests/*',
            ':!:Userland/Libraries/LibCpp/Tests/parser/*',
            ':!:Userland/Libraries/LibCpp/Tests/preprocessor/*'
        ],
        check=True,
        capture_output=True
    ).stdout.decode().strip('\n').split('\n')

    no_newline_at_eof_errors = []
    blank_lines_at_eof_errors = []

    did_fail = False
    for filename in files:
        with open(filename, "r") as f:
            f.seek(0, os.SEEK_END)

            f.seek(f.tell() - 1, os.SEEK_SET)
            if f.read(1) != '\n':
                did_fail = True
                no_newline_at_eof_errors.append(filename)
                continue

            while True:
                f.seek(f.tell() - 2, os.SEEK_SET)
                char = f.read(1)
                if not char.isspace():
                    break
                if char == '\n':
                    did_fail = True
                    blank_lines_at_eof_errors.append(filename)
                    break

    if no_newline_at_eof_errors:
        print("Files with no newline at the end:", " ".join(no_newline_at_eof_errors))
    if blank_lines_at_eof_errors:
        print("Files that have blank lines at the end:", " ".join(blank_lines_at_eof_errors))

    if did_fail:
        sys.exit(1)


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/..")
    run()
