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
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <stdio.h>
#include <sys/stat.h>

struct Count {
    String name;
    bool exists = true;
    unsigned int lines = 0;
    unsigned int characters = 0;
    unsigned int words = 0;
    size_t bytes = 0;
};

bool output_line = false;
bool output_byte = false;
bool output_word = false;

static void wc_out(Count& count)
{
    if (output_line)
        printf("%7i ", count.lines);
    if (output_word)
        printf("%7i ", count.words);
    if (output_byte)
        printf("%7lu ", count.bytes);

    printf("%14s\n", count.name.characters());
}

static Count get_count(const String& file_name)
{
    Count count;
    FILE* file_pointer = nullptr;
    if (file_name == "-") {
        count.name = "";
        file_pointer = stdin;
    } else {
        count.name = file_name;
        if ((file_pointer = fopen(file_name.characters(), "r")) == NULL) {
            fprintf(stderr, "wc: unable to open %s\n", file_name.characters());
            count.exists = false;
            return count;
        }
    }
    bool start_a_new_word = true;
    for (int ch = fgetc(file_pointer); ch != EOF; ch = fgetc(file_pointer)) {
        count.bytes++;
        if (isspace(ch)) {
            start_a_new_word = true;
        } else if (start_a_new_word) {
            start_a_new_word = false;
            count.words++;
        }
        if (ch == '\n')
            count.lines++;
    }
    return count;
}

static Count get_total_count(Vector<Count>& counts)
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

    Vector<const char*> files;

    Core::ArgsParser args_parser;
    args_parser.add_option(output_line, "Output line count", "lines", 'l');
    args_parser.add_option(output_byte, "Output byte count", "bytes", 'c');
    args_parser.add_option(output_word, "Output word count", "words", 'w');
    args_parser.add_positional_argument(files, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!output_line && !output_byte && !output_word)
        output_line = output_byte = output_word = true;

    Vector<Count> counts;
    for (auto& file : files) {
        Count count = get_count(file);
        counts.append(count);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (files.size() > 1) {
        Count total_count = get_total_count(counts);
        counts.append(total_count);
    }

    if (files.is_empty()) {
        Count count = get_count("-");
        counts.append(count);
    }

    for (auto& count : counts)
        if (count.exists)
            wc_out(count);

    return 0;
}
