/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
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
    if (!file.seek(0, Core::SeekMode::FromEndPosition, &pos)) {
        warnln("Failed to find end of file: {}", file.error_string());
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool follow = false;
    int line_count = DEFAULT_LINE_COUNT;
    const char* file = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print the end ('tail') of a file.");
    args_parser.add_option(follow, "Output data as it is written to the file", "follow", 'f');
    args_parser.add_option(line_count, "Fetch the specified number of lines", "lines", 'n', "number");
    args_parser.add_positional_argument(file, "File path", "file");
    args_parser.parse(arguments);

    auto f = TRY(Core::File::open(file, Core::OpenMode::ReadOnly));
    TRY(Core::System::pledge("stdio"));

    auto pos = find_seek_pos(*f, line_count);
    return tail_from_pos(*f, pos, follow);
}
