/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

enum NumberStyle {
    NumberAllLines,
    NumberNonEmptyLines,
    NumberNoLines,
};

int main(int argc, char** argv)
{
    NumberStyle number_style = NumberNonEmptyLines;
    int increment = 1;
    const char* separator = "  ";
    int start_number = 1;
    int number_width = 6;
    Vector<const char*> files;

    Core::ArgsParser args_parser;

    Core::ArgsParser::Option number_style_option {
        true,
        "Line numbering style: 't' for non-empty lines, 'a' for all lines, 'n' for no lines",
        "body-numbering",
        'b',
        "style",
        [&number_style](const char* s) {
            if (!strcmp(s, "t"))
                number_style = NumberNonEmptyLines;
            else if (!strcmp(s, "a"))
                number_style = NumberAllLines;
            else if (!strcmp(s, "n"))
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
    args_parser.add_positional_argument(files, "Files to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    Vector<FILE*> file_pointers;
    if (!files.is_empty()) {
        for (auto& file : files) {
            FILE* file_pointer = fopen(file, "r");
            if (!file_pointer) {
                warnln("Failed to open {}: {}", file, strerror(errno));
                continue;
            }
            file_pointers.append(file_pointer);
        }
    } else {
        file_pointers.append(stdin);
    }

    for (auto& file_pointer : file_pointers) {
        int line_number = start_number - increment; // so the line number can start at 1 when added below
        int previous_character = 0;
        int next_character = 0;
        while ((next_character = fgetc(file_pointer)) != EOF) {
            if (previous_character == 0 || previous_character == '\n') {
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
        fclose(file_pointer);
        if (previous_character != '\n')
            outln(); // for cases where files have no trailing newline
    }
    return 0;
}
