/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    StringView path;
    Optional<size_t> head_count;
    bool is_zero_terminated = false;

    args_parser.add_positional_argument(path, "File", "file", Core::ArgsParser::Required::No);
    args_parser.add_option(head_count, "Output at most \"count\" lines", "head-count", 'n', "count");
    args_parser.add_option(is_zero_terminated, "Split input on \\0, not newline", "zero-terminated", 'z');

    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open_file_or_standard_stream(path, Core::Stream::OpenMode::Read));
    ByteBuffer buffer = TRY(file->read_until_eof());

    u8 input_delimiter = is_zero_terminated ? '\0' : '\n';
    Vector<Bytes> lines;

    auto bytes = buffer.span();
    size_t line_start = 0;
    size_t line_length = 0;
    for (size_t i = 0; i < bytes.size(); ++i) {
        if (bytes[i] == input_delimiter) {
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

    // Fisher-Yates shuffle
    Bytes tmp;
    for (size_t i = lines.size() - 1; i >= 1; --i) {
        size_t j = get_random_uniform(i + 1);
        // Swap i and j
        if (i == j)
            continue;
        tmp = lines[j];
        lines[j] = lines[i];
        lines[i] = tmp;
    }

    Array<u8, 1> output_delimiter = { '\n' };
    for (size_t i = 0; i < min(head_count.value_or(lines.size()), lines.size()); ++i) {
        TRY(Core::System::write(STDOUT_FILENO, lines.at(i)));
        TRY(Core::System::write(STDOUT_FILENO, output_delimiter));
    }

    return 0;
}
