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

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>

#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("nl");
    args_parser.add_arg("b", "type", "Line count type. \n\tt counts non-empty lines. \n\ta counts all lines. \n\tn counts no lines.");
    args_parser.add_arg("i", "incr", "Set line count increment.");
    args_parser.add_arg("s", "delim", "Set buffer between the line numbers and text. 1-63 bytes");
    args_parser.add_arg("v", "startnum", "Initial value used to number logical page lines.");
    args_parser.add_arg("w", "width", "The number of characters used for the line number.");
    args_parser.add_single_value("file");
    CArgsParserResult args = args_parser.parse(argc, argv);

    bool all_lines_flag = false;
    bool line_numbers_flag = true;
    String value_of_b;
    if (args.is_present("b")) {
        value_of_b = args.get("b");
        if (value_of_b == "a")
            all_lines_flag = true;
        else if (value_of_b == "t")
            all_lines_flag = false;
        else if (value_of_b == "n")
            line_numbers_flag = false;
        else {
            args_parser.print_usage();
            return 1;
        }
    }

    long line_number_increment = 1;
    String value_of_i;
    if (args.is_present("i")) {
        value_of_i = args.get("i");
        line_number_increment = atol(value_of_i.characters());
        if (!line_number_increment) {
            args_parser.print_usage();
            return 1;
        }
    }

    bool delimiter_flag = false;
    String value_of_s;
    if (args.is_present("s")) {
        value_of_s = args.get("s");
        if (value_of_s.length() > 0 && value_of_s.length() < 64)
            delimiter_flag = true;
        else {
            args_parser.print_usage();
            return 1;
        }
    }
    char delimiter[64];
    strcpy(delimiter, delimiter_flag ? value_of_s.characters() : "  ");

    long line_number = 1;
    String value_of_v;
    if (args.is_present("v")) {
        value_of_v = args.get("v");
        line_number = atol(value_of_v.characters());
    }

    String value_of_w;
    unsigned int line_number_width = 6;
    if (args.is_present("w")) {
        value_of_w = args.get("w");
        line_number_width = atol(value_of_w.characters());
    }

    Vector<String> files = args.get_single_values();
    Vector<FILE*> file_pointers;
    if (files.size() > 0) {
        for (auto& file : files) {
            FILE* file_pointer;
            if ((file_pointer = fopen(file.characters(), "r")) == NULL) {
                fprintf(stderr, "unable to open %s\n", file.characters());
                continue;
            }
            file_pointers.append(file_pointer);
        }
    } else {
        file_pointers.append(stdin);
    }

    line_number -= line_number_increment; // so the line number can start at 1 when added below
    for (auto& file_pointer : file_pointers) {
        int previous_character = 0;
        int next_character = 0;
        while ((next_character = fgetc(file_pointer)) != EOF) {
            if (previous_character == 0 || previous_character == '\n') {
                if (!all_lines_flag && next_character == '\n') {
                    // skips printing line count on empty lines if all_lines_flags is false
                    printf("\n");
                    continue;
                }
                if (line_numbers_flag)
                    printf("%*lu%s", line_number_width, (line_number += line_number_increment), delimiter);
                else
                    printf("%*s", line_number_width, "");
            }
            putchar(next_character);
            previous_character = next_character;
        }
        fclose(file_pointer);
        if (previous_character != '\n')
            printf("\n"); // for cases where files have no trailing newline
    }
    return 0;
}
