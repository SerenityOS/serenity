/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) > 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> files;
    Vector<String> lines;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(files, "File(s) to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto handle_file = [&](StringView filename) -> bool {
        auto file = Core::File::construct(filename);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("Failed to open {}: {}", filename, file->error_string());
            return false;
        }

        while (file->can_read_line())
            lines.append(file->read_line());
        return true;
    };

    if (files.is_empty()) {
        for (;;) {
            char* buffer = nullptr;
            ssize_t buflen = 0;
            size_t n;
            errno = 0;
            buflen = getline(&buffer, &n, stdin);
            if (buflen == -1 && errno != 0) {
                perror("getline");
                exit(1);
            }
            if (buflen == -1)
                break;
            lines.append({ buffer, AK::ShouldChomp::Chomp });
        }
    } else {
        for (auto& filename : files) {
            if (!handle_file(filename))
                return 1;
        }
    }

    quick_sort(lines, [](auto& a, auto& b) {
        return strcmp(a.characters(), b.characters()) < 0;
    });

    for (auto& line : lines) {
        fputs(line.characters(), stdout);
        fputc('\n', stdout);
    }

    return 0;
}
