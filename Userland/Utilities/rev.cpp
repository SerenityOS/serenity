/*
 * Copyright (c) 2021, Thomas Voss <thomasvoss@live.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<String> paths;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Concatente files to stdout with each line in reverse.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    Vector<RefPtr<Core::File>> files;
    for (auto const& path : paths) {
        auto file_or_error = Core::File::open(path, Core::File::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            continue;
        }

        files.append(file_or_error.value());
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (files.is_empty()) {
        char* line = nullptr;
        size_t length = 0;
        ssize_t nread = 0;

        ScopeGuard free_line = [line] { free(line); };
        while ((nread = getline(&line, &length, stdin)) != -1) {
            VERIFY(nread > 0);
            if (line[--nread] == '\n')
                nread--;

            for (; nread >= 0; nread--)
                putchar(line[nread]);

            putchar('\n');
        }
    } else {
        for (auto& file : files) {
            while (file->can_read_line()) {
                auto line = file->read_line();
                outln("{}", line.reverse());
            }
        }
    }

    return 0;
}
