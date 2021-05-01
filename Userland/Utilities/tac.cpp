/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout, last line first.");
    args_parser.add_positional_argument(path, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    RefPtr<Core::File> file;
    if (path == nullptr) {
        file = Core::File::standard_input();
    } else {
        auto file_or_error = Core::File::open(path, Core::File::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            return 1;
        }
        file = file_or_error.value();
    }

    Vector<String> lines;
    while (file->can_read_line()) {
        auto line = file->read_line();
        lines.append(line);
    }

    for (int i = lines.size() - 1; i >= 0; --i)
        outln("{}", lines[i]);

    return 0;
}
