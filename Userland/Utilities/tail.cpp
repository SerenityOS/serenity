/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define DEFAULT_LINE_COUNT 10

static int tail_from_pos(Core::File& file, off_t startline, bool want_follow)
{
    if (!file.seek(startline + 1))
        return 1;

    while (true) {
        const auto& b = file.read(4096);
        if (b.is_empty()) {
            if (!want_follow) {
                break;
            } else {
                while (!file.can_read()) {
                    // FIXME: would be nice to have access to can_read_from_fd with an infinite timeout
                    usleep(100);
                }
                continue;
            }
        }

        if (write(STDOUT_FILENO, b.data(), b.size()) < 0)
            return 1;
    }

    return 0;
}

static off_t find_seek_pos(Core::File& file, int wanted_lines)
{
    // Rather than reading the whole file, start at the end and work backwards,
    // stopping when we've found the number of lines we want.
    off_t pos = 0;
    if (!file.seek(0, Core::IODevice::SeekMode::FromEndPosition, &pos)) {
        fprintf(stderr, "Failed to find end of file: %s\n", file.error_string());
        return 1;
    }

    off_t end = pos;
    int lines = 0;

    // FIXME: Reading char-by-char is only OK if IODevice's read buffer
    // is smart enough to not read char-by-char. Fix it there, or fix it here :)
    for (; pos >= 0; pos--) {
        file.seek(pos);
        const auto& ch = file.read(1);
        if (ch.is_empty()) {
            // Presumably the file got truncated?
            // Keep trying to read backwards...
        } else {
            if (*ch.data() == '\n' && (end - pos) > 1) {
                lines++;
                if (lines == wanted_lines)
                    break;
            }
        }
    }

    return pos;
}

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool follow = false;
    int line_count = DEFAULT_LINE_COUNT;
    const char* file = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print the end ('tail') of a file.");
    args_parser.add_option(follow, "Output data as it is written to the file", "follow", 'f');
    args_parser.add_option(line_count, "Fetch the specified number of lines", "lines", 'n', "number");
    args_parser.add_positional_argument(file, "File path", "file");
    args_parser.parse(argc, argv);

    auto f = Core::File::construct(file);
    if (!f->open(Core::OpenMode::ReadOnly)) {
        fprintf(stderr, "Error opening file %s: %s\n", file, strerror(errno));
        exit(1);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto pos = find_seek_pos(*f, line_count);
    return tail_from_pos(*f, pos, follow);
}
