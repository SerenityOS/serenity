/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <ctype.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

struct Count {
    StringView name;
    bool exists { true };
    unsigned lines { 0 };
    unsigned characters { 0 };
    unsigned words { 0 };
    unsigned max_line_length { 0 };
    size_t bytes { 0 };
};

bool g_output_line = false;
bool g_output_byte = false;
bool g_output_word = false;
bool g_output_max_line_length = false;

static void wc_out(Count const& count)
{
    if (g_output_line)
        out("{:7} ", count.lines);
    if (g_output_word)
        out("{:7} ", count.words);
    if (g_output_byte)
        out("{:7} ", count.bytes);
    if (g_output_max_line_length)
        out("{:7} ", count.max_line_length);

    outln("{:>14}", count.name);
}

static ErrorOr<Count> get_count(StringView file_specifier, bool only_bytes)
{
    Count count;

    auto maybe_file = Core::File::open_file_or_standard_stream(file_specifier, Core::File::OpenMode::Read);

    if (maybe_file.is_error()) {
        warnln("wc: unable to open {}", file_specifier.is_empty() ? "stdin"sv : file_specifier);
        count.exists = false;
        return count;
    }

    auto file = TRY(Core::InputBufferedFile::create(maybe_file.release_value()));
    count.name = file_specifier;

    if (only_bytes) {
        auto seek_or_error = file->size();
        if (!seek_or_error.is_error()) {
            count.bytes = seek_or_error.release_value();
            return count;
        }

        // Handle the file as non-seekable if the error indicates it as such.
        if (seek_or_error.error().code() != ESPIPE)
            return seek_or_error.release_error();
    }

    bool start_a_new_word = true;
    unsigned current_line_length = 0;

    u8 ch;
    for (Bytes bytes = TRY(file->read_some({ &ch, 1 })); bytes.size() != 0; bytes = TRY(file->read_some(bytes))) {
        count.bytes++;
        if (ch != '\n')
            current_line_length++;
        if (is_ascii_space(ch)) {
            start_a_new_word = true;
            if (ch == '\n') {
                count.lines++;
                if (current_line_length > count.max_line_length)
                    count.max_line_length = current_line_length;

                current_line_length = 0;
            }
        } else if (start_a_new_word) {
            start_a_new_word = false;
            count.words++;
        }
    }

    return count;
}

static Count get_total_count(Vector<Count> const& counts)
{
    Count total_count { "total"sv };
    for (auto& count : counts) {
        total_count.lines += count.lines;
        total_count.words += count.words;
        total_count.characters += count.characters;
        total_count.bytes += count.bytes;
        if (count.max_line_length > total_count.max_line_length)
            total_count.max_line_length = count.max_line_length;
    }
    return total_count;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> file_specifiers;

    Core::ArgsParser args_parser;
    args_parser.add_option(g_output_line, "Output line count", "lines", 'l');
    args_parser.add_option(g_output_byte, "Output byte count", "bytes", 'c');
    args_parser.add_option(g_output_word, "Output word count", "words", 'w');
    args_parser.add_option(g_output_max_line_length, "Output byte count of the longest line", "max-line-length", 'L');
    args_parser.add_positional_argument(file_specifiers, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!g_output_line && !g_output_byte && !g_output_word && !g_output_max_line_length)
        g_output_line = g_output_byte = g_output_word = true;

    bool only_bytes = !g_output_line && !g_output_word && !g_output_max_line_length;

    Vector<Count> counts;
    for (auto const& file_specifier : file_specifiers)
        counts.append(TRY(get_count(file_specifier, only_bytes)));

    TRY(Core::System::pledge("stdio"));

    if (file_specifiers.is_empty())
        counts.append(TRY(get_count(""sv, only_bytes)));
    else if (file_specifiers.size() > 1)
        counts.append(get_total_count(counts));

    for (auto const& count : counts) {
        if (count.exists)
            wc_out(count);
    }

    return 0;
}
