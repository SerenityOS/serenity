/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    StringView path;
    Optional<size_t> head_count;
    bool is_zero_terminated = false;
    bool allow_repeats = false;

    args_parser.add_positional_argument(path, "File", "file", Core::ArgsParser::Required::No);
    args_parser.add_option(head_count, "Output at most \"count\" lines", "head-count", 'n', "count");
    args_parser.add_option(allow_repeats, "Pick lines at random rather than shuffling. The program will continue indefinitely if no `-n` option is specified", "repeat", 'r');
    args_parser.add_option(is_zero_terminated, "Split input on \\0, not newline", "zero-terminated", 'z');

    args_parser.parse(arguments);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));
    ByteBuffer buffer = TRY(file->read_until_eof());

    u8 delimiter = is_zero_terminated ? '\0' : '\n';
    Vector<Bytes> lines;

    auto bytes = buffer.span();
    size_t line_start = 0;
    size_t line_length = 0;
    for (size_t i = 0; i < bytes.size(); ++i) {
        if (bytes[i] == delimiter) {
            lines.append(bytes.slice(line_start, line_length));
            line_start = i + 1;
            line_length = 0;
        } else {
            ++line_length;
        }
    }
    if (line_length > 0) {
        lines.append(bytes.slice(line_start));
    }

    if (lines.is_empty())
        return 0;

    Array<u8, 1> delimiter_bytes { delimiter };
    if (allow_repeats) {
        for (size_t line_count = 0; !head_count.has_value() || line_count < head_count.value(); ++line_count) {
            size_t i = get_random_uniform(lines.size());
            out(lines.at(i));
            out(delimiter_bytes);
        }
    } else {
        shuffle(lines);
        for (size_t i = 0; i < min(head_count.value_or(lines.size()), lines.size()); ++i) {
            out(lines.at(i));
            out(delimiter_bytes);
        }
    }

    return 0;
}
