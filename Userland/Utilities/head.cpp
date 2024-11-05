/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static int head(ByteString const& filename, bool print_filename, ssize_t line_count, ssize_t byte_count)
{
    bool is_stdin = false;
    int fd = -1;

    ScopeGuard fd_close_guard = [&fd] {
        if (fd > 0)
            close(fd);
    };

    if (filename == "" || filename == "-") {
        fd = 0;
        is_stdin = true;
    } else {
        fd = open(filename.characters(), O_RDONLY);
        if (fd < 0) {
            warnln("Failed to open {}: {}", filename, strerror(errno));
            return 1;
        }
    }

    if (print_filename) {
        if (is_stdin) {
            outln("==> standard input <==");
        } else {
            outln("==> {} <==", filename);
        }
    }

    fflush(stdout);

    size_t buffer_size = line_count != -1 ? BUFSIZ : min((size_t)BUFSIZ, (size_t)byte_count);
    char buffer[buffer_size];

    while (line_count > 0 || byte_count > 0) {
        size_t ntoread = line_count != -1 ? buffer_size : min(buffer_size, (size_t)byte_count);
        ssize_t nread = read(fd, buffer, ntoread);
        if (nread < 0) {
            perror("read");
            return 1;
        } else if (nread == 0) {
            break;
        }

        size_t ntowrite;
        if (byte_count != -1) {
            // Write out everything we've read, since we have explicitly ensured
            // that we wouldn't read more than we want to write.
            ntowrite = nread;
            byte_count -= nread;
        } else {
            // Count line breaks.
            ntowrite = 0;
            while (line_count) {
                char const* newline = strchr(buffer + ntowrite, '\n');
                if (newline) {
                    // Found another line break, include this line.
                    ntowrite = newline - buffer + 1;
                    line_count--;
                } else {
                    // No more line breaks, write the whole thing.
                    ntowrite = nread;
                    break;
                }
            }
        }

        size_t ncomplete = 0;
        while (ncomplete < ntowrite) {
            ssize_t nwritten = write(1, buffer + ncomplete, ntowrite - ncomplete);
            if (nwritten < 0) {
                perror("write");
                return 1;
            }
            ncomplete += nwritten;
        }
    }

    if (print_filename) {
        puts("");
    }

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));

    int line_count = -1;
    int byte_count = -1;
    bool never_print_filenames = false;
    bool always_print_filenames = false;
    Vector<ByteString> files;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print the beginning ('head') of a file.");
    args_parser.add_option(line_count, "Number of lines to print (default 10)", "lines", 'n', "number");
    args_parser.add_option(byte_count, "Number of bytes to print", "bytes", 'c', "number");
    args_parser.add_option(never_print_filenames, "Never print filenames", "quiet", 'q');
    args_parser.add_option(always_print_filenames, "Always print filenames", "verbose", 'v');
    args_parser.add_positional_argument(files, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(args);

    if (line_count == -1 && byte_count == -1) {
        line_count = 10;
    }

    bool print_filenames = files.size() > 1;
    if (always_print_filenames)
        print_filenames = true;
    else if (never_print_filenames)
        print_filenames = false;

    if (files.is_empty()) {
        return head("", print_filenames, line_count, byte_count);
    }

    int rc = 0;

    for (auto& file : files) {
        if (head(file, print_filenames, line_count, byte_count) != 0) {
            rc = 1;
        }
    }

    return rc;
}
