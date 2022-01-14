/*
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/StringView.h>
#include <AK/Try.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

static ErrorOr<bool> is_regular_file(int fd)
{
    auto st = TRY(Core::System::fstat(fd));
    return S_ISREG(st.st_mode);
}

static ErrorOr<size_t> read_all(int fd, Bytes buffer)
{
    size_t nread = 0;
    while (nread < buffer.size()) {
        auto current_nread = TRY(Core::System::read(fd, buffer.slice(nread)));
        if (current_nread == 0)
            break;
        nread += current_nread;
    }
    return nread;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/utilities/cmp.html
static ErrorOr<int> cmp_main(int argc, char** argv)
{
    TRY(Core::System::pledge("stdio rpath"));
    bool silent = false;
    bool verbose = false;
    StringView path1;
    StringView path2;

    Core::ArgsParser args_parser;
    args_parser.add_option(silent, "Do not print anything, just set the exit code", "silent", 's');
    args_parser.add_option(verbose, "Output each differing byte", "verbose", 'l');
    args_parser.add_positional_argument(path1, "File to compare", "file1");
    args_parser.add_positional_argument(path2, "File to compare", "file2");
    args_parser.parse(argc, argv);

    if (silent && verbose)
        return Error::from_string_literal("Options -s and -l are incompatible"sv);

    int fd1 = path1 == "-"sv ? STDIN_FILENO : TRY(Core::System::open(path1, O_RDONLY));
    int fd2 = path2 == "-"sv ? STDIN_FILENO : TRY(Core::System::open(path2, O_RDONLY));

    // Most of the time, callers are only interested whether the contents are the same, so we have an
    // optimized implementation for silent mode.
    // If we're dealing with a weird file (e.g. a socket or a pipe), or we have to keep track of our position
    // as we're not in silent mode, we perform naive byte-by-byte reads.
    if (!silent || !TRY(is_regular_file(fd1)) || !TRY(is_regular_file(fd2))) {
        auto* file1 = fd1 == STDIN_FILENO ? stdin : TRY(Core::System::fdopen(fd1, "r"));
        auto* file2 = fd2 == STDIN_FILENO ? stdin : TRY(Core::System::fdopen(fd2, "r"));

        bool different = false;
        for (size_t line = 1, byte = 1;; ++byte) {
            auto char1 = getc(file1);
            auto char2 = getc(file2);
            if (char1 == EOF || char2 == EOF)
                break;

            if (char1 != char2) {
                if (verbose) {
                    different = true;
                    outln("{} {:o} {:o}", byte, char1, char2);
                } else if (silent) {
                    return 1;
                } else {
                    outln("{} {} differ: char {}, line {}", path1, path2, byte, line);
                    return 1;
                }
            }

            if (char1 == '\n')
                ++line;
        }

        if (feof(file1) != feof(file2)) {
            warnln("cmp: EOF on {}", feof(file1) ? path1 : path2);
            return 1;
        }

        return different;
    } else {
        u8 buf1[4096];
        u8 buf2[4096];
        while (true) {
            auto nread1 = TRY(read_all(fd1, { buf1, sizeof(buf1) }));
            auto nread2 = TRY(read_all(fd2, { buf2, sizeof(buf2) }));

            if (nread1 != nread2)
                return 1;
            if (nread1 == 0)
                return 0;

            return memcmp(buf1, buf2, nread1) != 0;
        }
    }
}

// Note: this utility can't use LibMain, as we have to return exit code 2 if an error happens.
int main(int argc, char** argv)
{
    auto result = cmp_main(argc, argv);
    if (result.is_error()) {
        auto error = result.release_error();
        warnln("\033[31;1mRuntime error\033[0m: {}", error);
#ifdef __serenity__
        dbgln("\033[31;1mExiting with runtime error\033[0m: {}", error);
#endif
        return 2;
    }
    return result.value();
}
