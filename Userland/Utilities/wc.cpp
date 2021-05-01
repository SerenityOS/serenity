/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

struct Count {
    String name;
    bool exists { true };
    unsigned lines { 0 };
    unsigned characters { 0 };
    unsigned words { 0 };
    size_t bytes { 0 };
};

bool g_output_line = false;
bool g_output_byte = false;
bool g_output_word = false;

static void wc_out(const Count& count)
{
    if (g_output_line)
        out("{:7} ", count.lines);
    if (g_output_word)
        out("{:7} ", count.words);
    if (g_output_byte)
        out("{:7} ", count.bytes);

    outln("{:>14}", count.name);
}

static Count get_count(const String& file_specifier)
{
    Count count;
    FILE* file_pointer = nullptr;
    if (file_specifier == "-") {
        count.name = "";
        file_pointer = stdin;
    } else {
        count.name = file_specifier;
        if ((file_pointer = fopen(file_specifier.characters(), "r")) == nullptr) {
            warnln("wc: unable to open {}", file_specifier);
            count.exists = false;
            return count;
        }
    }

    bool start_a_new_word = true;
    for (int ch = fgetc(file_pointer); ch != EOF; ch = fgetc(file_pointer)) {
        count.bytes++;
        if (isspace(ch)) {
            start_a_new_word = true;
            if (ch == '\n')
                count.lines++;
        } else if (start_a_new_word) {
            start_a_new_word = false;
            count.words++;
        }
    }

    if (file_pointer != stdin)
        fclose(file_pointer);

    return count;
}

static Count get_total_count(const Vector<Count>& counts)
{
    Count total_count { "total" };
    for (auto& count : counts) {
        total_count.lines += count.lines;
        total_count.words += count.words;
        total_count.characters += count.characters;
        total_count.bytes += count.bytes;
    }
    return total_count;
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> file_specifiers;

    Core::ArgsParser args_parser;
    args_parser.add_option(g_output_line, "Output line count", "lines", 'l');
    args_parser.add_option(g_output_byte, "Output byte count", "bytes", 'c');
    args_parser.add_option(g_output_word, "Output word count", "words", 'w');
    args_parser.add_positional_argument(file_specifiers, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!g_output_line && !g_output_byte && !g_output_word)
        g_output_line = g_output_byte = g_output_word = true;

    Vector<Count> counts;
    for (const auto& file_specifier : file_specifiers)
        counts.append(get_count(file_specifier));

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (file_specifiers.is_empty())
        counts.append(get_count("-"));
    else if (file_specifiers.size() > 1)
        counts.append(get_total_count(counts));

    for (const auto& count : counts) {
        if (count.exists)
            wc_out(count);
    }

    return 0;
}
