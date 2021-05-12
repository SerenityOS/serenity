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

    Vector<String> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout, last line first.");
    args_parser.add_positional_argument(paths, "File path(s)", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto read_lines = [&](RefPtr<Core::File> file) {
        Vector<String> lines;
        while (file->can_read_line()) {
            lines.append(file->read_line());
        }
        file->close();
        for (int i = lines.size() - 1; i >= 0; --i)
            outln("{}", lines[i]);
    };

    if (!paths.is_empty()) {
        for (auto const& path : paths) {
            RefPtr<Core::File> file;
            if (path == "-") {
                file = Core::File::standard_input();
            } else {
                auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
                if (file_or_error.is_error()) {
                    warnln("Failed to open {}: {}", path, strerror(errno));
                    continue;
                }
                file = file_or_error.release_value();
            }
            read_lines(file);
        }
    } else {
        read_lines(Core::File::standard_input());
    }

    return 0;
}
