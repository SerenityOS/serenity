/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

enum NumberStyle {
    NumberAllLines,
    NumberNonEmptyLines,
    NumberNoLines,
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    NumberStyle number_style = NumberNonEmptyLines;
    int increment = 1;
    StringView separator = "  "sv;
    int start_number = 1;
    int number_width = 6;
    Vector<StringView> filenames;

    Core::ArgsParser args_parser;

    Core::ArgsParser::Option number_style_option {
        Core::ArgsParser::OptionArgumentMode::Required,
        "Line numbering style: 't' for non-empty lines, 'a' for all lines, 'n' for no lines",
        "body-numbering",
        'b',
        "style",
        [&number_style](StringView s) {
            if (s == "t"sv)
                number_style = NumberNonEmptyLines;
            else if (s == "a"sv)
                number_style = NumberAllLines;
            else if (s == "n"sv)
                number_style = NumberNoLines;
            else
                return false;

            return true;
        }
    };

    args_parser.add_option(move(number_style_option));
    args_parser.add_option(increment, "Line count increment", "increment", 'i', "number");
    args_parser.add_option(separator, "Separator between line numbers and lines", "separator", 's', "string");
    args_parser.add_option(start_number, "Initial line number", "startnum", 'v', "number");
    args_parser.add_option(number_width, "Number width", "width", 'w', "number");
    args_parser.add_positional_argument(filenames, "Files to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (filenames.is_empty())
        filenames.append(""sv);

    for (auto const filename : filenames) {
        auto maybe_file = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read);
        if (maybe_file.is_error()) {
            warnln("Failed to open {}: {}", filename, maybe_file.release_error());
            continue;
        }

        auto file = maybe_file.release_value();

        int line_number = start_number - increment; // so the line number can start at 1 when added below
        Optional<u8> previous_character;
        u8 next_character;
        for (Bytes bytes = TRY(file->read_some({ &next_character, 1 })); bytes.size() != 0; bytes = TRY(file->read_some(bytes))) {
            if (!previous_character.has_value() || previous_character == '\n') {
                if (next_character == '\n' && number_style != NumberAllLines) {
                    // Skip printing line count on empty lines.
                    outln();
                    continue;
                }
                if (number_style != NumberNoLines)
                    out("{1:{0}}{2}", number_width, (line_number += increment), separator);
                else
                    out("{1:{0}}", number_width, "");
            }
            putchar(next_character);
            previous_character = next_character;
        }

        if (previous_character.has_value() && previous_character != '\n')
            outln(); // for cases where files have no trailing newline
    }
    return 0;
}
