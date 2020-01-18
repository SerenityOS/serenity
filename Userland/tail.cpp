/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CFile.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define DEFAULT_LINE_COUNT 10

int tail_from_pos(CFile& file, off_t startline, bool want_follow)
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

off_t find_seek_pos(CFile& file, int wanted_lines)
{
    // Rather than reading the whole file, start at the end and work backwards,
    // stopping when we've found the number of lines we want.
    off_t pos = 0;
    if (!file.seek(0, CIODevice::SeekMode::FromEndPosition, &pos)) {
        fprintf(stderr, "Failed to find end of file: %s\n", file.error_string());
        return 1;
    }

    off_t end = pos;
    int lines = 0;

    // FIXME: Reading char-by-char is only OK if CIODevice's read buffer
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

    CArgsParser args_parser("tail");

    args_parser.add_arg("f", "follow -- appended data is output as it is written to the file");
    args_parser.add_arg("n", "lines", "fetch the specified number of lines");
    args_parser.add_required_single_value("file");

    CArgsParserResult args = args_parser.parse(argc, argv);

    Vector<String> values = args.get_single_values();
    if (values.size() != 1) {
        args_parser.print_usage();
        return 1;
    }

    int line_count = 0;
    if (args.is_present("n")) {
        line_count = strtol(args.get("n").characters(), NULL, 10);
        if (errno == EINVAL) {
            args_parser.print_usage();
            return 1;
        }
    } else {
        line_count = DEFAULT_LINE_COUNT;
    }

    auto f = CFile::construct(values[0]);
    if (!f->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error opening file %s: %s\n", f->filename().characters(), strerror(errno));
        exit(1);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool flag_follow = args.is_present("f");
    auto pos = find_seek_pos(*f, line_count);
    return tail_from_pos(*f, pos, flag_follow);
}
