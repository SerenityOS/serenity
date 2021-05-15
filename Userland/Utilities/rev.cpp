/*
 * Copyright (c) 2021, Thomas Voss <thomasvoss@live.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
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

    Vector<String> paths;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Concatente files to stdout with each line in reverse.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    Vector<RefPtr<Core::File>> files;
    if (paths.is_empty()) {
        files.append(Core::File::standard_input());
    } else {
        for (auto const& path : paths) {
            auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
            if (file_or_error.is_error()) {
                warnln("Failed to open {}: {}", path, file_or_error.error());
                continue;
            }

            files.append(file_or_error.value());
        }
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    for (auto& file : files) {
        while (file->can_read_line()) {
            auto line = file->read_line();
            outln("{}", line.reverse());
        }
    }

    return 0;
}
