/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    RefPtr<Core::File> file;

    if (!path) {
        file = Core::File::standard_input();
    } else {
        auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            return 1;
        }
        file = file_or_error.value();
    }

    auto contents = file->read_all();

    Vector<u8, 16> line;

    auto print_line = [&] {
        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size())
                out("{:02x} ", line[i]);
            else
                out("   ");

            if (i == 7)
                out("  ");
        }

        out("  ");

        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size() && isprint(line[i]))
                putchar(line[i]);
            else
                putchar(' ');
        }

        putchar('\n');
    };

    for (size_t i = 0; i < contents.size(); ++i) {
        line.append(contents[i]);

        if (line.size() == 16) {
            print_line();
            line.clear();
        }
    }

    if (!line.is_empty())
        print_line();

    return 0;
}
