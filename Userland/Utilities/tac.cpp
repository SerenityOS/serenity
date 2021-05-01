/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <unistd.h>

static void read_stdin(void)
{
    char* line = nullptr;
    size_t length = 0;
    ssize_t nread = 0;
    Vector<String> lines;

    ScopeGuard free_line = [line] { free(line); };
    while ((nread = getline(&line, &length, stdin)) != -1) {
        VERIFY(nread > 0);
        lines.append(String(line));
    }
    for (int i = lines.size() - 1; i >= 0; i--)
        out("{}", lines[i]);
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<String> paths;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Concatenate files or pipes to stdout, line order reversed.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (paths.is_empty()) {
        read_stdin();
    } else {
        for (auto const& path : paths) {
            if (path == "-") {
                read_stdin();
            } else {
                auto file_or_error = Core::File::open(path, Core::File::ReadOnly);
                if (file_or_error.is_error()) {
                    warnln("Failed to open {}: {}", path, file_or_error.error());
                    continue;
                }

                Vector<String> lines;
                auto file = file_or_error.value();
                while (file->can_read_line())
                    lines.append(file->read_line());

                for (int i = lines.size() - 1; i >= 0; i--)
                    outln("{}", lines[i]);
            }
        }
    }

    return 0;
}