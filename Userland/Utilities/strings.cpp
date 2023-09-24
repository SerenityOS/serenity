/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Forward.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

enum class StringOffsetFormat {
    None = 0,
    Decimal,
    Octal,
    Hexadecimal
};

// NOTE: This is similar to how the cat utility works in the sense of aggregating
// data in 32K buffer.
static constexpr size_t buffer_read_size = 32768;

static bool should_print_characters(Vector<u8> const& characters)
{
    for (u8 ch : characters) {
        if (is_ascii_printable(ch) && !is_ascii_space(ch))
            return true;
    }
    return false;
}

static void print_characters(Vector<u8> const& characters, StringOffsetFormat string_offset_format, size_t string_offset_position)
{
    switch (string_offset_format) {
    case StringOffsetFormat::Decimal:
        out("{:>7d} ", string_offset_position);
        break;
    case StringOffsetFormat::Octal:
        out("{:>7o} ", string_offset_position);
        break;
    case StringOffsetFormat::Hexadecimal:
        out("{:>7x} ", string_offset_position);
        break;
    default:
        break;
    }
    outln("{:s}", characters.span());
}

static int process_characters_in_span(Vector<u8>& characters, ReadonlyBytes span)
{
    int processed_characters = 0;
    for (u8 ch : span) {
        ++processed_characters;
        if (is_ascii_printable(ch) || ch == '\t')
            characters.append(ch);
        else
            break;
    }
    return processed_characters;
}

static ErrorOr<void> process_strings_in_file(StringView path, bool show_paths, StringOffsetFormat string_offset_format, size_t minimum_string_length)
{
    Array<u8, buffer_read_size> buffer;
    Vector<u8> output_characters;
    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));
    size_t processed_characters = 0;
    size_t string_offset_position = 0;
    while (!file->is_eof()) {
        auto buffer_span = TRY(file->read_some(buffer));
        while (!buffer_span.is_empty()) {
            string_offset_position += processed_characters;
            processed_characters = process_characters_in_span(output_characters, buffer_span);
            if (output_characters.size() >= minimum_string_length && should_print_characters(output_characters)) {
                if (show_paths)
                    out("{}:", path);

                print_characters(output_characters, string_offset_format, string_offset_position);
            }
            buffer_span = buffer_span.slice(processed_characters);
            output_characters.clear();
        }
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    size_t minimum_string_length = 4;
    bool show_paths = false;

    StringOffsetFormat string_offset_format { StringOffsetFormat::None };

    Core::ArgsParser args_parser;
    args_parser.add_option(minimum_string_length, "Specify the minimum string length.", "bytes", 'n', "number");
    args_parser.add_option(show_paths, "Print the name of the file before each string.", "print-file-name", 'f');
    args_parser.add_option({ Core::ArgsParser::OptionArgumentMode::Required,
        "Write offset relative to start of each file in (d)ec, (o)ct, or he(x) format.",
        "radix",
        't',
        "format",
        [&string_offset_format](StringView value) {
            if (value == "d") {
                string_offset_format = StringOffsetFormat::Decimal;
            } else if (value == "o") {
                string_offset_format = StringOffsetFormat::Octal;
            } else if (value == "x") {
                string_offset_format = StringOffsetFormat::Hexadecimal;
            } else {
                return false;
            }
            return true;
        } });
    args_parser.add_option(string_offset_format, StringOffsetFormat::Octal, "Equivalent to specifying -t o.", nullptr, 'o');
    args_parser.set_general_help("Write the sequences of printable characters in files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (minimum_string_length < 1) {
        warnln("Invalid minimum string length {}", minimum_string_length);
        return 1;
    }

    if (paths.is_empty())
        paths.append("-"sv);

    bool has_errors = false;
    for (auto const& path : paths) {
        auto maybe_error = process_strings_in_file(path, show_paths, string_offset_format, minimum_string_length);
        if (maybe_error.is_error()) {
            warnln("strings: '{}'. {}", path, strerror(maybe_error.error().code()));
            has_errors = true;
        }
    }

    return has_errors ? 1 : 0;
}
